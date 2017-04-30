/*
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

package com.android.settings.aoscp.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.List;

public class NotificationsAdvanced extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "NotificationsAdvanced";
	
	private static final String STATUS_BAR_SHOW_TICKER = "status_bar_show_ticker";
	private static final String KEY_NOTIFICATION_LIGHT = "notification_light";

    private static final String CATEGORY_NOTIFICATION = "lights_category";

	private ListPreference mTickerNotifications;
    private Preference mNotifLedFragment;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.notification_advanced_settings);
		
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();
		
		final PreferenceCategory leds = (PreferenceCategory) findPreference(CATEGORY_NOTIFICATION);
		
		mTickerNotifications = (ListPreference) findPreference(STATUS_BAR_SHOW_TICKER);
		mTickerNotifications.setOnPreferenceChangeListener(this);
        int tickerMode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER,
                0, UserHandle.USER_CURRENT);
        mTickerNotifications.setValue(String.valueOf(tickerMode));
		updateTickerModeSummary(tickerMode);

        mNotifLedFragment = findPreference(KEY_NOTIFICATION_LIGHT);
        //remove notification led settings if device doesnt support it
        if (!getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            leds.removePreference(findPreference(KEY_NOTIFICATION_LIGHT));
        }
	}
	
	@Override
    protected int getMetricsCategory() {
        return MetricsEvent.ADDITIONS;
    }
	
	@Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mTickerNotifications)) {
            int tickerMode = Integer.parseInt(((String) newValue).toString());
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_TICKER, tickerMode,
                    UserHandle.USER_CURRENT);
            updateTickerModeSummary(tickerMode);
            return true;
        }
        return false;
    }
	
	private void updateTickerModeSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Ticker Disabled
            mTickerNotifications.setSummary(res.getString(R.string.ticker_disabled));
        } else if (value == 1) {
            // Ticker Enabled
            mTickerNotifications.setSummary(res.getString(R.string.ticker_enabled));
		} else if (value == 2) {
            // Ticker Enabled with music capability
            mTickerNotifications.setSummary(res.getString(R.string.ticker_media_enabled));
        } else {
            String mode = res.getString(value == 1
                    ? R.string.ticker_enabled
                    : R.string.ticker_media_enabled);
            mTickerNotifications.setSummary(res.getString(R.string.ticker_settings_title_summary, mode));
        }
	}
}