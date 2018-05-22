/*
 *   Copyright (C) 2018 GuardHat Inc. All rights reserved.
 *
 *   The copyright to the computer software herein is the property of
 *   GuardHat Inc. This software may not be used and/or copied without explicit
 *   written permission of GuardHat Inc., and only in accordance with the terms
 *   and conditions stipulated in the license agreement and/or contract under
 *   which the software has been supplied.
 */

package com.guardhat.sipengine.sdk;

import android.support.annotation.NonNull;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

/**
 * Value class for SIP SDK configuration.
 *
 * @author Guardhat Inc.
 * @since 2018
 */
public final class StackConfiguration {

    private final ImmutableList<Integer> audioCodec;
    private final ImmutableList<Integer> videoCodec;
    private final int bitrate;
    private final int frameHeight;
    private final int frameWidth;
    private final int quality;

    private StackConfiguration(@NonNull final ImmutableList<Integer> audioCodec,
                               @NonNull final ImmutableList<Integer> videoCodec,
                               final int bitrate,
                               final int frameHeight,
                               final int frameWidth,
                               final int quality) {
        super();
        this.audioCodec = audioCodec;
        this.videoCodec = videoCodec;
        this.bitrate = bitrate;
        this.frameHeight = frameHeight;
        this.frameWidth = frameWidth;
        this.quality = quality;
    }

    @NonNull
    public List<Integer> getAudioCodec() {
        return Collections.unmodifiableList(audioCodec);
    }

    @NonNull
    public List<Integer> getVideoCodec() {
        return Collections.unmodifiableList(videoCodec);
    }

    public int getBitrate() {
        return bitrate;
    }

    static StackConfiguration getConfiguration(@NonNull final ImmutableList<Integer> audioCodec,
                                               @NonNull final ImmutableList<Integer> videoCodec,
                                               final int bitrate,
                                               final int height,
                                               final int width,
                                               final int quality) {

        return new StackConfiguration(audioCodec, videoCodec, bitrate, height, width, quality);
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getQuality() {
        return quality;
    }
}
