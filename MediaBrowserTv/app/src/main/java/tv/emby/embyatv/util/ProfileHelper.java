package tv.emby.embyatv.util;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.ContainerProfile;
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
    private static MediaCodecCapabilitiesTest MediaTest = new MediaCodecCapabilitiesTest();

    public static DeviceProfile getBaseProfile() {
        DeviceProfile profile = new DeviceProfile();

        profile.setName("Android");
        profile.setMaxStreamingBitrate(20000000);
        profile.setMaxStaticBitrate(30000000);

        List<TranscodingProfile> transcodingProfiles = new ArrayList<>();

        TranscodingProfile mkvProfile = new TranscodingProfile();
        mkvProfile.setContainer("mkv");
        mkvProfile.setVideoCodec("h264");
        mkvProfile.setAudioCodec("aac,mp3");
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
        profile.setSubtitleProfiles(new SubtitleProfile[] {
                getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("subrip", SubtitleDeliveryMethod.External),
                getSubtitleProfile("ass", SubtitleDeliveryMethod.External),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.External),
                getSubtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.External),
                getSubtitleProfile("vtt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("sub", SubtitleDeliveryMethod.External),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.External)
        });

        return profile;

    }

    public static void setVlcOptions(DeviceProfile profile, boolean isLiveTv) {

        DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
        videoDirectPlayProfile.setContainer("m4v,3gp,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,m2v,avi,mpg,mpeg,mp4,webm");
        videoDirectPlayProfile.setAudioCodec("aac,mp3,mp2,ac3,wma,wmav2,dca,pcm,PCM_S16LE,PCM_S24LE,opus,flac" + (Utils.downMixAudio() || !isLiveTv ? "" : ",aac_latm"));
        videoDirectPlayProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        audioDirectPlayProfile.setContainer("flac,aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape");
        audioDirectPlayProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile photoDirectPlayProfile = new DirectPlayProfile();
        photoDirectPlayProfile.setContainer("jpg,jpeg,png,gif");
        photoDirectPlayProfile.setType(DlnaProfileType.Photo);

        profile.setDirectPlayProfiles(new DirectPlayProfile[]{videoDirectPlayProfile, audioDirectPlayProfile, photoDirectPlayProfile});

        CodecProfile h264MainProfile = new CodecProfile();
        h264MainProfile.setType(CodecType.Video);
        h264MainProfile.setCodec("h264");
        h264MainProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, "41"),
                        new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.RefFrames, "2"),
                });

        CodecProfile refFramesProfile = new CodecProfile();
        refFramesProfile.setType(CodecType.Video);
        refFramesProfile.setCodec("h264");
        refFramesProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "12"),
                });
        refFramesProfile.setApplyConditions(new ProfileCondition[] {
                new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.Width, "1200")
        });

        CodecProfile refFramesProfile2 = new CodecProfile();
        refFramesProfile2.setType(CodecType.Video);
        refFramesProfile2.setCodec("h264");
        refFramesProfile2.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "4"),
                });
        refFramesProfile2.setApplyConditions(new ProfileCondition[] {
                new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.Width, "1900")
        });

        ContainerProfile videoContainerProfile = new ContainerProfile();
        videoContainerProfile.setType(DlnaProfileType.Video);
        videoContainerProfile.setContainer("avi");
        videoContainerProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoCodecTag, "xvid"),
                });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "6")});

        profile.setCodecProfiles(new CodecProfile[]{getHevcProfile(), h264MainProfile, refFramesProfile, refFramesProfile2, videoAudioCodecProfile});
        profile.setContainerProfiles(new ContainerProfile[] {videoContainerProfile});
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
            videoDirectPlayProfile.setVideoCodec(Utils.isShield() || Utils.isNexus() ? "h264,hevc,vp8,vp9,mpeg4,mpeg2video" : "h264,vp8,vp9,mpeg4,mpeg2video");
            if (Utils.downMixAudio()) {
                //compatible audio mode - will need to transcode dts and ac3
                TvApp.getApplication().getLogger().Info("*** Excluding DTS and AC3 audio from direct play due to compatible audio setting");
                videoDirectPlayProfile.setAudioCodec("aac,mp3,mp2");
            } else {
                videoDirectPlayProfile.setAudioCodec("aac,ac3,eac3,aac_latm,mp3,mp2" + (allowDTS ? ",dca" : ""));
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

        CodecProfile refFramesProfile = new CodecProfile();
        refFramesProfile.setType(CodecType.Video);
        refFramesProfile.setCodec("h264");
        refFramesProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "12"),
                });
        refFramesProfile.setApplyConditions(new ProfileCondition[] {
                new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.Width, "1200")
        });

        CodecProfile refFramesProfile2 = new CodecProfile();
        refFramesProfile2.setType(CodecType.Video);
        refFramesProfile2.setCodec("h264");
        refFramesProfile2.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "4"),
                });
        refFramesProfile2.setApplyConditions(new ProfileCondition[] {
                new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.Width, "1900")
        });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "6")});

        profile.setCodecProfiles(new CodecProfile[] { videoCodecProfile, refFramesProfile, refFramesProfile2, getHevcProfile(), videoAudioCodecProfile });
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

    private static CodecProfile getHevcProfile() {
        CodecProfile hevcProfile = new CodecProfile();
        hevcProfile.setType(CodecType.Video);
        hevcProfile.setCodec("hevc");
        if (!MediaTest.supportsHevc()) {
            //The following condition is a method to exclude all HEVC
            TvApp.getApplication().getLogger().Info("*** Does NOT support HEVC");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.Equals, ProfileConditionValue.VideoProfile, "none"),
                    });

        } else if (!MediaTest.supportsHevcMain10()) {
            TvApp.getApplication().getLogger().Info("*** Does NOT support HEVC 10 bit");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoProfile, "Main 10"),
                    });

        } else {
            // supports all HEVC
            TvApp.getApplication().getLogger().Info("*** Supports HEVC 10 bit");
            hevcProfile.setConditions(new ProfileCondition[]
                    {
                            new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoProfile, "none"),
                    });

        }

        return hevcProfile;
    }

    public static void addAc3Streaming(DeviceProfile profile, boolean primary) {
        TranscodingProfile mkvProfile = getTranscodingProfile(profile, "mkv");
        if (mkvProfile != null && !Utils.downMixAudio())
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
