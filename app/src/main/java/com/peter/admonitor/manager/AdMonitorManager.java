package com.peter.admonitor.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.gson.Gson;
import com.peter.admonitor.MainApplication;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 广告控件监测处理类
 *
 * @author Peter 2019-08-23
 */
public class AdMonitorManager {

    private static final String TAG = AdMonitorManager.class.getSimpleName();
    private static final int API_AD_RPT_TYPE_SHOW = 1;
    private static final int API_AD_RPT_TYPE_CLICK = 2;
    /**
     * 是否已经设置过全局生命周期监听
     */
    private static boolean isLifecycleListened = false;
    /**
     * 目前栈顶的页面信息
     */
    private static String topActivityInfo;
    /**
     * 受监听的广告控件列表
     */
    private static List<AdViews> adViewsList;
    private static Gson gson;

    /**
     * 注册一个广告控件，注册为广告控件的View，会在Sdk中完成显示状态的监听，并完成广告曝光事件的统计
     * <strong>NOTE: </strong>在页面实例未被重建前已注册过的控件无需重复注册，重复调用只更新广告监测属性
     *
     * @param view          目标控件，只有非空才能有效注册
     * @param adMonitorAttr 广告监测属性，只有非空才能有效注册
     */
    public static void regAdView(final View view, AdMonitorAttr adMonitorAttr) {
        if (view == null || view.getContext() == null || adMonitorAttr == null) {
            return;
        }
        AdViews adViewsReged = findView(view);
        if (adViewsReged != null) {
            Log.i(TAG, view + " 上一次注册还有效，本次只更新广告监测属性");
            //先把之前的设置为关闭，以形成曝光事件
            onAdViewShowStateChanged(view, false);
            //再更新其他属性
            adViewsReged.adMonitorAttr = adMonitorAttr;
            adViewsReged.setShow(isViewShow(view));
            return;
        }
        if (adViewsList == null) {
            adViewsList = new ArrayList<>();
        }
        AdViews adView = new AdViews(view, adMonitorAttr);
        adView.setShow(isViewShow(view));
        adViewsList.add(adView);

        //注册所在Activity生命周期监听
        regListener(view.getContext().getApplicationContext());

        //订阅相关ViewTree事件
        addViewTreeObserverListener(view);
    }

