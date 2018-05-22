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

import android.support.annotation.NonNull;

public class SipContact {
    private String mSipAddr;
    private String mSubDescription = "";
    private boolean mSubstatus;
    private boolean mSubscribed;
    private boolean mAccept;
    private long mSubId;

    public String currentStatusToString() {
        String status = "";
        status += mSubstatus ? ("--online" + mSubDescription) : "--offline";
        status += mSubscribed ? "--Subscribed" : "--unSubscribed";
        status += mAccept ? "--accept" : "--reject";
        return status;
    }

    @NonNull
    public String getSipAddr() {
        return mSipAddr;
    }

    public long getSubId() {
        return mSubId;
    }

    public String getSubDescription() {
        return mSubDescription;
    }

    public boolean isAccept() {
        return mAccept;
    }

    public boolean isSubscribed() {
        return mSubscribed;
    }

    public boolean isSubstatus() {
        return mSubstatus;
    }

    public void setSipAddr(@NonNull final String sipAddr) {
        this.mSipAddr = sipAddr;
    }

    public void setAccept(final boolean accept) {
        this.mAccept = accept;
    }

    public void setSubDescription(final String subDescription) {
        this.mSubDescription = subDescription;
    }

    public void setSubscribed(final boolean subscribed) {
        this.mSubscribed = subscribed;
    }

    public void setSubId(final long subId) {
        this.mSubId = subId;
    }

    public void setSubstatus(final boolean substatus) {
        this.mSubstatus = substatus;
    }
}
