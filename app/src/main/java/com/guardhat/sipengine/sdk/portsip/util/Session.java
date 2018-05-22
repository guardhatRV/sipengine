/*
 * Copyright (C) 2018, GuardHat Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * GuardHat Inc. This software may not be used and/or copied without explicit
 * written permission of GuardHat Inc., and only in accordance with the terms
 * and conditions stipulated in the license agreement and/or contract under
 * which the software has been supplied.
 */

package com.guardhat.sipengine.sdk.portsip.util;


import com.portsip.PortSipErrorcode;

import java.util.Observable;

public class Session extends Observable {

    private long mSessionId = PortSipErrorcode.INVALID_SESSION_ID;
    private boolean mHoldState = false;
    private boolean mSessionState = false;
    private boolean mConferenceState = false;
    private boolean mRecvCallState = false;
    private boolean mHasEarlyMedia = false;
    private boolean mVideoState = false;
    private boolean mIsReferCall = false;
    private long mOriginSessionId = PortSipErrorcode.INVALID_SESSION_ID;

    public void reset() {
        mSessionId = PortSipErrorcode.INVALID_SESSION_ID;
        mHoldState = false;
        mSessionState = false;
        mConferenceState = false;
        mRecvCallState = false;
        mHasEarlyMedia = false;
        mVideoState = false;
        mIsReferCall = false;
        mOriginSessionId = PortSipErrorcode.INVALID_SESSION_ID;
    }

    public boolean hasEarlyMedia() {
        return mHasEarlyMedia;
    }

    public void setEarlyMedia(final boolean earlyMedia) {
        mHasEarlyMedia = earlyMedia;
    }

    public boolean isReferCall() {
        return mIsReferCall;
    }

    public long getOriginCallSessionId() {
        return mOriginSessionId;
    }

    public void setReferCall(final boolean referCall, final long l) {
        mIsReferCall = referCall;
        mOriginSessionId = l;
    }

    public void setSessionId(final long sessionId) {
        mSessionId = sessionId;
    }

    public long getSessionId() {
        return mSessionId;
    }

    public void setHoldState(final boolean state) {
        mHoldState = state;
    }

    public boolean getHoldState() {
        return mHoldState;
    }

    public void setSessionState(final boolean state) {
        mSessionState = state;
    }

    public boolean getSessionState() {
        return mSessionState;
    }

    public void setRecvCallState(final boolean state) {
        mRecvCallState = state;
    }

    public boolean getRecvCallState() {
        return mRecvCallState;
    }

    public void setVideoState(final boolean state) {
        mVideoState = state;
    }

    public boolean getVideoState() {
        return mVideoState;
    }

    public boolean getReferState() {
        return mIsReferCall;
    }

    boolean getConferenceState() {
        return mConferenceState;
    }
}
