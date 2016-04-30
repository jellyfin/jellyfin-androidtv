package tv.emby.embyatv.util;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.EncodingContext;
import mediabrowser.model.dlna.ProfileCondition;
import mediabrowser.model.dlna.ProfileConditionType;
import mediabrowser.model.dlna.ProfileConditionValue;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dlna.SubtitleProfile;
import mediabrowser.model.dlna.TranscodingProfile;
import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 2/29/2016.
 */
public class ProfileHelper {

    public static DeviceProfile getBaseProfile() {
        DeviceProfile profile = new DeviceProfile();

        profile.setName("Android");
        profile.setMaxStreamingBitrate(20000000);
        profile.setMaxStaticBitrate(30000000);

        List<TranscodingProfile> transcodingProfiles = new ArrayList<>();

        TranscodingProfile mkvProfile = new TranscodingProfile();
        mkvProfile.setContainer("mkv");
        mkvProfile.setVideoCodec("h264");
        mkvProfile.setAudioCodec(Utils.is60() && !"1".equals(TvApp.getApplication().getPrefs().getString("pref_audio_option","0")) ? "aac,mp3,dca" : "aac,mp3");
        mkvProfile.setType(DlnaProfileType.Video);
        mkvProfile.setContext(EncodingContext.Streaming);
        mkvProfile.setCopyTimestamps(true);
        transcodingProfiles.add(mkvProfile);

        TranscodingProfile tempVar = new TranscodingProfile();
        tempVar.setContainer("mp3");
        tempVar.setAudioCodec("mp3");
        tempVar.setType(DlnaProfileType.Audio);
        tempVar.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar);

        profile.setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[transcodingProfiles.size()]));

        return profile;

    }

    public static void setVlcOptions(DeviceProfile profile) {

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
        videoAudioCodecProfile.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "6")});

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

    public static void setExoOptions(DeviceProfile profile, boolean isLiveTv, boolean allowDTS) {

        List<DirectPlayProfile> directPlayProfiles = new ArrayList<>();
        if (!isLiveTv || TvApp.getApplication().directStreamLiveTv()) {
            DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
            videoDirectPlayProfile.setContainer((isLiveTv ? "ts,mpegts," : "") + "m4v,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,mp4,webm");
            videoDirectPlayProfile.setVideoCodec(Utils.isShield() ? "h264,hevc,vp8,vp9,mpeg4,mpeg2video" : "h264,vp8,vp9,mpeg4,mpeg2video");
            if ("1".equals(TvApp.getApplication().getPrefs().getString("pref_audio_option","0"))) {
                //compatible audio mode - will need to transcode dts and ac3
                TvApp.getApplication().getLogger().Info("*** Excluding DTS and AC3 audio from direct play due to compatible audio setting");
                videoDirectPlayProfile.setAudioCodec("aac,mp3,mp2");
            } else {
                videoDirectPlayProfile.setAudioCodec(allowDTS ? "aac,ac3,eac3,dca,mp3,mp2" : "aac,ac3,eac3,mp3,mp2");
            }
            videoDirectPlayProfile.setType(DlnaProfileType.Video);
            directPlayProfiles.add(videoDirectPlayProfile);
        }

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        audioDirectPlayProfile.setContainer("aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape,opus");
        audioDirectPlayProfile.setType(DlnaProfileType.Audio);
        directPlayProfiles.add(audioDirectPlayProfile);

        DirectPlayProfile photoDirectPlayProfile = new DirectPlayProfile();
        photoDirectPlayProfile.setContainer("jpg,jpeg,png,gif");
        photoDirectPlayProfile.setType(DlnaProfileType.Photo);
        directPlayProfiles.add(photoDirectPlayProfile);

        DirectPlayProfile[] profiles = new DirectPlayProfile[directPlayProfiles.size()];
        profile.setDirectPlayProfiles(directPlayProfiles.toArray(profiles));

        CodecProfile videoCodecProfile = new CodecProfile();
        videoCodecProfile.setType(CodecType.Video);
        videoCodecProfile.setCodec("h264");
        videoCodecProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, "51")
                });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "6")});

        profile.setCodecProfiles(new CodecProfile[] { videoCodecProfile, videoAudioCodecProfile });
        profile.setSubtitleProfiles(new SubtitleProfile[] {
                getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("srt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ass", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("sub", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed)
        });
    }

    public static void addAc3Streaming(DeviceProfile profile, boolean primary) {
        TranscodingProfile mkvProfile = getTranscodingProfile(profile, "mkv");
        if (mkvProfile != null && !("1".equals(TvApp.getApplication().getPrefs().getString("pref_audio_option", "0"))))
        {
            TvApp.getApplication().getLogger().Info("*** Adding AC3 as supported transcoded audio");
            mkvProfile.setAudioCodec(primary ? "ac3,".concat(mkvProfile.getAudioCodec()) : mkvProfile.getAudioCodec().concat(",ac3"));
        }
    }

    private static TranscodingProfile getTranscodingProfile(DeviceProfile deviceProfile, String container) {
        for (TranscodingProfile profile : deviceProfile.getTranscodingProfiles()) {
            if (container.equals(profile.getContainer())) return profile;
        }

        return null;
    }

    private static SubtitleProfile getSubtitleProfile(String format, SubtitleDeliveryMethod method) {
        SubtitleProfile subs = new SubtitleProfile();
        subs.setFormat(format);
        subs.setMethod(method);
        return subs;
    }
}
