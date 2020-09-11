package org.jellyfin.androidtv.data.compat;

import java.util.ArrayList;
import java.util.Arrays;

import org.jellyfin.androidtv.constants.CodecTypes;
import org.jellyfin.androidtv.constants.ContainerTypes;
import org.jellyfin.androidtv.constants.MediaTypes;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.CodecProfile;
import org.jellyfin.apiclient.model.dlna.CodecType;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile;
import org.jellyfin.apiclient.model.dlna.DlnaProfileType;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dlna.ProfileCondition;
import org.jellyfin.apiclient.model.dlna.ProfileConditionType;
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue;
import org.jellyfin.apiclient.model.dlna.ResponseProfile;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.dlna.TranscodingProfile;

@Deprecated
public class AndroidProfile extends DeviceProfile {
    public AndroidProfile() {
        this(new AndroidProfileOptions());
    }

    public AndroidProfile(String deviceName) {
        this(new AndroidProfileOptions(deviceName));
    }

    public AndroidProfile(AndroidProfileOptions profileOptions) {
        setName("Android");

        setMaxStaticBitrate(30000000);
        setMaxStreamingBitrate(20000000);

        // Adds a lot of weight and not needed in this context
        setProtocolInfo(null);

        ArrayList<TranscodingProfile> transcodingProfiles = new ArrayList<>();

        TranscodingProfile tempVar = new TranscodingProfile();
        tempVar.setContainer(CodecTypes.MP3);
        tempVar.setAudioCodec(CodecTypes.MP3);
        tempVar.setType(DlnaProfileType.Audio);
        tempVar.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar);

        TranscodingProfile tempVar0 = new TranscodingProfile();
        tempVar0.setContainer(CodecTypes.MP3);
        tempVar0.setAudioCodec(CodecTypes.MP3);
        tempVar0.setType(DlnaProfileType.Audio);
        tempVar0.setContext(EncodingContext.Static);
        transcodingProfiles.add(tempVar0);

        if (profileOptions.SupportsHls) {
            TranscodingProfile tempVar2 = new TranscodingProfile();
            tempVar2.setProtocol(MediaTypes.HLS);
            tempVar2.setContainer(ContainerTypes.TS);
            tempVar2.setVideoCodec(CodecTypes.H264);
            tempVar2.setAudioCodec(CodecTypes.AAC);
            tempVar2.setType(DlnaProfileType.Video);
            tempVar2.setContext(EncodingContext.Streaming);
            transcodingProfiles.add(tempVar2);
        }
        TranscodingProfile mkvProfile = new TranscodingProfile();
        mkvProfile.setContainer(ContainerTypes.MKV);
        mkvProfile.setVideoCodec(CodecTypes.H264);
        mkvProfile.setAudioCodec(Utils.join(",", CodecTypes.AAC, CodecTypes.MP3));
        mkvProfile.setType(DlnaProfileType.Video);
        mkvProfile.setContext(EncodingContext.Streaming);
        mkvProfile.setCopyTimestamps(true);
        transcodingProfiles.add(mkvProfile);

        TranscodingProfile tempVar3 = new TranscodingProfile();
        tempVar3.setContainer(ContainerTypes.MP4);
        tempVar3.setVideoCodec(CodecTypes.H264);
        tempVar3.setAudioCodec(CodecTypes.AAC);
        tempVar3.setType(DlnaProfileType.Video);
        tempVar3.setContext(EncodingContext.Static);
        transcodingProfiles.add(tempVar3);

        TranscodingProfile webmProfile = new TranscodingProfile();
        webmProfile.setContainer(ContainerTypes.WEBM);
        webmProfile.setVideoCodec(CodecTypes.VPX);
        webmProfile.setAudioCodec(CodecTypes.VORBIS);
        webmProfile.setType(DlnaProfileType.Video);
        webmProfile.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(webmProfile);

        setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[0]));

        DirectPlayProfile tempVar4 = new DirectPlayProfile();
        tempVar4.setContainer(ContainerTypes.MP4);
        tempVar4.setVideoCodec(Utils.join(",", CodecTypes.H264, CodecTypes.MPEG4));
        tempVar4.setAudioCodec(CodecTypes.AAC);
        tempVar4.setType(DlnaProfileType.Video);

        DirectPlayProfile tempVar5 = new DirectPlayProfile();
        tempVar5.setContainer(Utils.join(",", ContainerTypes.MP4, CodecTypes.AAC));
        tempVar5.setAudioCodec(CodecTypes.AAC);
        tempVar5.setType(DlnaProfileType.Audio);

        DirectPlayProfile tempVar6 = new DirectPlayProfile();
        tempVar6.setContainer(CodecTypes.MP3);
        tempVar6.setAudioCodec(CodecTypes.MP3);
        tempVar6.setType(DlnaProfileType.Audio);

        DirectPlayProfile tempVar7 = new DirectPlayProfile();
        tempVar7.setContainer(CodecTypes.FLAC);
        tempVar7.setAudioCodec(CodecTypes.FLAC);
        tempVar7.setType(DlnaProfileType.Audio);

        DirectPlayProfile tempVar8 = new DirectPlayProfile();
        tempVar8.setContainer(ContainerTypes.OGG);
        tempVar8.setAudioCodec(CodecTypes.VORBIS);
        tempVar8.setType(DlnaProfileType.Audio);

        DirectPlayProfile tempVar9 = new DirectPlayProfile();
        tempVar9.setContainer("jpeg,png,gif,bmp");
        tempVar9.setType(DlnaProfileType.Photo);
        setDirectPlayProfiles(new DirectPlayProfile[]{tempVar4, tempVar5, tempVar6, tempVar7, tempVar8, tempVar9});

        CodecProfile tempVar10 = new CodecProfile();
        tempVar10.setType(CodecType.Video);
        tempVar10.setCodec(CodecTypes.H264);
        tempVar10.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, profileOptions.DefaultH264Profile),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, String.valueOf(profileOptions.DefaultH264Level)),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"),

                        // TODO: This needs to vary per resolution
                        //new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "4"),
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true")
                });

        CodecProfile tempVar11 = new CodecProfile();
        tempVar11.setType(CodecType.Video);
        tempVar11.setCodec(Utils.join(",", CodecTypes.VPX, CodecTypes.MPEG4));
        tempVar11.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"),
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true")
                });

        CodecProfile tempVar12 = new CodecProfile();
        tempVar12.setType(CodecType.VideoAudio);
        tempVar12.setCodec(CodecTypes.AAC);
        tempVar12.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        CodecProfile tempVar13 = new CodecProfile();
        tempVar13.setType(CodecType.Audio);
        tempVar13.setCodec(CodecTypes.AAC);
        tempVar13.setConditions(new ProfileCondition[]{new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        CodecProfile tempVar14 = new CodecProfile();
        tempVar14.setType(CodecType.Audio);
        tempVar14.setCodec(CodecTypes.MP3);
        tempVar14.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioBitrate, "320000")
                });
        setCodecProfiles(new CodecProfile[]{tempVar10, tempVar11, tempVar12, tempVar13, tempVar14});

        buildDynamicProfiles(profileOptions);

        addM4v();

        if (profileOptions.SupportsAc3) {
            addAc3();
        }

        if (profileOptions.SupportsDts || profileOptions.SupportsDtsHdMa || profileOptions.SupportsTrueHd) {
            addDca();
        }

        buildSubtitleProfiles();
    }

    private void addAc3() {
        for (DirectPlayProfile profile : getDirectPlayProfiles()) {
            if (profile.getType() == DlnaProfileType.Video) {
                String container = profile.getContainer();
                if (container != null && (container.toLowerCase().contains(ContainerTypes.MP4) ||
                        container.toLowerCase().contains(ContainerTypes.MKV) ||
                        container.toLowerCase().contains(ContainerTypes.M4V))) {

                    String audioCodec = profile.getAudioCodec();
                    if (Utils.isEmpty(audioCodec)) {
                        profile.setAudioCodec(CodecTypes.AC3);
                    } else {
                        profile.setAudioCodec(audioCodec + "," + CodecTypes.AC3);
                    }
                }
            }
        }
    }

    private void addDca() {
        for (DirectPlayProfile profile : getDirectPlayProfiles()) {
            if (profile.getType() == DlnaProfileType.Video) {
                String container = profile.getContainer();
                if (container != null && (container.toLowerCase().contains(ContainerTypes.MP4) ||
                        container.toLowerCase().contains(ContainerTypes.MKV) ||
                        container.toLowerCase().contains(ContainerTypes.M4V) ||
                        container.toLowerCase().contains(ContainerTypes.TS))) {

                    String audioCodec = profile.getAudioCodec();
                    if (Utils.isEmpty(audioCodec)) {
                        profile.setAudioCodec(CodecTypes.DCA);
                    } else {
                        profile.setAudioCodec(audioCodec + "," + CodecTypes.DCA);
                    }
                }
            }
        }
    }

    private void addM4v() {
        for (DirectPlayProfile profile : getDirectPlayProfiles()) {
            if (profile.getType() == DlnaProfileType.Video) {
                String container = profile.getContainer();
                if (container != null && container.toLowerCase().contains(ContainerTypes.MP4)) {
                    profile.setContainer(container + "," + ContainerTypes.M4V);
                }
            }
        }

        ArrayList<ResponseProfile> responseProfiles = new ArrayList<>(
                Arrays.asList(getResponseProfiles())
        );

        ResponseProfile m4vProfile = new ResponseProfile();
        m4vProfile.setContainer(ContainerTypes.M4V);
        m4vProfile.setType(DlnaProfileType.Video);
        m4vProfile.setMimeType("video/mp4");
        responseProfiles.add(m4vProfile);

        setResponseProfiles(responseProfiles.toArray(new ResponseProfile[] {}));
    }

    private void buildDynamicProfiles(AndroidProfileOptions options) {
         new Api21Builder(options).buildProfiles(this);
    }

    private void buildSubtitleProfiles() {
        SubtitleProfile srtSubs = new SubtitleProfile();
        srtSubs.setFormat("srt");
        srtSubs.setMethod(SubtitleDeliveryMethod.External);

        setSubtitleProfiles(new SubtitleProfile[]{srtSubs});
    }
}
