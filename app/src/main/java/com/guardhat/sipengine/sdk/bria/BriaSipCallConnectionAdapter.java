/*
 * Copyright (C) 2018 GuardHat Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * GuardHat Inc. This software may not be used and/or copied without explicit
 * written permission of GuardHat Inc., and only in accordance with the terms
 * and conditions stipulated in the license agreement and/or contract under
 * which the software has been supplied.
*/
package com.guardhat.sipengine.sdk.bria;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import com.counterpath.sdk.*;
import com.counterpath.sdk.android.PermissionsHandler;
import com.counterpath.sdk.android.SipAudioAndroid;
import com.counterpath.sdk.android.SipPhoneAndroid;
import com.counterpath.sdk.android.SipVideoAndroid;
import com.counterpath.sdk.handler.SipAccountHandler;
import com.counterpath.sdk.handler.SipConversationHandler;
import com.counterpath.sdk.handler.SipPhoneHandler;
import com.counterpath.sdk.handler.SipVideoHandler;
import com.counterpath.sdk.pb.Account;
import com.counterpath.sdk.pb.Media;
import com.counterpath.sdk.pb.Phone;
import com.counterpath.sdk.pb.Video;
import com.guardhat.sipengine.sdk.ISipConnector;
import com.guardhat.sipengine.sdk.ISipEventsCallBack;
import com.guardhat.sipengine.sdk.StackConfiguration;
import com.guardhat.sipengine.sdk.portsip.util.UserInfo;
import com.guardhat.sipengine.sdk.portsip.util.UserInfo.TransportType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static com.counterpath.sdk.pb.Conversation.*;

