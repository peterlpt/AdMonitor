package com.peter.admonitor.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.peter.admonitor.R;
import com.peter.admonitor.manager.AdMonitorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ViewPager vp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startActivity(new Intent(MainActivity.this, ScrollingActivity.class));
            }
        });
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();

        vp = findViewById(R.id.vp);
        List<View> bannerViews = new ArrayList<>();
        AdInfo adInfo1 = new AdInfo();
        adInfo1.adImgRes = R.mipmap.banner_ad_1;
        adInfo1.tvTitle = "横幅广告1";
        adInfo1.landingUrl = "https://devework.com/";
        adInfo1.showRptUrls = Arrays.asList("https://m.banner1.com/show1", "https://m.banner1.com/show2");
        adInfo1.clickRptUrls = Arrays.asList("https://m.banner1.com/click1", "https://m.banner1.com/click2");
        bannerViews.add(genBannerView(adInfo1, 1));
        AdInfo adInfo2 = new AdInfo();
        adInfo2.adImgRes = R.mipmap.banner_ad_2;
        adInfo2.tvTitle = "横幅广告2";
        adInfo2.landingUrl = "https://www.jiawin.com/";
        adInfo2.showRptUrls = Arrays.asList("https://m.banner2.com/show1", "https://m.banner2.com/show2");
        adInfo2.clickRptUrls = Arrays.asList("https://m.banner2.com/click1", "https://m.banner2.com/click2");
        bannerViews.add(genBannerView(adInfo2, 2));
        AdInfo adInfo3 = new AdInfo();
        adInfo3.adImgRes = R.mipmap.banner_ad_3;
        adInfo3.tvTitle = "横幅广告3";
        adInfo3.landingUrl = "https://www.cmhello.com/";
        adInfo3.showRptUrls = Arrays.asList("https://m.banner3.com/show1", "https://m.banner3.com/show2");
        adInfo3.clickRptUrls = Arrays.asList("https://m.banner3.com/click1", "https://m.banner3.com/click2");
        bannerViews.add(genBannerView(adInfo3, 3));
        AdInfo adInfo4 = new AdInfo();
        adInfo4.adImgRes = R.mipmap.banner_ad_4;
        adInfo4.tvTitle = "横幅广告4";
        adInfo4.landingUrl = "https://peterlpt.github.io/";
        adInfo4.showRptUrls = Arrays.asList("https://m.banner4.com/show1", "https://m.banner4.com/show2");
        adInfo4.clickRptUrls = Arrays.asList("https://m.banner4.com/click1", "https://m.banner4.com/click2");
        bannerViews.add(genBannerView(adInfo4, 4));
        vp.setAdapter(new VpAdapter(bannerViews));
        vp.setCurrentItem(0);


        AdMonitorManager.regAdView(findViewById(R.id.iv_ad), new AdMonitorManager.AdMonitorAttr()
                .setPlaceEventId("position_single_ad")
                .setAdTag("图片广告")
                .setIndexInfo("0")
                .setShowRptUrls(Arrays.asList("https://m.imgad.com/show1", "https://m.imgad.com/show2"))
                .setClickRptUrls(Arrays.asList("https://m.imgad.com/click1", "https://m.imgad.com/click2")));
    }

    private View genBannerView(AdInfo adInfo, int index) {
        View view = LayoutInflater.from(this).inflate(R.layout.content_banner_page, null);
        TextView tv = view.findViewById(R.id.tv_banner_ad_title);
        tv.setText(adInfo.tvTitle);
        ImageView iv = view.findViewById(R.id.iv_banner);
        iv.setImageResource(adInfo.adImgRes);
        iv.setTag(adInfo.landingUrl);
        AdMonitorManager.regAdView(iv, new AdMonitorManager.AdMonitorAttr()
                .setPlaceEventId("position_banner")
                .setAdTag(adInfo.tvTitle)
                .setIndexInfo(String.valueOf(index))
                .setShowRptUrls(adInfo.showRptUrls)
                .setClickRptUrls(adInfo.clickRptUrls));
        return view;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        String url = "https://c7sky.com/";
        switch (view.getId()) {
            case R.id.iv_ad:
                url = "https://c7sky.com/";
                break;
            case R.id.iv_banner:
                url = String.valueOf(view.getTag());
                break;
            default:
                break;
        }
        AdMonitorManager.onAdClick(view);
        Intent intent = new Intent(MainActivity.this, AdLandingActivity.class);
        intent.putExtra(AdLandingActivity.URL, url);
        startActivity(intent);
    }

    class VpAdapter extends PagerAdapter {
        private List<View> mViewList;

        public VpAdapter(List<View> mViewList) {
            this.mViewList = mViewList;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(mViewList.get(position));
        }

        @Override
        @NonNull
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));
            return (mViewList.get(position));
        }

        @Override
        public int getCount() {
            if (mViewList == null)
                return 0;
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }
    }

    class AdInfo {
        @DrawableRes
        int adImgRes;
        String tvTitle;
        String landingUrl;
        List<String> showRptUrls;
        List<String> clickRptUrls;
    }
}
