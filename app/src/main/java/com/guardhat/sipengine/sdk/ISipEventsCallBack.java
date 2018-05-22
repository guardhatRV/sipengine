/*
 * Copyright (C) 2018 GuardHat Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * GuardHat Inc. This software may not be used and/or copied without explicit
 * written permission of GuardHat Inc., and only in accordance with the terms
 * and conditions stipulated in the license agreement and/or contract under
 * which the software has been supplied.
 *
 */
package com.guardhat.sipengine.sdk;

import android.support.annotation.NonNull;

/**
 * Callback interface for inbound calls.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public interface ISipEventsCallBack {

    void onCallConnected();

    void onCallEnded();

    void onRegisterSuccess();

    void onRegisterFailure();

    void onIncomingCall(long sessionId);

    void log(@NonNull String message);

}
