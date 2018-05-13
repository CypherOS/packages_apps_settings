/*
 * Copyright (C) 2018 CypherOS
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

package com.android.settings.aoscp.display;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.aoscp.display.NightDisplayPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.widget.SeekBarPreference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NightDisplayFragment extends DashboardFragment
        implements NightDisplayController.Callback, RadioButtonPreference.OnClickListener {

    private static final String TAG = "NightDisplay";
	
	private static final String KEY_NIGHT_DISPLAY              = "night_display_activated";
	private static final String KEY_NIGHT_DISPLAY_TEMPERATURE  = "night_display_temperature";

	private static final String KEY_NIGHT_DISPLAY_HIGH         = "night_display_high";
	private static final String KEY_NIGHT_DISPLAY_MID          = "night_display_mid";
    private static final String KEY_NIGHT_DISPLAY_LOW          = "night_display_low";
	private static final String KEY_NIGHT_DISPLAY_CUSTOM       = "night_display_custom";
	
	private static int NIGHT_DISPLAY_LOW = 2596;
	private static int NIGHT_DISPLAY_MID = 2850;
	private static int NIGHT_DISPLAY_HIGH = 4082;
	private static int NIGHT_DISPLAY_DEFAULT = 2850;

	private RadioButtonPreference mModes;
    List<RadioButtonPreference> mNightDisplayModes = new ArrayList<>();

    private Context mContext;
	private NightDisplayController mController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NIGHT_DISPLAY_SETTINGS;
    }
    
    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.night_display_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    public void displayResourceTiles() {
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            return;
        }
        addPreferencesFromResource(resId);
        final PreferenceScreen screen = getPreferenceScreen();
        Collection<AbstractPreferenceController> controllers = mPreferenceControllers.values();
        for (AbstractPreferenceController controller : controllers) {
            controller.displayPreference(screen);
        }
		mController = new NightDisplayController(context);
		
		mTemperaturePreference = (SeekBarPreference) findPreference(KEY_NIGHT_DISPLAY_TEMPERATURE);
		mTemperaturePreference.setOnPreferenceChangeListener(this);
		mTemperaturePreference.setMax(convertTemperature(mController.getMinimumColorTemperature()));
        mTemperaturePreference.setContinuousUpdates(true);

        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            if (pref instanceof RadioButtonPreference) {
                RadioButtonPreference nightDisplayModes = (RadioButtonPreference) pref;
                nightDisplayModes.setOnClickListener(this);
                mNightDisplayModes.add(nightDisplayModes);
				mModes = nightDisplayModes;
            }
        }

        switch (mController.getColorTemperature()) {
			case NIGHT_DISPLAY_HIGH:
			    mTemperaturePreference.setEnabled(false);
                updatePresets(KEY_NIGHT_DISPLAY_HIGH);
                break;
			case NIGHT_DISPLAY_MID:
			    mTemperaturePreference.setEnabled(false);
                updatePresets(KEY_NIGHT_DISPLAY_MID);
                break;
            case NIGHT_DISPLAY_LOW:
			    mTemperaturePreference.setEnabled(false);
                updatePresets(KEY_NIGHT_DISPLAY_LOW);
                break;
			case NIGHT_DISPLAY_DEFAULT:
			    mTemperaturePreference.setEnabled(true);
                updatePresets(KEY_NIGHT_DISPLAY_CUSTOM);
                break;
				
        }
    }
	
	@Override
    public void onStart() {
        super.onStart();
        // Listen for changes only while visible.
        mController.setListener(this);

        // Update the current state since it have changed while not visible.
        onActivated(mController.isActivated());
        onColorTemperatureChanged(mController.getColorTemperature());
    }
	
	@Override
    public void onStop() {
        super.onStop();
        // Stop listening for state changes.
        mController.setListener(null);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
		controllers.add(new NightDisplayPreferenceController(context, KEY_NIGHT_DISPLAY));
        return controllers;
    }

    private void updatePresets(String selectionKey) {
        for (RadioButtonPreference pref : mNightDisplayModes) {
            if (selectionKey.equals(pref.getKey())) {
                pref.setChecked(true);
            } else {
                pref.setChecked(false);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference pref) {
        switch (pref.getKey()) {
            case KEY_NIGHT_DISPLAY_HIGH:
                mController.setColorTemperature(NIGHT_DISPLAY_HIGH);
                break;
            case KEY_NIGHT_DISPLAY_MID:
                mController.setColorTemperature(NIGHT_DISPLAY_MID);
                break;
            case KEY_NIGHT_DISPLAY_LOW:
                mController.setColorTemperature(NIGHT_DISPLAY_LOW);
                break;
			case KEY_NIGHT_DISPLAY_CUSTOM:
                mController.setColorTemperature(NIGHT_DISPLAY_MID);
                break;
        }
        updatePresets(pref.getKey());
    }
	
	/**
     * Inverts and range-adjusts a raw value from the SeekBar (i.e. [0, maxTemp-minTemp]), or
     * converts an inverted and range-adjusted value to the raw SeekBar value, depending on the
     * adjustment status of the input.
     */
    private int convertTemperature(int temperature) {
        return mController.getMaximumColorTemperature() - temperature;
    }

	@Override
    public void onActivated(boolean activated) {
		mModes.setEnabled(activated);
        mTemperaturePreference.setEnabled(activated);
    }
	
	@Override
    public void onColorTemperatureChanged(int colorTemperature) {
        mTemperaturePreference.setProgress(convertTemperature(colorTemperature));
    }
	
	@Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTemperaturePreference) {
            return mController.setColorTemperature(convertTemperature((Integer) newValue));
        }
        return false;
    }
}