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
import android.text.TextUtils;

public class UserInfo {

    private static final int MAX_PORT_NUMBER = 65535;
    private static final int DEFAULT_SIP_PORT = 5060;

    public enum TransportType {
        TCP(0),
        UDP(1),
        TLS(2)
        ;

        private final int type;

        TransportType(final int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public enum SRTPolicyType {

        ENUM_SRTPPOLICY_NONE(0),
        ENUM_SRTPPOLICY_FORCE(1),
        ENUM_SRTPPOLICY_PREFER(2)
        ;

        private final int type;

        SRTPolicyType(final int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    private final String mUsername;
    private final String mUserpwd;
    private final String mSipServer;
    private static final String mUserdomain = "";
    private static final String mAuthName = "";
    private final String mUserDisName;
    private static final String mStunSvr = "";

    private final String mCallee;

    private final int mStunPort = DEFAULT_SIP_PORT;
    private final int msipPort;
    private final TransportType mtransType;
    private final SRTPolicyType msrtpType;

    public UserInfo(@NonNull final String username,
                    @NonNull final String password,
                    @NonNull final String sipServer,
                    @NonNull final String userDisplayName,
                    @NonNull final String callee) {
        this(username,
             password,
             sipServer,
             userDisplayName,
             callee,
             DEFAULT_SIP_PORT,
             TransportType.TCP,
             SRTPolicyType.ENUM_SRTPPOLICY_NONE);
    }

    private UserInfo(@NonNull final String username,
                     @NonNull final String password,
                     @NonNull final String sipServer,
                     @NonNull final String userDisplayName,
                     @NonNull final String callee,
                     final int sipPort,
                     final TransportType transportType,
                     final SRTPolicyType srtpType) {
        super();
        this.mUsername = username;
        this.mUserpwd = password;
        this.mSipServer = sipServer;
        this.mCallee = callee;
        this.msipPort = sipPort;
        this.mUserDisName = userDisplayName;
        this.mtransType = transportType;
        this.msrtpType = srtpType;
    }

    @NonNull
    public String getUserName() {
        return mUsername;
    }

    @NonNull
    public String getUserPassword() {
        return mUserpwd;
    }

    @NonNull
    public String getSipServer() {
        return mSipServer;
    }

    public String getUserDisplayName() {
        return mUserDisName;
    }

    public String getUserdomain() {
        return mUserdomain;
    }

    public String getAuthName() {
        return mAuthName;
    }

    public int getSipPort() {
        return msipPort;
    }

    public String getStunServer() {
        return mStunSvr;
    }

    public int getStunPort() {
        return mStunPort;
    }

    public TransportType getTransType() {
        return mtransType;
    }

    public SRTPolicyType getSrtpType() {
        return msrtpType;
    }

    public boolean isValid() {
        if (!TextUtils.isEmpty(mUsername)) {
            if (!TextUtils.isEmpty(mUserpwd)) {
                if (msipPort > 0) if (msipPort < MAX_PORT_NUMBER) if (!TextUtils.isEmpty(mSipServer)) return true;
            }
        }
        return false;
    }
}
