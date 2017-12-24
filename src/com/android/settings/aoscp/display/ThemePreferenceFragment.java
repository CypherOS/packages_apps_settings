/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.aoscp.display;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ThemePreferenceFragment extends RadioButtonPickerFragment {

    private static final String TAG = "ThemePreference";
	
    static final String KEY_THEME_DEFAULT = "theme_default";
    static final String KEY_THEME_TEAL = "theme_teal";
	static final String THEME_TEAL = "com.google.android.theme.teal";
	
	private OverlayManager mOverlayService;
	private IOverlayManager mOverlayManager;
	private PackageManager mPackageManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
		mOverlayService = new OverlayManager();
		mPackageManager = context.getPackageManager();
    }
	
	@Override
    public void onCreatePreferences(Bundle savedInstanceState, String key) {
        super.onCreatePreferences(savedInstanceState, key);
        addPreferencesFromResource(R.xml.theme_settings);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context ctx = getContext();
        return Arrays.asList(
            new ThemeCandidateInfo(ctx.getString(R.string.theme_default),
                    KEY_THEME_DEFAULT),
            new ThemeCandidateInfo(ctx.getString(R.string.theme_teal),
                    KEY_THEME_TEAL)
        );
    }

    @Override
    protected String getDefaultKey() {
		return KEY_THEME_DEFAULT;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_THEME_DEFAULT:
                restoreDefaultTheme();
                break;
            case KEY_THEME_TEAL:
			    try {
                    mOverlayService.setEnabled(THEME_TEAL,
                            true, UserHandle.myUserId());
				} catch (RemoteException e) {
					Log.w(TAG, "Can't change theme", e);
				}
                break;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DISPLAY;
    }
	
	private void restoreDefaultTheme() {
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).isEnabled() &&
                        isChangeableOverlay(infos.get(i).packageName)) {
                    mOverlayService.setEnabled(infos.get(i).packageName, false, UserHandle.myUserId());
                }
            }
        } catch (RemoteException e) {
        }
    }
	
	private boolean isChangeableOverlay(String packageName) {
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            return pi != null && !pi.isStaticOverlay;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
	
	public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabledExclusive(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabledExclusive(pkg, enabled, userId);
        }
		
		public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }

    static class ThemeCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        ThemeCandidateInfo(CharSequence label, String key) {
            super(true);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

}
