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
 * limitations under the License
 */
package com.android.settings.aoscp.wifi;

import android.annotation.DrawableRes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.NetworkBadging;
import android.net.wifi.WifiConfiguration;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.TronUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.wifi.AccessPoint;

public class ConnectedAPSignalPreference extends Preference {

    private static final int[] STATE_SECURED = {
            com.android.settingslib.R.attr.state_encrypted
    };

    private static final int[] STATE_METERED = {
            com.android.settingslib.R.attr.state_metered
    };

    private static final int[] FRICTION_ATTRS = {
            com.android.settingslib.R.attr.wifi_friction
    };

    private final StateListDrawable mFrictionSld;
    private final int mBadgePadding;
    private final UserBadgeCache mBadgeCache;
    private TextView mTitleView;

    private boolean mForSavedNetworks = false;
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private int mLevel;
    private int mDefaultIconResId;
	
	private String[] mSignalStr;

    public static String generatePreferenceKey(AccessPoint accessPoint) {
        StringBuilder builder = new StringBuilder();

        if (TextUtils.isEmpty(accessPoint.getSsidStr())) {
            builder.append(accessPoint.getBssid());
        } else {
            builder.append(accessPoint.getSsidStr());
        }

        builder.append(',').append(accessPoint.getSecurity());
        return builder.toString();
    }

    // Used for dummy pref.
    public ConnectedAPSignalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFrictionSld = null;
        mBadgePadding = 0;
        mBadgeCache = null;
    }

    public ConnectedAPSignalPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache,
            boolean forSavedNetworks) {
        super(context);
        setWidgetLayoutResource(R.layout.connected_ap_signal_friction_widget);
        mBadgeCache = cache;
        mAccessPoint = accessPoint;
        mForSavedNetworks = forSavedNetworks;
        mAccessPoint.setTag(this);
        mLevel = -1;

        TypedArray frictionSld;
        try {
            frictionSld = context.getTheme().obtainStyledAttributes(FRICTION_ATTRS);
        } catch (Resources.NotFoundException e) {
            // Fallback for platforms that do not need friction icon resources.
            frictionSld = null;
        }
        mFrictionSld = frictionSld != null ? (StateListDrawable) frictionSld.getDrawable(0) : null;

        // Distance from the end of the title at which this AP's user badge should sit.
        mBadgePadding = context.getResources()
                .getDimensionPixelSize(com.android.settingslib.R.dimen.wifi_preference_badge_padding);
				
		mSignalStr = context.getResources().getStringArray(R.array.wifi_signal);
        refresh();
    }

    public ConnectedAPSignalPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache,
            int iconResId, boolean forSavedNetworks) {
        super(context);
        setWidgetLayoutResource(R.layout.connected_ap_signal_friction_widget);
        mBadgeCache = cache;
        mAccessPoint = accessPoint;
        mForSavedNetworks = forSavedNetworks;
        mAccessPoint.setTag(this);
        mLevel = -1;
        mDefaultIconResId = iconResId;

        TypedArray frictionSld;
        try {
            frictionSld = context.getTheme().obtainStyledAttributes(FRICTION_ATTRS);
        } catch (Resources.NotFoundException e) {
            // Fallback for platforms that do not need friction icon resources.
            frictionSld = null;
        }
        mFrictionSld = frictionSld != null ? (StateListDrawable) frictionSld.getDrawable(0) : null;

        // Distance from the end of the title at which this AP's user badge should sit.
        mBadgePadding = context.getResources()
                .getDimensionPixelSize(com.android.settingslib.R.dimen.wifi_preference_badge_padding);
				
		mSignalStr = context.getResources().getStringArray(R.array.wifi_signal);
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mAccessPoint == null) {
            // Used for dummy pref.
            return;
        }
        Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(mLevel);
        }

        mTitleView = (TextView) view.findViewById(android.R.id.title);
        if (mTitleView != null) {
            // Attach to the end of the title view
            mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mBadge, null);
            mTitleView.setCompoundDrawablePadding(mBadgePadding);
        }

        ImageView frictionImageView = (ImageView) view.findViewById(R.id.friction_icon);
        bindFrictionImage(frictionImageView);
    }

    protected void updateIcon(int level, Context context) {
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_settings_wifi_signal_meter);
        if (drawable != null) {
            drawable.setTint(getContext().getResources().getColor(getSignalColor(level)));
            setIcon(drawable);
        }
    }

    /**
     * Binds the friction icon drawable using a StateListDrawable.
     *
     * <p>Friction icons will be rebound when notifyChange() is called, and therefore
     * do not need to be managed in refresh()</p>.
     */
    private void bindFrictionImage(ImageView frictionImageView) {
        if (frictionImageView == null || mFrictionSld == null) {
            return;
        }
        if (mAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
            mFrictionSld.setState(STATE_SECURED);
        } else if (mAccessPoint.isMetered()) {
            mFrictionSld.setState(STATE_METERED);
        }
        Drawable drawable = mFrictionSld.getCurrent();
        frictionImageView.setImageDrawable(drawable);
    }

    protected void updateBadge(Context context) {
        WifiConfiguration config = mAccessPoint.getConfig();
        if (config != null) {
            // Fetch badge (may be null)
            // Get the badge using a cache since the PM will ask the UserManager for the list
            // of profiles every time otherwise.
            mBadge = mBadgeCache.getUserBadge(config.creatorUid);
        }
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        setTitle(this, mAccessPoint, mForSavedNetworks);
        final Context context = getContext();
        int level = mAccessPoint.getLevel();
        if (level != mLevel) {
            mLevel = level;
            updateIcon(mLevel, context);
            notifyChanged();
        }

        updateBadge(context);

        setSummary(getContext().getResources().getString(R.string.connected_signal_pref_summary));
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            // Let our BG thread callbacks call setTitle/setSummary.
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    @VisibleForTesting
    public void setTitle(ConnectedAPSignalPreference preference, AccessPoint ap, boolean savedNetworks) {
		int summarySignalLevel = ap.getLevel();
        preference.setTitle(String.format(getContext().getResources().getString(
		        R.string.connected_signal_pref_title), mSignalStr[summarySignalLevel]));
    }

    public void onLevelChanged() {
        postNotifyChanged();
    }

    private void postNotifyChanged() {
        if (mTitleView != null) {
            mTitleView.post(mNotifyChanged);
        } // Otherwise we haven't been bound yet, and don't need to update.
    }

    private final Runnable mNotifyChanged = new Runnable() {
        @Override
        public void run() {
            notifyChanged();
        }
    };

	@DrawableRes 
	private int getSignalColor(int signalLevel) {
        switch (signalLevel) {
            case 0:
                return R.color.ic_signal_meter_poor;
            case 1:
                return R.color.ic_signal_meter_poor;
            case 2:
                return R.color.ic_signal_meter_fair;
            case 3:
                return R.color.ic_signal_meter_poor; // Change me to ic_signal_meter_good
            case 4:
                return R.color.ic_signal_meter_excellent;
            default:
                throw new IllegalArgumentException("Invalid signal level: " + signalLevel);
        }
    }

    public static class UserBadgeCache {
        private final SparseArray<Drawable> mBadges = new SparseArray<>();
        private final PackageManager mPm;

        public UserBadgeCache(PackageManager pm) {
            mPm = pm;
        }

        private Drawable getUserBadge(int userId) {
            int index = mBadges.indexOfKey(userId);
            if (index < 0) {
                Drawable badge = mPm.getUserBadgeForDensity(new UserHandle(userId), 0 /* dpi */);
                mBadges.put(userId, badge);
                return badge;
            }
            return mBadges.valueAt(index);
        }
    }
}
