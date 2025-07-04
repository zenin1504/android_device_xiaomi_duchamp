/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;
import android.telecom.DefaultDialerManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.preference.PreferenceManager;

import com.android.settingslib.applications.AppUtils;

import com.xiaomi.settings.utils.FileUtils;

import java.util.List;
import java.util.Map;

public final class ThermalUtils {

    private static final String TAG = "ThermalUtils";
    private static final String THERMAL_CONTROL = "thermal_control_v2";
    private static final String THERMAL_ENABLED = "thermal_enabled";

    public static final int STATE_DEFAULT = 0;
    public static final int STATE_BENCHMARK = 1;
    public static final int STATE_BROWSER = 2;
    public static final int STATE_CAMERA = 3;
    public static final int STATE_DIALER = 4;
    public static final int STATE_GAMING = 5;
    public static final int STATE_NAVIGATION = 6;
    public static final int STATE_STREAMING = 7;
    public static final int STATE_VIDEO = 8;

    private static final Map<Integer, String> THERMAL_STATE_MAP = Map.of(
        STATE_DEFAULT, "0",
        STATE_BENCHMARK, "6",
        STATE_BROWSER, "11",
        STATE_CAMERA, "15",
        STATE_DIALER, "1",
        STATE_GAMING, "19",
        STATE_NAVIGATION, "19",
        STATE_STREAMING, "4",
        STATE_VIDEO, "21"
    );

    private static final String THERMAL_BENCHMARK = "thermal.benchmark=";
    private static final String THERMAL_BROWSER = "thermal.browser=";
    private static final String THERMAL_CAMERA = "thermal.camera=";
    private static final String THERMAL_DIALER = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.gaming=";
    private static final String THERMAL_NAVIGATION = "thermal.navigation=";
    private static final String THERMAL_STREAMING = "thermal.streaming=";
    private static final String THERMAL_VIDEO = "thermal.video=";
    private static final String THERMAL_DEFAULT = "thermal.default=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private static final String GMAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String GMEET_PACKAGE = "com.google.android.apps.tachyon";

    private Context mContext;
    private Display mDisplay;
    private SharedPreferences mSharedPrefs;
    private Intent mServiceIntent;

    private static ThermalUtils sInstance;

    private ThermalUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        WindowManager mWindowManager = context.getSystemService(WindowManager.class);
        mDisplay = mWindowManager.getDefaultDisplay();
        mServiceIntent = new Intent(context, ThermalService.class);
    }

    public static synchronized ThermalUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThermalUtils(context);
        }
        return sInstance;
    }

    /**
     * Returns the current enabled state always reading from shared preferences.
     */
    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(THERMAL_ENABLED, false);
    }

    /**
     * Sets the thermal master switch enabled/disabled. It starts or stops the service as needed.
     */
    public void setEnabled(boolean enabled) {
        // Write the value into SharedPreferences
        mSharedPrefs.edit().putBoolean(THERMAL_ENABLED, enabled).apply();
        dlog("setEnabled: " + enabled);
        if (enabled) {
            startService();
        } else {
            setDefaultThermalProfile();
            stopService();
        }
    }

    public void startService() {
        if (isEnabled()) {
            dlog("startService");
            mContext.startServiceAsUser(mServiceIntent, UserHandle.CURRENT);
        }
    }

    private void stopService() {
        dlog("stopService");
        mContext.stopService(mServiceIntent);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);
        if (value == null || value.isEmpty()) {
            value = THERMAL_BENCHMARK + ":" + THERMAL_BROWSER + ":" + THERMAL_CAMERA + ":" +
                    THERMAL_DIALER + ":" + THERMAL_GAMING + ":" + THERMAL_NAVIGATION + ":" +
                    THERMAL_STREAMING + ":" + THERMAL_VIDEO + ":" + THERMAL_DEFAULT;
            writeValue(value);
        }
        return value;
    }

    public void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");

        switch (mode) {
            case STATE_BENCHMARK:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_BROWSER:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_CAMERA:
                modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_DIALER:
                modes[3] = modes[3] + packageName + ",";
                break;
            case STATE_GAMING:
                modes[4] = modes[4] + packageName + ",";
                break;
            case STATE_NAVIGATION:
                modes[5] = modes[5] + packageName + ",";
                break;
            case STATE_STREAMING:
                modes[6] = modes[6] + packageName + ",";
                break;
            case STATE_VIDEO:
                modes[7] = modes[7] + packageName + ",";
                break;
            case STATE_DEFAULT:
                modes[8] = modes[8] + packageName + ",";
                break;
        }

        String finalString = modes[0] + ":" + modes[1] + ":" + modes[2] + ":" + modes[3] + ":" +
                modes[4] + ":" + modes[5] + ":" + modes[6] + ":" + modes[7] + ":" + modes[8];

        writeValue(finalString);
    }

    public int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;

        if (modes[0].contains(packageName + ",")) {
            state = STATE_BENCHMARK;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_BROWSER;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_CAMERA;
        } else if (modes[3].contains(packageName + ",")) {
            state = STATE_DIALER;
        } else if (modes[4].contains(packageName + ",")) {
            state = STATE_GAMING;
        } else if (modes[5].contains(packageName + ",")) {
            state = STATE_NAVIGATION;
        } else if (modes[6].contains(packageName + ",")) {
            state = STATE_STREAMING;
        } else if (modes[7].contains(packageName + ",")) {
            state = STATE_VIDEO;
        } else if (modes[8].contains(packageName + ",")) {
            state = STATE_DEFAULT;
        } else {
            // derive a default state based on package name
            state = getDefaultStateForPackage(packageName);
        }

        return state;
    }

    public void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_MAP.get(STATE_DEFAULT));
    }

    public void setThermalProfile(String packageName) {
        final int state = getStateForPackage(packageName);
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_MAP.get(state));
    }

    private int getDefaultStateForPackage(String packageName) {
        switch (packageName) {
            case GMAPS_PACKAGE:
                return STATE_NAVIGATION;
            case GMEET_PACKAGE:
                return STATE_STREAMING;
        }

        final PackageManager pm = mContext.getPackageManager();
        final ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return STATE_DEFAULT;
        }

        switch (appInfo.category) {
            case ApplicationInfo.CATEGORY_GAME:
                return STATE_GAMING;
            case ApplicationInfo.CATEGORY_VIDEO:
                return STATE_VIDEO;
            case ApplicationInfo.CATEGORY_MAPS:
                return STATE_NAVIGATION;
        }

        if (AppUtils.isBrowserApp(mContext, packageName, UserHandle.myUserId())) {
            return STATE_BROWSER;
        } else if (DefaultDialerManager.getDefaultDialerApplication(mContext).equals(packageName)) {
            return STATE_DIALER;
        } else if (isCameraApp(packageName)) {
            return STATE_CAMERA;
        } else {
            return STATE_DEFAULT;
        }
    }

    private boolean isCameraApp(String packageName) {
        final Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .setPackage(packageName);

        final List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivitiesAsUser(
                cameraIntent, PackageManager.MATCH_ALL, UserHandle.myUserId());
        for (ResolveInfo info : list) {
            if (info.activityInfo != null) {
                return true;
            }
        }
        return false;
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
