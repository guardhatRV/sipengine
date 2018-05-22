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
import android.support.annotation.NonNull;
import com.guardhat.sipengine.sdk.ISipEventsCallBack;
import com.guardhat.sipengine.sdk.portsip.util.Line;
import com.guardhat.sipengine.sdk.portsip.util.Network;
import com.guardhat.sipengine.sdk.portsip.util.Ring;
import com.guardhat.sipengine.sdk.portsip.util.SipContact;
import com.portsip.OnPortSIPEvent;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Guardhat Inc.
 * @since 2018
 */
public class PortSipEvents implements OnPortSIPEvent {

    public static final int EXPIRES_TIME = 90;
    public static final int REJECT_CALL_CODE = 486;
    public static final int OUTBOUND_SERVER_PORT = 5060;

    private PortSipSdk mPortSipSdk;
    private final boolean conference = false;
    private boolean mSipLogged = false;
    private boolean mSipOnline = false;

    private ISipEventsCallBack eventsCallBackParam;

    private final Context context;
    private Network mNetManager;
    private static final Line[] CALL_SESSIONS = new Line[Line.MAX_LINES];
    private static final List<SipContact> SIP_CONTACTS = new ArrayList<>();
    private Line currentlyLine = CALL_SESSIONS[Line.LINE_BASE];

    static Line[] getLines() {
        return CALL_SESSIONS;
    }

    public PortSipEvents(@NonNull final Context applicationContext) {
        super();
        this.context = applicationContext;
        init();
    }

    public void setEventsCallBackParam(final ISipEventsCallBack eventsCallBackParam) {
        this.eventsCallBackParam = eventsCallBackParam;
    }

    private void init() {
        mPortSipSdk = new PortSipSdk();
        mNetManager = new Network(context, (wifiConnect, mobileConnect) -> new Thread(() -> {
            synchronized (PortSipEvents.class) {
                if (mSipLogged && (wifiConnect || mobileConnect)) {//need auto reg
                    mPortSipSdk.refreshRegistration(EXPIRES_TIME);
                }
            }
        }).start());

        mPortSipSdk.setOnPortSIPEvent(this);

        for (int i = 0; i < CALL_SESSIONS.length; i++) {
            CALL_SESSIONS[i] = new Line(i);
        }
    }

    public PortSipSdk getPortSIPSDK() {
        return mPortSipSdk;
    }

    private boolean isConference() {
        return conference;
    }

    void setLoginState(final boolean state) {
        mSipLogged = state;

        if (!state) {
        mSipOnline = false;
        }
    }

    public boolean answerSessionCall(final long sessionId, final boolean videoCall) {
        final Line sessionLine = findLineBySessionId(sessionId);
        int rt = PortSipErrorcode.INVALID_SESSION_ID;
        Ring.getInstance().stopRingTone();

        if (sessionId != PortSipErrorcode.INVALID_SESSION_ID) {
            rt = mPortSipSdk.answerCall((sessionLine != null) ? sessionLine.getSessionId() : 0, videoCall);
        }

        if (rt == 0) {
            sessionLine.setSessionState(true);
            setCurrentLine(sessionLine);
            if (videoCall) {
                sessionLine.setVideoState(true);
            } else {
                sessionLine.setVideoState(false);
            }

            if (conference) {
                mPortSipSdk.joinToConference(sessionLine.getSessionId());
                mPortSipSdk.sendVideo(sessionLine.getSessionId(), sessionLine.getVideoState());
            }
            eventsCallBackParam.log("answerSessionCall: " +  sessionLine.getLineName() + ": Call established");
        } else {
            if (sessionLine != null) {
                sessionLine.reset();
            }
            eventsCallBackParam.log("answerSessionCall: " + sessionLine.getLineName() + ": failed to answer call !");
            return false;
        }

        return true;
    }

    public int rejectSessionCall(final long sessionId) {
        final Line sessionLine = findLineBySessionId(sessionId);

        Ring.getInstance().stopRingTone();

        if (sessionId != PortSipErrorcode.INVALID_SESSION_ID) {
            mPortSipSdk.rejectCall(sessionId, REJECT_CALL_CODE);
        }
        if (sessionLine != null) {
            sessionLine.reset();
        }
        Ring.getInstance().stopRingTone();
        eventsCallBackParam.log("rejectSessionCall: Call Rejected");
        eventsCallBackParam.onCallEnded();
        return 0;
    }

    @Override
    public void onRegisterSuccess(final String s, final int code, final String reason) {
        mSipOnline = true;
        eventsCallBackParam.log(String.format(Locale.getDefault(), "Registration Successful %s , %d", reason, code));
        eventsCallBackParam.onRegisterSuccess();
    }

    @Override
    public void onRegisterFailure(final String s, final int code, final String reason) {
        mSipOnline = false;
        eventsCallBackParam.log(String.format(Locale.getDefault(), "Registration failed %s , %d", reason, code));
        eventsCallBackParam.onRegisterFailure();
    }