/**
 * Call Connection Adapter for the Bria SDK implementation.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public class BriaSipCallConnectionAdapter implements ISipConnector, PermissionsHandler {

    public static final int KEEP_ALIVE_TIME = 270;
    public static final int UDP_KEEP_ALIVE_TIME = 30;
    private SipAccount account = null;
    private SipConversation activeConversation = null;
    private SipPhoneAndroid phone = null;
    private StackConfiguration stackConfiguration;

    private final Context context;
    private final Collection<BriaVideoCodecs> videoCodecs =  new ArrayList<>();

    private ISipEventsCallBack eventsCallBack;
    private UserInfo userInfo;

    public BriaSipCallConnectionAdapter(@NonNull final Context context) {
        super();
        this.context = context;
    }

    private void initializeVideo() {
        final SipVideoAndroid videoApi = SipVideoAndroid.get(phone);

        if (checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager
                .PERMISSION_GRANTED) {
            return;
        }
        videoApi.queryDeviceList();

        videoApi.addHandler(new SipVideoHandler.SipVideoHandlerAdapter() {
            @Override
            public void onVideoCodecListUpdatedEvent(@NonNull final SipVideo video, @NonNull final Video.VideoEvents
                    .VideoCodecListUpdatedEvent
                    events) {
                videoApi.removeHandler(this);

                final List<Video.VideoCodecInfo> codecs = events.getCodecInfoList();

                for (int i = 0; i < codecs.size(); i++) {
                    videoCodecs.add(new BriaVideoCodecs(codecs.get(i).getId(), codecs.get(i).getCodecName()));
                }
                setPreferredResolution(stackConfiguration.getQuality());
            }
        });
        videoApi.queryCodecList();

        if (checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager
                .PERMISSION_GRANTED) {
            return;
        }
        SipVideoAndroid.get(phone).startCapture();
    }

    private void setPreferredResolution(final int selectedResolution) {
        if ((phone == null) || (SipVideoAndroid.get(phone) == null)) return;

        for (final BriaVideoCodecs videoCodec : videoCodecs) {
            SipVideoAndroid.get(phone).setPreferredResolution(videoCodec.getVideoCodecId(), selectedResolution);
        }
    }

    private void initializePhone(@NonNull final String license) {
        if (phone == null) {

            if ((checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager
                    .PERMISSION_GRANTED)
                || (checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager
                    .PERMISSION_GRANTED)) {
                return;
            }
            phone = new SipPhoneAndroid(context, license,true);
            phone.addPermissionsHandler(this);

            final Media.MediaStackSettings mediaSettings = new Media.MediaStackSettings();
            MediaManager.get(phone).initializeMediaStack(mediaSettings);

            phone.addHandler(phoneHandler);
            phone.setLoggingEnabled(true);

            phone.enableBackgroundingSupport(phone.handle());

            phone.enableNetworkChangeManager(phone.handle());

            SipAudioAndroid.get(phone).setAudioFocusMode(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

            initializeVideo();
        }
    }

    @Override
    public void register(@NonNull final UserInfo userInfo,
                         @NonNull final ISipEventsCallBack eventsCallBack,
                         @NonNull final StackConfiguration stackConfiguration) {
        this.eventsCallBack = eventsCallBack;
        this.userInfo = userInfo;
        this.stackConfiguration = stackConfiguration;

        // TODO Fix the license here too
        //initializePhone(ConfigurationManager.getTelephonyConfig().getLicense());

        final Account.AccountSettings settings = new Account.AccountSettings()
                .setUsername(userInfo.getUserName())
                .setDomain(userInfo.getSipServer())
                .setPassword(userInfo.getUserPassword())
                .setAuthUsername(userInfo.getAuthName())
                .setUseOutbound(false)
                .setUseRport(false);

        final TransportType trans = userInfo.getTransType();

        switch (trans) {
            case TCP:
                settings.setSipTransportType(Account.TransportType_TCP);
                break;
            case UDP:
                settings.setSipTransportType(Account.TransportType_UDP);
                break;
            case TLS:
                settings.setSipTransportType(Account.TransportType_TLS);
                break;
        }

        final SipAccountApi accountApi = SipAccountApi.get(phone);
        account = accountApi.newAccount(settings);

        settings.setUdpKeepAliveTime(UDP_KEEP_ALIVE_TIME);
        settings.setTcpKeepAliveTime(KEEP_ALIVE_TIME);
        account.configureTransportAccountSettings(settings, Phone.TransportWWAN);

        settings.setUdpKeepAliveTime(UDP_KEEP_ALIVE_TIME);
        settings.setTcpKeepAliveTime(KEEP_ALIVE_TIME);
        account.configureTransportAccountSettings(settings, Phone.TransportWiFi);

        account.addHandler(accountHandler);
        final SipConversationApi conversationApi = SipConversationApi.get(account);
        final ConversationSettings convoSettings = new ConversationSettings();
        convoSettings.setNatTraversalMode(NatTraversalMode_Auto);
        convoSettings.setNatTraversalServerSource(NatTraversalServerSource_SRV);
        conversationApi.setDefaultSettings(convoSettings);

        conversationApi.addHandler(conversationHandler);

        setPreferredResolution(stackConfiguration.getQuality());

        account.enable();

        eventsCallBack.log("onRegister() register was successful");
    }

    private void onUnregister()
    {
        if(activeConversation!=null) {
            activeConversation.end();
        }
        account.disable();
        eventsCallBack.log("onUnregister() register was successful");
    }

    private boolean onCall(@NonNull final String to, @NonNull final boolean enableVideo) {

        final StringBuilder buffer = new StringBuilder(to);

        if (!buffer.toString().startsWith("sip:")) {
            buffer.append("sip:").append(to);
        }

        if (!buffer.toString().contains("@"))
        {
            buffer.append('@').append(userInfo.getSipServer());
        }

        final SipConversationApi conversationApi = SipConversationApi.get(account);
        activeConversation = conversationApi.newConversation();
        activeConversation.addParticipant(buffer.toString());
        activeConversation.setMediaEnabled(MediaType_Video, enableVideo);
        activeConversation.start();
        eventsCallBack.log("Call is trying to connect");

        eventsCallBack.log("onCall() call started to: " + buffer);
        eventsCallBack.onCallConnected();
        return true;
    }

    private void onEndCall()
    {
        activeConversation.end();
        activeConversation = null;
        eventsCallBack.log("onEndCall() call ended");
        eventsCallBack.onCallEnded();
    }

    @Override
    public boolean initiateCall(@NonNull final String callee, @NonNull final boolean enableVideo) {
       return onCall(callee, enableVideo);
    }

    @Override
    public boolean acceptCall(final long sessionId) {

        if (activeConversation == null) {
            eventsCallBack.log("Incoming call cannot be accepted");
            return false;
        }

        activeConversation.accept();
        return true;
    }

    @Override
    public void rejectCall(final long sessionId) {

        if (activeConversation == null) {
            eventsCallBack.log("No active call in session");
            return;
        }

        activeConversation.reject();
    }

    @Override
    public void endCall() {
        onEndCall();
    }

    @Override
    public void deregister() {
        onUnregister();
    }

    @Override
    public void requestPermissions(final int i, final String[] strings) {

    }

    private final SipPhoneHandler phoneHandler = new SipPhoneHandler.SipPhoneHandlerAdapter() {
        @Override
        public void onErrorEvent(@NonNull final SipPhone phone, @NonNull final Phone.PhoneEvents.PhoneErrorEvent msg) {
            eventsCallBack.log("Phone onErrorEvent MSG: " + msg.getErrorText());
        }

        @Override
        public void onLogEvent(@NonNull final SipPhone sipPhone, @NonNull final Phone.PhoneEvents.PhoneLogEvent
                logEvent) {
            eventsCallBack.log('[' + logEvent.getSubsystem().trim() + "] " + logEvent.getLogMessage());

        }

        @Override
        public void onLicensingErrorEvent(@NonNull final SipPhone phoneApi, @NonNull final Phone.PhoneEvents
                .PhoneLicensingErrorEvent event) {
            eventsCallBack.log("onLicensingError() with MSG: " + event.getErrorText());
            phone = null;
        }
    };

    private final SipAccountHandler accountHandler = new SipAccountHandler.SipAccountHandlerAdapter() {
        @Override
        public void onSipAccountStatusChangedEvent(@NonNull final SipAccount account, @NonNull final Account
                                                   .AccountEvents
                .AccountStatusChangedEvent msg) {
            // update ui state based on new account registration status
            switch (msg.getAccountStatus()) {
                case Account.AccountStatus_Registered:
                    eventsCallBack.onRegisterSuccess();
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to Registered : " + msg
                            .getSignalingStatusCode() + ' ' + msg.getSignalingResponseText());
                    break;
                case Account.AccountStatus_Unregistered:

                    eventsCallBack.onRegisterFailure();
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to UnRegistered : " +
                                       msg.getSignalingStatusCode() + ' '
                                       + msg.getSignalingResponseText());
                    break;
                case Account.AccountStatus_Registering:
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to Registering");
                    break;
                case Account.AccountStatus_Unregistering:
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to Unregistering");
                    break;
                case Account.AccountStatus_WaitingToRegister:
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to Waiting to Register : " +
                                       msg.getSignalingStatusCode() + ' '
                                       + msg.getSignalingResponseText());
                    break;
                default:
                    eventsCallBack.log("onSipAccountStatusChangedEvent() set to Unknown");
                    break;
            }
        }
    };

    private final SipConversationHandler conversationHandler = new SipConversationHandler.SipConversationHandlerAdapter() {
        @Override
        public void onNewConversationEvent(@NonNull final SipConversation conversation, @NonNull final ConversationEvents
                .NewConversationEvent evt) {
            if (evt.getConversationType() == ConversationType_Incoming) {
                if (activeConversation != null) {
                    eventsCallBack
                            .log("onNewConversationEvent() reject second incoming call if we already have a call from before");
                    conversation.reject();
                    return;
                }

                activeConversation = conversation;
                String number = evt.getRemoteDisplayName();
                if (number.isEmpty()) number = evt.getRemoteAddress();
                eventsCallBack.log("Incoming call from : " + number);
                eventsCallBack.onIncomingCall(-1);
            } else {
                eventsCallBack.log("onNewConversationEvent() outgoing call proceeding, all is well");
            }
        }

        @Override
        public void onConversationStateChangedEvent(@NonNull final SipConversation conversation, @NonNull final
                ConversationEvents
                .ConversationStateChangedEvent evt) {
            switch (evt.getConversationState()) {
                case ConversationState_Connected:
                    eventsCallBack.log("onConversationStateChangedEvent() state set to Connected");
                    break;
                case ConversationState_LocalRinging:
                case ConversationState_RemoteRinging:
                    eventsCallBack.log("onConversationStateChangedEvent() state set to Connected Ringing ");
                    break;
                    default:
                        break;
            }
        }

        @Override
        public void onConversationEndedEvent(@NonNull final SipConversation conversation, @NonNull final
                ConversationEvents
                .ConversationEndedEvent
                evt) {
            if (Objects.equals(conversation, activeConversation)) {
                activeConversation = null;
                eventsCallBack.onCallEnded();
                eventsCallBack.log("onConversationEndedEvent() Conversation Ended");
            }
        }

        @Override
        public void onConversationMediaChangedEvent(@NonNull final SipConversation conversation, @NonNull final
                ConversationEvents
                .ConversationMediaChangedEvent msg) {

        }
    };

    private static class BriaVideoCodecs {

        private final int videoCodecId;
        private final String videoCodecName;

        public BriaVideoCodecs(final int videoCodecId, final String videoCodecName) {
            super();
            this.videoCodecId = videoCodecId;
            this.videoCodecName = videoCodecName;
        }

        public int getVideoCodecId() {
            return videoCodecId;
        }

        public String getVideoCodecName() {
            return videoCodecName;
        }
    }

}
