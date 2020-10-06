package org.jellyfin.androidtv.util;

import org.jellyfin.androidtv.constant.CodecTypes;
import org.jellyfin.androidtv.constant.ContainerTypes;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.apiclient.model.dlna.CodecProfile;
import org.jellyfin.apiclient.model.dlna.CodecType;
import org.jellyfin.apiclient.model.dlna.ContainerProfile;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile;
import org.jellyfin.apiclient.model.dlna.DlnaProfileType;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
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

    public static DeviceProfile getBaseProfile(boolean isLiveTv) {
        DeviceProfile profile = new DeviceProfile();

        profile.setName("Android");
        profile.setMaxStreamingBitrate(20000000);
        profile.setMaxStaticBitrate(100000000);

        List<TranscodingProfile> transcodingProfiles = new ArrayList<>();

        TranscodingProfile transProfile = new TranscodingProfile();
//        if (isLiveTv) {
//            transProfile.setContainer(ContainerTypes.TS);
//            transProfile.setVideoCodec(CodecTypes.H264);
//            transProfile.setAudioCodec(Utils.join(",", CodecTypes.AAC, CodecTypes.MP3));
//            transProfile.setType(DlnaProfileType.Video);
//            transProfile.setContext(EncodingContext.Streaming);
//            transProfile.setProtocol(MediaTypes.HLS);
//
//        } else {
            transProfile.setContainer(ContainerTypes.MKV);
            transProfile.setVideoCodec(CodecTypes.H264);
            transProfile.setAudioCodec(Utils.join(",", CodecTypes.AAC, CodecTypes.MP3));
            transProfile.setType(DlnaProfileType.Video);
            transProfile.setContext(EncodingContext.Streaming);
            transProfile.setCopyTimestamps(true);

//        }

        transcodingProfiles.add(transProfile);

        TranscodingProfile tempVar = new TranscodingProfile();
        tempVar.setContainer(CodecTypes.MP3);
        tempVar.setAudioCodec(CodecTypes.MP3);
        tempVar.setType(DlnaProfileType.Audio);
        tempVar.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar);

        profile.setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[transcodingProfiles.size()]));
        profile.setSubtitleProfiles(new SubtitleProfile[] {
                getSubtitleProfile("srt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("subrip", SubtitleDeliveryMethod.External),
                getSubtitleProfile("ass", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("pgs", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.External),
                getSubtitleProfile("vtt", SubtitleDeliveryMethod.External),
                getSubtitleProfile("sub", SubtitleDeliveryMethod.External),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.External)
        });

        return profile;

    }

    public static DeviceProfile getExternalProfile() {
        DeviceProfile profile = new DeviceProfile();

        profile.setName("Android-External");
        profile.setMaxStaticBitrate(100000000);

        DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
        List<String> containers = Arrays.asList(
            ContainerTypes.M4V,
            ContainerTypes._3GP,
            ContainerTypes.TS,
            ContainerTypes.MPEGTS,
            ContainerTypes.MOV,
            ContainerTypes.XVID,
            ContainerTypes.VOB,
            ContainerTypes.MKV,
            ContainerTypes.WMV,
            ContainerTypes.ASF,
            ContainerTypes.OGM,
            ContainerTypes.OGV,
            ContainerTypes.M2V,
            ContainerTypes.AVI,
            ContainerTypes.MPG,
            ContainerTypes.MPEG,
            ContainerTypes.MP4,
            ContainerTypes.WEBM,
            ContainerTypes.DVR_MS,
            ContainerTypes.WTV
        );
        videoDirectPlayProfile.setContainer(Utils.join(",", containers));
        videoDirectPlayProfile.setType(DlnaProfileType.Video);

        profile.setDirectPlayProfiles(new DirectPlayProfile[] {videoDirectPlayProfile});

        List<TranscodingProfile> transcodingProfiles = new ArrayList<>();

        TranscodingProfile mkvProfile = new TranscodingProfile();
        mkvProfile.setContainer(ContainerTypes.MKV);
        mkvProfile.setVideoCodec(CodecTypes.H264);
        mkvProfile.setAudioCodec(Utils.join(",", CodecTypes.AAC, CodecTypes.MP3, CodecTypes.AC3));
        mkvProfile.setType(DlnaProfileType.Video);
        mkvProfile.setContext(EncodingContext.Streaming);
        mkvProfile.setCopyTimestamps(true);
        transcodingProfiles.add(mkvProfile);

        TranscodingProfile tempVar = new TranscodingProfile();
        tempVar.setContainer(CodecTypes.MP3);
        tempVar.setAudioCodec(CodecTypes.MP3);
        tempVar.setType(DlnaProfileType.Audio);
        tempVar.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar);

        profile.setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[transcodingProfiles.size()]));
        profile.setSubtitleProfiles(new SubtitleProfile[] {
            getSubtitleProfile("srt", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("subrip", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("ass", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("ssa", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("pgs", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("pgssub", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("dvdsub", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("vtt", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("sub", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed),
            getSubtitleProfile("smi", SubtitleDeliveryMethod.Embed)
        });

        return profile;

    }

    public static void setVlcOptions(DeviceProfile profile, boolean isLiveTv) {
        profile.setName("Android-VLC");
        DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();

        List<String> videoContainers = Arrays.asList(
            ContainerTypes.M4V,
            ContainerTypes._3GP,
            ContainerTypes.TS,
            ContainerTypes.MPEGTS,
            ContainerTypes.MOV,
            ContainerTypes.XVID,
            ContainerTypes.VOB,
            ContainerTypes.MKV,
            ContainerTypes.WMV,
            ContainerTypes.ASF,
            ContainerTypes.OGM,
            ContainerTypes.OGV,
            ContainerTypes.M2V,
            ContainerTypes.AVI,
            ContainerTypes.MPG,
            ContainerTypes.MPEG,
            ContainerTypes.MP4,
            ContainerTypes.WEBM,
            ContainerTypes.WTV
        );
        videoDirectPlayProfile.setContainer(Utils.join(",", videoContainers));
        List<String> audioCodecs = new ArrayList<>(Arrays.asList(
            CodecTypes.AAC,
            CodecTypes.MP3,
            CodecTypes.MP2,
            CodecTypes.AC3,
            CodecTypes.WMA,
            CodecTypes.WMAV2,
            CodecTypes.DCA,
            CodecTypes.DTS,
            CodecTypes.PCM,
            CodecTypes.PCM_S16LE,
            CodecTypes.PCM_S24LE,
            CodecTypes.OPUS,
            CodecTypes.FLAC,
            CodecTypes.TRUEHD
        ));
        if (!Utils.downMixAudio() && isLiveTv) {
            audioCodecs.add(CodecTypes.AAC_LATM);
        }
        videoDirectPlayProfile.setAudioCodec(Utils.join(",", audioCodecs));
        videoDirectPlayProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        List<String> audioContainers = Arrays.asList(
            CodecTypes.FLAC,
            CodecTypes.AAC,
            CodecTypes.MP3,
            CodecTypes.MPA,
            CodecTypes.WAV,
            CodecTypes.WMA,
            CodecTypes.MP2,
            ContainerTypes.OGG,
            ContainerTypes.OGA,
            ContainerTypes.WEBMA,
            CodecTypes.APE
        );
        audioDirectPlayProfile.setContainer(Utils.join(",", audioContainers));
        audioDirectPlayProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile photoDirectPlayProfile = new DirectPlayProfile();
        photoDirectPlayProfile.setContainer("jpg,jpeg,png,gif");
        photoDirectPlayProfile.setType(DlnaProfileType.Photo);

        profile.setDirectPlayProfiles(new DirectPlayProfile[]{videoDirectPlayProfile, audioDirectPlayProfile, photoDirectPlayProfile});


        CodecProfile h264MainProfile = new CodecProfile();
        h264MainProfile.setType(CodecType.Video);
        h264MainProfile.setCodec(CodecTypes.H264);
        h264MainProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, DeviceUtils.isFireTvStick() ? "41" : "51"),
                        new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.RefFrames, "2"),
                });

        ContainerProfile videoContainerProfile = new ContainerProfile();
        videoContainerProfile.setType(DlnaProfileType.Video);
        videoContainerProfile.setContainer(ContainerTypes.AVI);
        videoContainerProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.VideoCodecTag, "xvid"),
                });

        CodecProfile videoAudioCodecProfile = new CodecProfile();
        videoAudioCodecProfile.setType(CodecType.VideoAudio);
        videoAudioCodecProfile.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "8")});

        profile.setCodecProfiles(new CodecProfile[]{getHevcProfile(), h264MainProfile, videoAudioCodecProfile});
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
                getSubtitleProfile("smi", SubtitleDeliveryMethod.Embed),
                getSubtitleProfile("idx", SubtitleDeliveryMethod.Embed)
        });
    }

    public static void setExoOptions(DeviceProfile profile, boolean isLiveTv, boolean allowDTS) {
        profile.setName("Android-Exo");

        List<DirectPlayProfile> directPlayProfiles = new ArrayList<>();
        if (!isLiveTv || get(UserPreferences.class).get(UserPreferences.Companion.getLiveTvDirectPlayEnabled())) {
            DirectPlayProfile videoDirectPlayProfile = new DirectPlayProfile();
            List<String> containers = new ArrayList<>();
            if (isLiveTv) {
                containers.add(ContainerTypes.TS);
                containers.add(ContainerTypes.MPEGTS);
            }
            containers.addAll(Arrays.asList(
                ContainerTypes.M4V,
                ContainerTypes.MOV,
                ContainerTypes.XVID,
                ContainerTypes.VOB,
                ContainerTypes.MKV,
                ContainerTypes.WMV,
                ContainerTypes.ASF,
                ContainerTypes.OGM,
                ContainerTypes.OGV,
                ContainerTypes.MP4,
                ContainerTypes.WEBM
            ));
            videoDirectPlayProfile.setContainer(Utils.join(",", containers));
            List<String> videoCodecs;
            if (DeviceUtils.isShield() || DeviceUtils.isNexus() || DeviceUtils.isBeyondTv()) {
                videoCodecs = Arrays.asList(
                    CodecTypes.H264,
                    CodecTypes.HEVC,
                    CodecTypes.VP8,
                    CodecTypes.VP9,
                    ContainerTypes.MPEG,
                    CodecTypes.MPEG2VIDEO
                );
            } else {
                videoCodecs = Arrays.asList(
                    CodecTypes.H264,
                    CodecTypes.VP8,
                    CodecTypes.VP9,
                    ContainerTypes.MPEG,
                    CodecTypes.MPEG2VIDEO
                );
            }
            videoDirectPlayProfile.setVideoCodec(Utils.join(",", videoCodecs));
            if (Utils.downMixAudio()) {
                //compatible audio mode - will need to transcode dts and ac3
                Timber.i("*** Excluding DTS and AC3 audio from direct play due to compatible audio setting");
                videoDirectPlayProfile.setAudioCodec(Utils.join(",", CodecTypes.AAC, CodecTypes.MP3, CodecTypes.MP2));
            } else {
                List<String> audioCodecs = new ArrayList<>(Arrays.asList(
                    CodecTypes.AAC,
                    CodecTypes.AC3,
                    CodecTypes.EAC3,
                    CodecTypes.AAC_LATM,
                    CodecTypes.MP3,
                    CodecTypes.MP2
                ));
                if (allowDTS) {
                    audioCodecs.add(CodecTypes.DCA);
                    audioCodecs.add(CodecTypes.DTS);
                }
                videoDirectPlayProfile.setAudioCodec(Utils.join(",", audioCodecs));
            }
            videoDirectPlayProfile.setType(DlnaProfileType.Video);
            directPlayProfiles.add(videoDirectPlayProfile);
        }

        DirectPlayProfile audioDirectPlayProfile = new DirectPlayProfile();
        List<String> audioContainers = Arrays.asList(
            CodecTypes.AAC,
            CodecTypes.MP3,
            CodecTypes.MPA,
            CodecTypes.WAV,
            CodecTypes.WMA,
            CodecTypes.MP2,
            ContainerTypes.OGG,
            ContainerTypes.OGA,
            ContainerTypes.WEBMA,
            CodecTypes.APE,
            CodecTypes.OPUS
        );
        audioDirectPlayProfile.setContainer(Utils.join(",", audioContainers));
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
        videoCodecProfile.setCodec(CodecTypes.H264);
        videoCodecProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "high|main|baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, DeviceUtils.isFireTvStick()? "41" : "51")
                });

        CodecProfile refFramesProfile = new CodecProfile();
        refFramesProfile.setType(CodecType.Video);
        refFramesProfile.setCodec(CodecTypes.H264);
        refFramesProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "12"),
                });
        refFramesProfile.setApplyConditions(new ProfileCondition[] {
                new ProfileCondition(ProfileConditionType.GreaterThanEqual, ProfileConditionValue.Width, "1200")
        });

        CodecProfile refFramesProfile2 = new CodecProfile();
        refFramesProfile2.setType(CodecType.Video);
        refFramesProfile2.setCodec(CodecTypes.H264);
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
                getSubtitleProfile("ass", SubtitleDeliveryMethod.Encode),
                getSubtitleProfile("ssa", SubtitleDeliveryMethod.Encode),
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

    public static void addAc3Streaming(DeviceProfile profile, boolean primary) {
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

    private static SubtitleProfile getSubtitleProfile(String format, SubtitleDeliveryMethod method) {
        SubtitleProfile subs = new SubtitleProfile();
        subs.setFormat(format);
        subs.setMethod(method);
        return subs;
    }
}
