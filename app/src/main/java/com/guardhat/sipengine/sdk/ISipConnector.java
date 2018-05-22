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
import com.guardhat.sipengine.sdk.portsip.util.UserInfo;

/**
 * Main interface for the Telephony Service.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public interface ISipConnector {

    void register(@NonNull UserInfo userInfo,
                  @NonNull ISipEventsCallBack eventsCallBack,
                  @NonNull StackConfiguration stackConfiguration);

    boolean initiateCall(@NonNull String callee, @NonNull boolean enableVideo);

    boolean acceptCall(long sessionId);

    void rejectCall(long sessionId);

    void endCall();

    void deregister();
}
