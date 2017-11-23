/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2017 CypherOS
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
package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.aoscp.fuelgauge.BatteryLightSettings;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnStart;
import com.android.settings.core.lifecycle.events.OnStop;
import com.android.settings.widget.MasterSwitchPreference;

import static android.provider.Settings.System.BATTERY_LIGHT_ENABLED;

public class BatteryLightPreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String TAG = "BatteryLightPreferenceController";
    private static final boolean DEBUG = false;

    private final BatteryLightStateReceiver mBatteryLightStateReceiver;
    private MasterSwitchPreference mBatteryLightPref;

    public BatteryLightPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        lifecycle.addObserver(this);
        mBatteryLightStateReceiver = new BatteryLightStateReceiver();
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_LIGHT;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryLightPref = (MasterSwitchPreference) screen.findPreference(KEY_BATTERY_LIGHT);
    }

    @Override
    public void updateState(Preference preference) {
        mBatteryLightPref.setChecked(BatteryLightSettings.isBatteryLightEnabled());
        updateSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (Boolean) newValue;
        if (enabled != BatteryLightSettings.isBatteryLightEnabled()
                && !BatteryLightSettings.setBatteryLightEnabled(enabled)) {
            return false;
        }
        updateSummary();
        return true;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.BATTERY_LIGHT_ENABLED)
                , true, mObserver);

        mBatteryStateChangeReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
        mBatteryStateChangeReceiver.setListening(false);
    }

    private void updateSummary() {
        final boolean enabled = BatteryLightSettings.isBatteryLightEnabled();
        final int format = enabled ? R.string.battery_light_title_summary_on
                : R.string.battery_light_title_summary_off;

        final String summary = mContext.getString(format);

        mBatteryLightPref.setSummary(summary);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateSummary();
        }
    };

    private final class BatteryLightStateReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Log.d(TAG, "Received: Battery Light state");
            }
            mBatteryLightPref.setChecked(BatteryLightSettings.isBatteryLightEnabled());
            updateSummary();
        }

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                mContext.registerReceiver(this, null);
                mRegistered = true;
            } else if (!listening && mRegistered) {
                mContext.unregisterReceiver(this);
                mRegistered = false;
            }
        }

    }
}
