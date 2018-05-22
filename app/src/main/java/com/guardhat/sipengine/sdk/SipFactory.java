package com.guardhat.sipengine.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import com.guardhat.sipengine.sdk.bria.BriaSipCallConnectionAdapter;
import com.guardhat.sipengine.sdk.portsip.PortSipCallConnectionAdapter;

/**
 * @author RaviTeja N.
 * @since 2018
 */

public final class SipFactory {

    static ISipConnector getSipConnector(@NonNull final Context context, @NonNull final String type) {

        if(type.contains("portsip")) {
            return new PortSipCallConnectionAdapter(context);
        }

        if(type.contains("bria")) {
            return new BriaSipCallConnectionAdapter(context);
        }

        throw new IllegalStateException("Please specify the required type of push to talk sdk");
    }
}
