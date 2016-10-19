package com.siyanhui.mojif.bqliveapp;

import android.app.Application;

import com.siyanhui.mojif.bqlive.BQLive;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * Demo Application
 * Created by fantasy on 16/9/28.
 */
public class BQLApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * 用AppID和AppSecret初始化SDK
         */
        BQLive.init(this, "15e0710942ec49a29d2224a6af4460ee", "b11e0936a9d04be19300b1d6eec0ccd5");
//        BQLive.thirdPartyInit(this, "97790e9a809a41c7aa523ba5fa019f25", "ff80808144b90b280144be81b1740001");
        CrashReport.initCrashReport(this);
    }
}
