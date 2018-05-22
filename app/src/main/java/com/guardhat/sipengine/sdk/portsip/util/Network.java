/*
 * Copyright (C) 2018 GuardHat Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * GuardHat Inc. This software may not be used and/or copied without explicit
 * written permission of GuardHat Inc., and only in accordance with the terms
 * and conditions stipulated in the license agreement and/or contract under
 * which the software has been supplied.
*/

package com.guardhat.sipengine.sdk.portsip.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network {

    private static final String TAG = Network.class.getCanonicalName();

    private BroadcastReceiver mNetStatusWatcher;

    @FunctionalInterface
    public interface NetWorkChangeListener {
        void handleNetworkChangeEvent(boolean wifiConnect, boolean mobileConnect);
    }

    private final NetWorkChangeListener mNetWorkChangeListener;

    public Network(final Context context, final NetWorkChangeListener listener) {
        super();
        mNetWorkChangeListener = listener;
        if (mNetStatusWatcher == null) {
            mNetStatusWatcher = new BroadcastReceiver() {

                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final NetworkInfo networkInfo;
                    boolean isWifiConnect = false;
                    boolean isMobileConnect = false;
                    final ConnectivityManager connManager = (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connManager != null) {
                        networkInfo = connManager.getActiveNetworkInfo();
                        if ((networkInfo != null) && (networkInfo.getState() == NetworkInfo.State.CONNECTED) &&
                            (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {

                            isWifiConnect = true;
                        }

                        if ((networkInfo != null) && (networkInfo.getState() == NetworkInfo.State.CONNECTED) &&
                            (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
                            isMobileConnect = true;
                        }

                        if (mNetWorkChangeListener != null) {
                            mNetWorkChangeListener.handleNetworkChangeEvent(isWifiConnect, isMobileConnect);
                        }
                    }
                }
            };
        }

        final IntentFilter intentNetWatcher = new IntentFilter();
        intentNetWatcher.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mNetStatusWatcher, intentNetWatcher);
    }
}
