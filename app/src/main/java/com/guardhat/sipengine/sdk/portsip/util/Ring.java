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

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.provider.Settings;
import android.util.Log;

/**
 * The default ring tone.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public final class Ring {

    private static final int TONE_RELATIVE_VOLUME = 70;
    private final ToneGenerator mRingBackPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,
                                                                    TONE_RELATIVE_VOLUME);
    private final ToneGenerator mDTMFPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_RELATIVE_VOLUME);
    private Ringtone mRingtonePlayer;
    private int ringRef = 0;

    private static final Ring ringInstance = new Ring();

    private Ring() {
        super();
    }

    public static Ring getInstance() {
        return ringInstance;
    }

    public void stop() {
        stopRingBackTone();
        stopRingTone();
        stopDTMF();
    }

    public void startDTMF(final int number) {
        synchronized (mDTMFPlayer) {
            mDTMFPlayer.startTone(number);
        }
    }

    private void stopDTMF() {
        synchronized (mDTMFPlayer) {
            mDTMFPlayer.stopTone();
        }
    }

    public void startRingTone(final Context mContext) {
        if ((mRingtonePlayer != null) && mRingtonePlayer.isPlaying()) {
            ringRef++;
            return;
        }

        if ((mRingtonePlayer == null) && (mContext != null)) {
            try {
                mRingtonePlayer = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
            } catch (final RuntimeException e) {
                Log.e("Ring", e.getMessage());
                return;
            }
        }

        if (mRingtonePlayer != null) {
            synchronized (mRingtonePlayer) {
                ringRef++;
                mRingtonePlayer.play();
            }
        }
    }

    public void stopRingTone() {
        if (mRingtonePlayer != null) {
            synchronized (mRingtonePlayer) {

                --ringRef;
                if (ringRef <= 0) {
                    mRingtonePlayer.stop();
                    mRingtonePlayer = null;
                }
            }
        }
    }

    public void startRingBackTone() {
        synchronized (mRingBackPlayer) {
            mRingBackPlayer.startTone(ToneGenerator.TONE_SUP_RINGTONE);
        }
    }

    public void stopRingBackTone() {
        synchronized (mRingBackPlayer) {
            mRingBackPlayer.stopTone();
        }
    }
}