    private static void addViewTreeObserverListener(final View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                @Override
                public void onDraw() {
                    //发生绘制事件时有可能会变更显示状态
                    onAdViewShowStateChanged(view, isViewShow(view));
                }
            });
        }
        viewTreeObserver.addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                //发生滑动事件时有可能会变更显示状态
                onAdViewShowStateChanged(view, isViewShow(view));
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                @Override
                public void onWindowAttached() {
                    //重新着附于窗口事件时有可能会变更显示状态
                    onAdViewShowStateChanged(view, isViewShow(view));
                }

                @Override
                public void onWindowDetached() {
                    //此时一定为不显示状态
                    onAdViewShowStateChanged(view, false);
                }
            });
        }
    }

    /**
     * 注册Application级别的全局Activity生命周期订阅事件<br/>
     * <strong>NOTE:</>重复调用只会在第一次调用时注册
     *
     * @param context ApplicationContext
     */
    private static void regListener(Context context) {
        if (!isLifecycleListened) {
            checkNotNull(context, "上下文不能为空");
            if (!(context instanceof MainApplication)) {
                throw new IllegalArgumentException("上下文必须为ApplicationContext");
            }
            ((MainApplication) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                }

                @Override
                public void onActivityStarted(Activity activity) {

                }

                @Override
                public void onActivityResumed(Activity activity) {
                    //onResumed时更新当前栈顶页面信息
                    topActivityInfo = activity.getClass().getSimpleName() + activity.hashCode();
                    //找出当前页面已注册的广告控件，刷新显示状态
                    List<AdViews> adViewsFound = foundActivityAdViews(activity);
                    for (AdViews adView : adViewsFound) {
                        onAdViewShowStateChanged(adView.adView, true);
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    //此时所有在当前页面的控件一定是不可见的
                    List<AdViews> adViewsFound = foundActivityAdViews(activity);
                    for (AdViews adView : adViewsFound) {
                        onAdViewShowStateChanged(adView.adView, false);
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    List<AdViews> adViewsFound = foundActivityAdViews(activity);
                    if (adViewsFound.size() > 0) {
                        for (AdViews adView : adViewsFound) {
                            if (adView.isShow) {
                                //如果页面异常终止，可能还有广告控件还来不及更新成未显示，补充更新
                                onAdViewShowStateChanged(adView.adView, false);
                            }
                        }
                        //把已终止页面中的所有广告控件均移除
                        adViewsList.removeAll(adViewsFound);
                    }
                }
            });
            isLifecycleListened = true;
        }
    }

    /**
     * @return 目标View是否已经注册过为广告控件
     */
    private static AdViews findView(@NonNull View view) {
        checkNotNull(view, "请传入有效的目标View");
        if (adViewsList == null || adViewsList.size() == 0) {
            return null;
        }
        for (AdViews adView : adViewsList) {
            if (view.equals(adView.adView)) {
                return adView;
            }
        }
        return null;
    }

    /**
     * @return 返回当前Activity中已经注册为广告控件的清单列表
     */
    private static List<AdViews> foundActivityAdViews(Activity activity) {
        List<AdViews> adViewsFound = new ArrayList<>();
        if (adViewsList == null || adViewsList.size() == 0) {
            return adViewsFound;
        }
        //添加hashCode，区分同一Activity不同实例
        String name = activity.getClass().getSimpleName() + activity.hashCode();
        for (AdViews adView : adViewsList) {
            if (adView.attachActivityName.equals(name)) {
                adViewsFound.add(adView);
            }
        }
        return adViewsFound;
    }

    /**
     * @return 返回目标View是否真实用户可见
     */
    private static boolean isViewShow(View view) {
        if (view == null) {
            return false;
        }
        //如果view所在Activity并不在栈顶，此View一定不在显示状态
        if (view.getContext() != null
                && !(view.getContext().getClass().getSimpleName() + view.getContext().hashCode()).equals(topActivityInfo)) {
            return false;
        }

        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 || height == 0) {
            //view的面积为0，一定不在显示状态
            return false;
        }
        Rect r = new Rect();
        // 首先判断View的基本visibility，再判断当前View在其ViewTree中有没被遮挡（还是有缺陷的：逻辑上，如果同一页面中被其他viewTree遮挡了，
        // 是无法识别到的，暂时未有很好的办法，好在这种case以目前App页面View结构复杂度来看几乎没有，影响暂时忽略）
        boolean isShow = view.isShown() && view.getLocalVisibleRect(r);
        if (!isShow) {
            return false;
        }

        //需求要求计算可见面积，如果小于View面积一并，仍要标记为未展示
        int area = width * height;
        int displayArea = (r.right - r.left) * (r.bottom - r.top);
        boolean isValidShow = area <= displayArea << 1;
        if (!isValidShow) {
            Log.i("--->", "展示面积小于一半，判定为不可见：" + shortStr(view.toString()));
        }
        return isValidShow;
    }

    /**
     * 统一更新View的显示状态，当更新为显示时，里面会调 {@link #isViewShow(View)}进一步判断；当更新为不显示时，
     * 如果之前是在显示状态，会组装一条曝光事件记录，之后更新状态为不显示
     *
     * @param isShow 只需要初步判定可能是显示的即可传true，只有确认为一定不显示时才传false
     */
    private static void onAdViewShowStateChanged(View view, boolean isShow) {
        if (adViewsList == null) {
            return;
        }
        AdViews adViewsTarget = null;
        for (AdViews adView : adViewsList) {
            if (adView.adView.equals(view)) {
                adViewsTarget = adView;
                break;
            }
        }
        if (adViewsTarget == null) {
            //未在显示中的广告控件列表中
            return;
        }
        if (isShow) {
            if (adViewsTarget.isShow) {
                //如果原来已经在显示了，本次也是"变为"显示（通常是重复调用了），则不需要处理什么
                return;
            }
            //重新判断是否在Activity内屏幕中显示
            if (isViewShow(view)) {
                //更新下目标广告控件显示状态
                adViewsTarget.setShow(true);
            }
        } else {
            if (!adViewsTarget.isShow) {
                //如果原来已经不在显示了，本次也是"变为"不显示（通常是重复调用了），则不需要处理什么
                return;
            }
            //需求要求曝光时长必须大于1秒时才能视为有效曝光
            long nowTime = System.currentTimeMillis();
            if (nowTime - adViewsTarget.startTime >= 1000) {
                //已经可以形成一对完整曝光时长时间，组成事件执行一次记录
                Log.i("--->", "生成一条广告曝光事件记录，广告位=" + adViewsTarget.adMonitorAttr.placeEventId + ", 广告标识=" +
                        adViewsTarget.adMonitorAttr.adTag + ", 曝光时长=" + (System.currentTimeMillis() - adViewsTarget.startTime) + "ms");

                doAsyncReport(adViewsTarget.adMonitorAttr, API_AD_RPT_TYPE_SHOW);

                //同时对配置的广告曝光异步监测进行上报
                onAdShow(adViewsTarget.adMonitorAttr);
            } else {
                Log.i("--->", "曝光" + (nowTime - adViewsTarget.startTime) + "ms，时长小于1秒，丢掉：广告位=" + adViewsTarget.adMonitorAttr.placeEventId +
                        ", 广告标识=" + adViewsTarget.adMonitorAttr.adTag);
            }

            //完成事件生成后更新同步控件显示状态
            adViewsTarget.setShow(false);
        }
    }

    private static void onAdShow(AdMonitorAttr adMonitorAttr) {
        List<String> urls = adMonitorAttr.getShowRptUrls();
        Log.i("--->", "异步上报广告\"" + adMonitorAttr.adTag + "\"曝光监测链接：" + getGson().toJson(adMonitorAttr.showRptUrls));

        doThirdAsyncReportUrls(urls);
    }

    public static void onAdClick(View view) {
        if (view == null) {
            return;
        }
        AdViews adViews = findView(view);
        if (adViews == null || adViews.adMonitorAttr == null) {
            Log.w(TAG, "广告控件未注册，点击监测响应丢掉：" + view);
            return;
        }
        List<String> urls = adViews.adMonitorAttr.getClickRptUrls();
        Log.i("--->", "异步上报广告\"" + adViews.adMonitorAttr.adTag + "\"点击监测链接：" + getGson().toJson(adViews.adMonitorAttr.clickRptUrls));
        doThirdAsyncReportUrls(urls);

        doAsyncReport(adViews.adMonitorAttr, API_AD_RPT_TYPE_CLICK);
    }

    /**
     * 广告控件信息
     */
    static class AdViews {
        private View adView;
        private String attachActivityName;
        private AdMonitorAttr adMonitorAttr;
        private long startTime = -1;
        private boolean isShow = false;

        /**
         * <strong>NOTE</strong>注册的广告View必须已经加载到上下文中，否则在新建实例时会抛出 {@link IllegalStateException}
         *
         * @param view          目标控件
         * @param adMonitorAttr 广告监测属性
         */
        AdViews(@NonNull View view, @NonNull AdMonitorAttr adMonitorAttr) {
            this.adView = checkNotNull(view, "目标控件不能为空");
            this.adMonitorAttr = checkNotNull(adMonitorAttr, "广告监测属性不能为空");
            if (view.getContext() == null) {
                throw new IllegalStateException("注册的广告View必须已经加载到上下文中");
            }
            attachActivityName = view.getContext().getClass().getSimpleName() + view.getContext().hashCode();
        }

        /**
         * 设置显示状态，会联动更新 startTime
         *
         * @param show 目标显示状态
         */
        void setShow(boolean show) {
            if (show) {
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
            } else {
                startTime = -1;
            }
            isShow = show;

            Log.i("--->", "setShow=" + isShow + ", startTime=" + startTime);
        }

        @NonNull
        @Override
        public String toString() {
            return "AdViews{" +
                    "adView=" + adView +
                    ", attachActivityName='" + attachActivityName + '\'' +
                    ", adMonitorAttr='" + adMonitorAttr + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", isShow=" + isShow +
                    '}';
        }
    }

    private static String shortStr(String src) {
        if (TextUtils.isEmpty(src) || src.length() <= 20) {
            return src;
        }
        return src.substring(0, 9) + "..." + src.substring(src.length() - 11);
    }

    private static Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    /**
     * TODO 这里增加上报曝光事件到自监测平台
     *
     * @param adMonitorAttr 广告配置信息
     * @param rptType       上报事件类型
     */
    private static void doAsyncReport(AdMonitorAttr adMonitorAttr, int rptType) {
    }

    /**
     * TODO 这里增加上报第三方监测，通常只需对链接依次发送GET请求就好
     *
     * @param urls 第三方异步监测链接，包含曝光或点击
     */
    private static void doThirdAsyncReportUrls(List<String> urls) {
    }

    /**
     * 广告监测属性对象
     */
    public static class AdMonitorAttr {
        private String placeEventId;
        private String indexInfo;
        private String adTag;
        private List<String> showRptUrls;
        private List<String> clickRptUrls;

        /**
         * @return 控件所在广告位的事件id
         */
        public String getPlaceEventId() {
            return placeEventId;
        }

        /**
         * @param placeEventId 控件所在广告位的事件id
         */
        public AdMonitorAttr setPlaceEventId(String placeEventId) {
            this.placeEventId = placeEventId;
            return this;
        }

        /**
         * @return 当前控件在广告位中索引位置（与动态事件定义一致）
         */
        public String getIndexInfo() {
            return indexInfo;
        }

        /**
         * @param indexInfo 当前控件在广告位中索引位置（与动态事件定义一致）
         */
        public AdMonitorAttr setIndexInfo(String indexInfo) {
            this.indexInfo = indexInfo;
            return this;
        }

        /**
         * @return 当前控件承载的广告内容标识符（与动态事件定义一致）
         */
        public String getAdTag() {
            return adTag;
        }

        /**
         * @param adTag 当前控件承载的广告内容标识符（与动态事件定义一致）
         */
        public AdMonitorAttr setAdTag(String adTag) {
            this.adTag = adTag;
            return this;
        }

        /**
         * @return 曝光地址列表，广告展示后循环逐一调用曝光检测地址列表，get请求，无参
         */
        public List<String> getShowRptUrls() {
            return showRptUrls;
        }

        /**
         * @param showRptUrls 曝光地址列表，广告展示后循环逐一调用曝光检测地址列表，get请求，无参
         */
        public AdMonitorAttr setShowRptUrls(List<String> showRptUrls) {
            this.showRptUrls = showRptUrls;
            return this;
        }

        /**
         * @return 点击地址列表，用户点击广告后循环逐一调用点击检测地址列表，get请求，无参
         */
        public List<String> getClickRptUrls() {
            return clickRptUrls;
        }

        /**
         * @param clickRptUrls 点击地址列表，用户点击广告后循环逐一调用点击检测地址列表，get请求，无参
         */
        public AdMonitorAttr setClickRptUrls(List<String> clickRptUrls) {
            this.clickRptUrls = clickRptUrls;
            return this;
        }

        @NonNull
        @Override
        public String toString() {
            return "{" +
                    "placeEventId:" + placeEventId +
                    ", indexInfo:" + indexInfo +
                    ", adTag:" + adTag +
                    ", showRptUrls:" + showRptUrls +
                    ", clickRptUrls:" + clickRptUrls +
                    '}';
        }
    }
}
