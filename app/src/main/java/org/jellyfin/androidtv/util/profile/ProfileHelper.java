package org.jellyfin.androidtv.util.profile;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.constant.CodecTypes;
import org.jellyfin.androidtv.constant.ContainerTypes;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.CodecProfile;
import org.jellyfin.apiclient.model.dlna.CodecType;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile;
import org.jellyfin.apiclient.model.dlna.DlnaProfileType;
import org.jellyfin.apiclient.model.dlna.ProfileCondition;
import org.jellyfin.apiclient.model.dlna.ProfileConditionType;
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.dlna.TranscodingProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class ProfileHelper {
    private static MediaCodecCapabilitiesTest MediaTest = new MediaCodecCapabilitiesTest();

    protected static @NonNull CodecProfile getHevcProfile() {
        CodecProfile hevcProfile = new CodecProfile();
        hevcProfile.setType(CodecType.Video);
        hevcProfile.setCodec(CodecTypes.HEVC);
        if (!MediaTest.supportsHevc()) {
            //The following condition is a method to exclude all HEVC
            Timber.i("*** Does NOT support HEVC");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.Equals, ProfileConditionValue.VideoProfile, "none"),
                    });

        } else if (!MediaTest.supportsHevcMain10()) {
            Timber.i("*** Does NOT support HEVC 10 bit");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoProfile, "Main 10"),
                    });

        } else {
            // supports all HEVC
            Timber.i("*** Supports HEVC 10 bit");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoProfile, "none"),
                    });

        }

        return hevcProfile;
    }

    public static void addAc3Streaming(@NonNull DeviceProfile profile, boolean primary) {
        TranscodingProfile mkvProfile = getTranscodingProfile(profile, ContainerTypes.MKV);
        if (mkvProfile != null && !Utils.downMixAudio())
        {
            Timber.i("*** Adding AC3 as supported transcoded audio");
            mkvProfile.setAudioCodec(primary ? CodecTypes.AC3 + ",".concat(mkvProfile.getAudioCodec()) : mkvProfile.getAudioCodec().concat("," + CodecTypes.AC3));
        }
    }

    private static TranscodingProfile getTranscodingProfile(DeviceProfile deviceProfile, String container) {
        for (TranscodingProfile profile : deviceProfile.getTranscodingProfiles()) {
            if (container.equals(profile.getContainer())) return profile;
        }

        return null;
    }

    protected static @NonNull SubtitleProfile getSubtitleProfile(@NonNull String format, @NonNull SubtitleDeliveryMethod method) {
        SubtitleProfile subs = new SubtitleProfile();
        subs.setFormat(format);
        subs.setMethod(method);
        return subs;
    }
}