    @Override
    public void onInviteIncoming(final long sessionId,
                                 final String callerDisplayName,
                                 final String caller,
                                 final String calleeDisplayName,
                                 final String callee,
                                 final String audioCodecs,
                                 final String videoCodecs,
                                 final boolean existsAudio,
                                 final boolean existsVideo,
                                 final String sipMessage) {
        final Line tempSession = findIdleLine();

        if(tempSession == null) {
            mPortSipSdk.rejectCall(sessionId, REJECT_CALL_CODE);
            return;
        }

        tempSession.setRecvCallState(true);
        Ring.getInstance().startRingTone(context);
        tempSession.setSessionId(sessionId);
        tempSession.setVideoState(existsVideo);
        final String comingCallTips = new StringBuilder().append("Call incoming: ").append(callerDisplayName)
                                                         .append("<").append(caller).append(">").toString();
        tempSession.setDescriptionString(comingCallTips);
        setCurrentLine(tempSession);
        eventsCallBackParam.onIncomingCall(sessionId);
        eventsCallBackParam.log("Incoming Call "+ callerDisplayName + "<" + caller + ">" + "Video : " + existsVideo);
    }

    @Override
    public void onInviteTrying(final long sessionId) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (tempSession == null) {
            return;
        }

        tempSession.setDescriptionString("onInviteTrying: Call is trying...");
    }

    @Override
    public void onInviteSessionProgress(final long sessionId,
                                        final String audioCodecs,
                                        final String videoCodecs,
                                        final boolean existsEarlyMedia,
                                        final boolean existsAudio,
                                        final boolean existsVideo,
                                        final String sipMessage) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (Objects.isNull(tempSession)) {
            return;
        }
        if (tempSession != null) {
            tempSession.setSessionState(true);
        }

        tempSession.setDescriptionString("Call session progress.");
        tempSession.setEarlyMedia(existsEarlyMedia);
    }

    @Override
    public void onInviteRinging(final long sessionId,
                                final String statusText,
                                final int statusCode,
                                final String sipMessage) {
        final Line tempSession = findLineBySessionId(sessionId);
        if(tempSession == null) return;

        if (!tempSession.hasEarlyMedia()) {
            Ring.getInstance().startRingBackTone();
        }

        tempSession.setDescriptionString("onInviteRinging: Ringing...");
    }

    @Override
    public void onInviteAnswered(final long sessionId,
                                 final String callerDisplayName,
                                 final String caller,
                                 final String calleeDisplayName,
                                 final String callee,
                                 final String audioCodecs,
                                 final String videoCodecs,
                                 final boolean existsAudio,
                                 final boolean existsVideo,
                                 final String sipMessage) {
        final Line tempSession = findLineBySessionId(sessionId);

        if(tempSession == null) return;

        if (existsVideo) {
            mPortSipSdk.sendVideo(tempSession.getSessionId(), true);
        }
        tempSession.setVideoState(existsVideo);
        tempSession.setSessionState(true);
        tempSession.setDescriptionString("call established");

        if (conference) {
            mPortSipSdk.joinToConference(tempSession.getSessionId());
            mPortSipSdk.sendVideo(tempSession.getSessionId(), tempSession.getVideoState());
            tempSession.setHoldState(false);

        }

        if (tempSession.isReferCall()) {
            tempSession.setReferCall(false, 0);
        }
        Ring.getInstance().stopRingBackTone();
        eventsCallBackParam.log("onInviteAnswered: Call was answered");
    }

    @Override
    public void onInviteFailure(final long sessionId, final String reason,
                                final int code, final String sipMessage) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (Objects.isNull(tempSession)) return;

        tempSession.setDescriptionString("call failure" + reason);

        if (tempSession.isReferCall()) {
            final Line originSession = findLineBySessionId(tempSession.getOriginCallSessionId());
            if (originSession != null) {
                mPortSipSdk.unHold(originSession.getSessionId());
                originSession.setHoldState(false);
                setCurrentLine(originSession);
                tempSession.setDescriptionString("refer failure:" + reason
                                                 + "resume original call");
            }
        }

        tempSession.reset();
        Ring.getInstance().stopRingBackTone();
        eventsCallBackParam.log("onInviteFailure: Connection terminated");
        eventsCallBackParam.onCallEnded();
    }

    @Override
    public void onInviteUpdated(final long sessionId,
                                final String audioCodecs,
                                final String videoCodecs,
                                final boolean existsAudio,
                                final boolean existsVideo,
                                final String sipMessage) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (Objects.isNull(tempSession)) return;
        tempSession.setDescriptionString("onInviteUpdated: Call is updated");
    }

    @Override
    public void onInviteConnected(final long sessionId) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (Objects.isNull(tempSession)) return;

        tempSession.setDescriptionString("Call is connected");
        eventsCallBackParam.log("onInviteConnected: Call is connected");
        eventsCallBackParam.onCallConnected();
    }

    @Override
    public void onInviteBeginingForward(final String forwardTo) {

    }

    @Override
    public void onInviteClosed(final long sessionId) {
        final Line tempSession = findLineBySessionId(sessionId);
        if (Objects.isNull(tempSession)) return;

        Ring.getInstance().stopRingTone();
        tempSession.reset();
        tempSession.setDescriptionString(": Call closed.");
        eventsCallBackParam.log("onInviteClosed: Call Closed");
        eventsCallBackParam.onCallEnded();
    }

    @Override
    public void onDialogStateUpdated(final String s, final String s1, final String s2, final String s3) {

    }

    @Override
    public void onRemoteHold(final long l) {

    }

    @Override
    public void onRemoteUnHold(final long l, final String s, final String s1, final boolean b, final boolean b1) {

    }

    @Override
    public void onReceivedRefer(final long l, final long l1, final String s, final String s1, final String s2) {

    }

    @Override
    public void onReferAccepted(final long l) {

    }

    @Override
    public void onTransferTrying(final long l) {
    }

    @Override
    public void onTransferRinging(final long l) {

    }

    @Override
    public void onReferRejected(final long sessionId, final String reason, final int code) {

    }

    @Override
    public void onACTVTransferSuccess(final long l) {

    }

    @Override
    public void onACTVTransferFailure(final long l, final String s, final int i) {

    }

    @Override
    public void onReceivedSignaling(final long l, final String s) {

    }

    @Override
    public void onSendingSignaling(final long l, final String s) {

    }

    @Override
    public void onWaitingVoiceMessage(final String s, final int i, final int i1, final int i2, final int i3) {

    }

    @Override
    public void onWaitingFaxMessage(final String s, final int i, final int i1, final int i2, final int i3) {

    }

    @Override
    public void onRecvDtmfTone(final long l, final int i) {

    }

    @Override
    public void onRecvOptions(final String s) {

    }

    @Override
    public void onRecvInfo(final String s) {

    }

    @Override
    public void onRecvNotifyOfSubscription(final long l, final String s, final byte[] bytes, final int i) {

    }

    @Override
    public void onPresenceRecvSubscribe(final long l, final String s, final String s1, final String s2) {

    }

    @Override
    public void onPresenceOnline(final String s, final String s1, final String s2) {

    }

    @Override
    public void onPresenceOffline(final String s, final String s1) {

    }

    @Override
    public void onRecvMessage(final long l, final String s, final String s1, final byte[] bytes, final int i) {

    }

    @Override
    public void onRecvOutOfDialogMessage(final String s, final String s1, final String s2, final String s3, final String s4, final String s5, final byte[] bytes, final int i, final String s6) {

    }

    @Override
    public void onSendMessageSuccess(final long l, final long l1) {

    }

    @Override
    public void onSendMessageFailure(final long l, final long l1, final String s, final int i) {
    }

    @Override
    public void onSendOutOfDialogMessageSuccess(final long l, final String s, final String s1, final String s2, final String s3) {
    }

    @Override
    public void onSendOutOfDialogMessageFailure(final long l, final String s, final String s1, final String s2, final String s3, final String s4, final int i) {
    }

    @Override
    public void onSubscriptionFailure(final long l, final int i) {
    }

    @Override
    public void onSubscriptionTerminated(final long l) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPlayAudioFileFinished(final long l, final String s) {
    }

    @Override
    public void onPlayVideoFileFinished(final long l) {
    }

    @Override
    public void onReceivedRTPPacket(final long l, final boolean b, final byte[] bytes, final int i) {
    }

    @Override
    public void onSendingRTPPacket(final long l, final boolean b, final byte[] bytes, final int i) {

    }

    @Override
    public void onAudioRawCallback(final long l, final int i, final byte[] bytes, final int i1, final int i2) {

    }

    @Override
    public void onVideoRawCallback(final long l, final int i, final int i1,final int i2,final byte[] bytes, final int i3) {
    }

    private static Line findLineBySessionId(final long sessionId) {
        for (int i = Line.LINE_BASE; i < Line.MAX_LINES; ++i) {
            if (CALL_SESSIONS[i].getSessionId() == sessionId) {
                return CALL_SESSIONS[i];
            }
        }

        return null;
    }

    public static Line findSessionByIndex(final int index) {

        if ((index >= Line.LINE_BASE) && (index < Line.MAX_LINES)) {
            return CALL_SESSIONS[index];
        }

        return null;
    }

    private static Line findIdleLine() {

        for (int i = Line.LINE_BASE; i < Line.MAX_LINES; ++i)
        {
            if (!CALL_SESSIONS[i].getSessionState()
                && !CALL_SESSIONS[i].getRecvCallState()) {
                return CALL_SESSIONS[i];
            }
        }
        return null;
    }

    public void setCurrentLine(final Line line) {
        currentlyLine = Objects.isNull(line) ? CALL_SESSIONS[Line.LINE_BASE] : line;

    }
}
