/*
 * Copyright (C) 2018 GuardHat Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * GuardHat Inc. This software may not be used and/or copied without explicit
 * written permission of GuardHat Inc., and only in accordance with the terms
 * and conditions stipulated in the license agreement and/or contract under
 * which the software has been supplied.
*/
package com.guardhat.sipengine.sdk.portsip;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import com.guardhat.sipengine.sdk.ISipConnector;
import com.guardhat.sipengine.sdk.ISipEventsCallBack;
import com.guardhat.sipengine.sdk.StackConfiguration;
import com.guardhat.sipengine.sdk.portsip.util.Line;
import com.guardhat.sipengine.sdk.portsip.util.Ring;
import com.guardhat.sipengine.sdk.portsip.util.Session;
import com.guardhat.sipengine.sdk.portsip.util.UserInfo;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import java.util.Random;

/**
 * Call Connection Adapter for the PortSip implementation.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public class PortSipCallConnectionAdapter implements ISipConnector {

    private final Context context;
    private static final int ALL_SESSION = -1;
    private ISipEventsCallBack eventsCallBack;
    private static final int currentLine = 0;
    private final PortSipEvents portSipEvents;
    private final int RETRY_COUNT = 3;

    public PortSipCallConnectionAdapter(@NonNull final Context context) {
        super();
        this.context = context;
        portSipEvents = new PortSipEvents(context);
    }

    @Override
    public void register(@NonNull final UserInfo userInfo,
                         @NonNull final ISipEventsCallBack eventsCallBack,
                         @NonNull final StackConfiguration stackConfiguration) {
        this.eventsCallBack = eventsCallBack;
        portSipEvents.setEventsCallBackParam(eventsCallBack);

        if (setUserInfo(userInfo, eventsCallBack) == PortSipErrorcode.ECoreErrorNone) {
            setAVArguments(portSipEvents.getPortSIPSDK(), stackConfiguration);
            if (portSipEvents.getPortSIPSDK().registerServer(PortSipEvents.EXPIRES_TIME, RETRY_COUNT) ==
                PortSipErrorcode.ECoreErrorNone) {
                eventsCallBack.onRegisterSuccess();
                portSipEvents.setLoginState(true);
            } else {
                eventsCallBack.onRegisterFailure();
            }
        }
    }

    @Override
    public boolean initiateCall(@NonNull final String callee, @NonNull final boolean enableVideo) {
        return connectCall(callee, enableVideo);
    }

    @Override
    public boolean acceptCall(final long sessionId) {
        return portSipEvents.answerSessionCall(sessionId, true);
    }

    @Override
    public void rejectCall(final long sessionId) {
        portSipEvents.rejectSessionCall(sessionId);
    }

    @Override
    public void endCall() {
        hangUpCall();
    }

    @Override
    public void deregister() {
        final Line[] mLines = PortSipEvents.getLines();

        for (int i = Line.LINE_BASE; i < Line.MAX_LINES; ++i) {
            if (mLines[i].getRecvCallState()) {
                portSipEvents.getPortSIPSDK().rejectCall(mLines[i].getSessionId(), PortSipEvents.REJECT_CALL_CODE);
            } else if (mLines[i].getSessionState()) {
                portSipEvents.getPortSIPSDK().hangUp(mLines[i].getSessionId());
            }

            mLines[i].reset();
        }
        portSipEvents.setLoginState(false);
        portSipEvents.getPortSIPSDK().unRegisterServer();
        portSipEvents.getPortSIPSDK().DeleteCallManager();
    }

    private int setUserInfo(final UserInfo info,
                            final ISipEventsCallBack eventsCallBack) {
        final String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + '/';

        final int localPort = new Random().nextInt(4940) + 5060;

        if (info.isValid()) {
            portSipEvents.getPortSIPSDK().CreateCallManager(context);

            final String localIP = "0.0.0.0";
            final int result = portSipEvents.getPortSIPSDK().initialize(info.getTransType().getType(), localIP, localPort,
                                                                  PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, logPath,
                                                                  Line.MAX_LINES, "PortSIP VoIP SDK for Android",
                                                                  0, 0, "", "", false, "");
            if (result != PortSipErrorcode.ECoreErrorNone) {
                eventsCallBack.onRegisterFailure();
                return result;
            }

            portSipEvents.getPortSIPSDK().setSrtpPolicy(info.getSrtpType().getType());
            // TODO  - Fix the license
          //  portSipEvents.getPortSIPSDK().setLicenseKey(ConfigurationManager.getTelephonyConfig().getLicense());
            final int userStatus = portSipEvents.getPortSIPSDK()
                                  .setUser(info.getUserName(), info.getUserDisplayName(), info.getAuthName(),
                                           info.getUserPassword(),
                                           info.getUserdomain(), info.getSipServer(), info.getSipPort(),
                                                info.getStunServer(), info.getStunPort(), null,
                                                PortSipEvents.OUTBOUND_SERVER_PORT);

            if (userStatus != PortSipErrorcode.ECoreErrorNone) {
                eventsCallBack.onRegisterFailure();
                return userStatus;
            }
        } else {
            return PortSipErrorcode.INVALID_SESSION_ID;
        }

        return PortSipErrorcode.ECoreErrorNone;
    }

    private static void setAVArguments(@NonNull final PortSipSdk sdk,
                                       @NonNull final StackConfiguration stackConfiguration) {

        sdk.clearAudioCodec();
        stackConfiguration.getAudioCodec().forEach(sdk::addAudioCodec);
        stackConfiguration.getVideoCodec().forEach(sdk::addVideoCodec);

        sdk.setVideoNackStatus(true);
        sdk.enableVAD(true);
        sdk.enableAEC(true);
        sdk.enableANS(true);
        sdk.enableAGC(true);
        sdk.enableCNG(true);

        sdk.setVideoResolution(stackConfiguration.getFrameWidth(),
                               stackConfiguration.getFrameHeight());
        sdk.setVideoBitrate(ALL_SESSION, stackConfiguration.getBitrate());
        sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE);
        sdk.setVideoDeviceId(1);
    }

    private boolean connectCall(final String caller, final boolean enableVideo) {
        if (caller.length() <= 0) {
            eventsCallBack.log("The phone number is empty.");
            return false;
        }

        final Line[] mLines = PortSipEvents.getLines();
        final Session currentSession = PortSipEvents.findSessionByIndex(currentLine);
        assert currentSession != null;
        if (currentSession.getSessionState() || currentSession.getRecvCallState()) {
            eventsCallBack.log("Current line is busy now, please switch a line.");
            return false;
        }

        if (portSipEvents.getPortSIPSDK().isAudioCodecEmpty()) {
            eventsCallBack.log("Audio Codec Empty,add audio codec at first");
            return false;
        }

        final long sessionId = portSipEvents.getPortSIPSDK().call(caller, true, enableVideo);
        if (sessionId <= 0) {
            eventsCallBack.log("Call Failure");
            return false;
        }

        currentSession.setSessionId(sessionId);
        currentSession.setSessionState(true);
        currentSession.setVideoState(enableVideo);
        portSipEvents.setCurrentLine(mLines[currentLine]);
        return true;
    }

    private void hangUpCall() {
        final Session currentSession = PortSipEvents.findSessionByIndex(currentLine);
        Ring.getInstance().stopRingTone();
        Ring.getInstance().stopRingBackTone();
        assert currentSession != null;
        if (currentSession.getRecvCallState()) {
            portSipEvents.getPortSIPSDK().rejectCall(currentSession.getSessionId(), PortSipEvents.REJECT_CALL_CODE);
            currentSession.reset();
            eventsCallBack.onCallEnded();
            return;
        }

        if (currentSession.getSessionState()) {
            portSipEvents.getPortSIPSDK().hangUp(currentSession.getSessionId());
            currentSession.reset();
            eventsCallBack.onCallEnded();
        }
    }
}
