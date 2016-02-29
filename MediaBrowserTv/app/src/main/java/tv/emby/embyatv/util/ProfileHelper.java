package tv.emby.embyatv.util;

import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.ProfileCondition;
import mediabrowser.model.dlna.ProfileConditionType;
import mediabrowser.model.dlna.ProfileConditionValue;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dlna.SubtitleProfile;

/**
 * Created by Eric on 2/29/2016.
 */
public class ProfileHelper {
    public static void setVlcOptions(AndroidProfile profile) {

        DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
        videoDirectPlayProfile.setContainer("m4v,3gp,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,m2v,avi,mpg,mpeg,mp4,webm");
        videoDirectPlayProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        audioDirectPlayProfile.setContainer("flac,aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape");
        audioDirectPlayProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile photoDirectPlayProfile = new DirectPlayProfile();
        photoDirectPlayProfile.setContainer("jpg,jpeg,png,gif");
        photoDirectPlayProfile.setType(DlnaProfileType.Photo);

        profile.setDirectPlayProfiles(new DirectPlayProfile[]{videoDirectPlayProfile, audioDirectPlayProfile, photoDirectPlayProfile});

        CodecProfile videoCodecProfile = new CodecProfile();
        videoCodecProfile.setType(CodecType.Video);
        videoCodecProfile.setCodec("h264");
        videoCodecProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, "41")
                });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "6")});

        profile.setCodecProfiles(new CodecProfile[]{videoCodecProfile, videoAudioCodecProfile});
        profile.setSubtitleProfiles(new SubtitleProfile[]{
                getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("srt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ass", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("pgs", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("sub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed)
        });
    }

    public static void setExoOptions(AndroidProfile profile) {

        DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
        videoDirectPlayProfile.setContainer("m4v,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,mp4,webm");
        videoDirectPlayProfile.setVideoCodec("h264,hevc,vp8,vp9");
        videoDirectPlayProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        audioDirectPlayProfile.setContainer("flac,aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape");
        audioDirectPlayProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile photoDirectPlayProfile = new DirectPlayProfile();
        photoDirectPlayProfile.setContainer("jpg,jpeg,png,gif");
        photoDirectPlayProfile.setType(DlnaProfileType.Photo);

        profile.setDirectPlayProfiles(new DirectPlayProfile[]{videoDirectPlayProfile, audioDirectPlayProfile, photoDirectPlayProfile});

        CodecProfile videoCodecProfile = new CodecProfile();
        videoCodecProfile.setType(CodecType.Video);
        videoCodecProfile.setCodec("h264");
        videoCodecProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, "42")
                });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "8")});

        profile.setCodecProfiles(new CodecProfile[] { videoCodecProfile, videoAudioCodecProfile });
        profile.setSubtitleProfiles(new SubtitleProfile[] {
                getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("srt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ass", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("pgs", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("sub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed)
        });
    }

    private static SubtitleProfile getSubtitleProfile(String format, SubtitleDeliveryMethod method) {
        SubtitleProfile subs = new SubtitleProfile();
        subs.setFormat(format);
        subs.setMethod(method);
        return subs;
    }
}
