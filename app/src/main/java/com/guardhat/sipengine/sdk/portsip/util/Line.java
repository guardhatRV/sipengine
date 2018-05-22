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

/**
 * Holds the line data while a call is in progress.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public class Line extends Session {
    private int index = 0;
    public static final int LINE_BASE = 0;
    public static final int MAX_LINES = 8;
    private String mDescriptionString = "";

    public Line(final int index) {
        super();
        this.index = index;
    }

    private String getStatusString() {
        String status = "";
        if (!getSessionState() && !getRecvCallState()) {
            status += mDescriptionString;
            return status;
        }
        if (getSessionState()) {
            status += " busy";
        } else {
            status += " idle";
            return status;
        }
        status += getHoldState() ? " Hold" : " UnHold";

        if (getConferenceState()) {
            status += " conference";
        }

        if (getReferState()) {
            status += " refered";
        }
        return status;
    }

    public String getLineName() {
        return "line-" + index;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        super.reset();
        mDescriptionString = "";
    }

    private String getDescriptionString() {
        return mDescriptionString;
    }

    public void setDescriptionString(final String descriptionString) {
        this.mDescriptionString = descriptionString;
    }

    @Override
    public String toString() {
        return "Line{" +
               "index=" + index +
               ", mDescriptionString='" + mDescriptionString + '\'' +
               '}';
    }
}
