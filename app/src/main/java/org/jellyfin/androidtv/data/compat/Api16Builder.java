package org.jellyfin.androidtv.data.compat;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import org.jellyfin.androidtv.constant.CodecTypes;
import org.jellyfin.androidtv.constant.ContainerTypes;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.CodecProfile;
import org.jellyfin.apiclient.model.dlna.CodecType;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile;
import org.jellyfin.apiclient.model.dlna.DlnaProfileType;
import org.jellyfin.apiclient.model.dlna.ProfileCondition;
import org.jellyfin.apiclient.model.dlna.ProfileConditionType;
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

@Deprecated
public class Api16Builder {
    protected AndroidProfileOptions Defaults;

    public Api16Builder(AndroidProfileOptions defaults) {
        Defaults = defaults;
    }

    public void buildProfiles(DeviceProfile profile){
        ArrayList<DirectPlayProfile> directPlayProfiles = new ArrayList<>();
        ArrayList<CodecProfile> codecProfiles = new ArrayList<>();

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                continue;
            }

            ProcessMediaCodecInfo(codecInfo, directPlayProfiles, codecProfiles);
        }

        profile.setDirectPlayProfiles(directPlayProfiles.toArray(new DirectPlayProfile[directPlayProfiles.size()]));
        profile.setCodecProfiles(codecProfiles.toArray(new CodecProfile[codecProfiles.size()]));
    }

    protected void ProcessMediaCodecInfo(MediaCodecInfo codecInfo, ArrayList<DirectPlayProfile> directPlayProfiles, ArrayList<CodecProfile> codecProfiles){
        for (String type : codecInfo.getSupportedTypes()){
            try {
                final MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(type);

                ProcessMediaCodecInfoType(codecInfo, type, codecCapabilities, directPlayProfiles, codecProfiles);
            } catch (IllegalArgumentException ex) {
                Timber.e("IllegalArgumentException thrown by codecInfo.getCapabilitiesForType(type). type: %s", type);
            }
        }
    }

    protected void ProcessMediaCodecInfoType(MediaCodecInfo codecInfo, String type, MediaCodecInfo.CodecCapabilities codecCapabilities, ArrayList<DirectPlayProfile> directPlayProfiles, ArrayList<CodecProfile> codecProfiles){
        addDirectPlayProfile(directPlayProfiles, type);

        ArrayList<CodecProfile> newCodecProfiles = getCodecProfiles(type, codecCapabilities);

        for (CodecProfile cp : newCodecProfiles){
            if (!containsCodecProfile(codecProfiles, cp)){
                codecProfiles.add(cp);
                processCodecProfile(codecInfo, type, codecCapabilities, cp);
            }
        }
    }

    protected void processCodecProfile(MediaCodecInfo codecInfo, String type, MediaCodecInfo.CodecCapabilities codecCapabilities, CodecProfile profile){

        AddProfileLevels(codecCapabilities, profile);
    }

    protected void AddProfileLevels(MediaCodecInfo.CodecCapabilities codecCapabilities, CodecProfile profile){
        ArrayList<ProfileCondition> conditions = new ArrayList<>();

        String[] videoProfiles = GetVideoProfiles(codecCapabilities);
        int maxLevel = getMaxLevel(codecCapabilities);

        if (CodecTypes.H264.equalsIgnoreCase(profile.getCodec())){
            if (videoProfiles.length > 0){
                conditions.add(new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, Utils.join("|", videoProfiles)));
            }
            else{
                conditions.add(new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, Defaults.DefaultH264Profile));
            }

            if (maxLevel <= 0){
                maxLevel = Defaults.DefaultH264Level;
            }
            conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoLevel, String.valueOf(maxLevel)));

            // TODO: This needs to vary per resolution
            //conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.RefFrames, "4"));
        }

        conditions.addAll(Arrays.asList(profile.getConditions()));

        profile.setConditions(conditions.toArray(new ProfileCondition[conditions.size()]));
    }

    private String[] GetVideoProfiles(MediaCodecInfo.CodecCapabilities codecCapabilities) {
        ArrayList<String> profiles = new ArrayList<>();

        MediaCodecInfo.CodecProfileLevel[] levels = codecCapabilities.profileLevels;

        for (MediaCodecInfo.CodecProfileLevel level : levels){

            for (String value : getProfiles(level)){
                // Insert at the beginning to put the most complex profile at the front
                profiles.add(0, value);
            }
        }

        return profiles.toArray(new String[profiles.size()]);
    }

    private int getMaxLevel(MediaCodecInfo.CodecCapabilities codecCapabilities) {
        MediaCodecInfo.CodecProfileLevel[] levels = codecCapabilities.profileLevels;
        int max = 0;

        for (MediaCodecInfo.CodecProfileLevel level : levels){
            int value = getLevel(level);

            max = Math.max(max, value);
        }

        return max;
    }

    private String[] getProfiles(MediaCodecInfo.CodecProfileLevel level){
        switch (level.level) {
            case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline:
                return new String[]{"constrained baseline", "baseline"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileExtended:
                return new String[]{"extended"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh:
                return new String[]{"high"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10:
                // TODO: Verify. Maybe high 10?
                return new String[]{"high"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422:
                // TODO: Verify
                return new String[]{"high"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444:
                // TODO: Verify
                return new String[]{"high"};
            case MediaCodecInfo.CodecProfileLevel.AVCProfileMain:
                return new String[]{"main"};
            default:
                return new String[]{};
        }
    }

    private int getLevel(MediaCodecInfo.CodecProfileLevel level){
        switch (level.level) {
            case MediaCodecInfo.CodecProfileLevel.AVCLevel1:
                return 1;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel11:
                return 11;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel12:
                return 12;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel13:
                return 13;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel1b:
                // TODO: Verify this
                return 10;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel2:
                return 20;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel21:
                return 21;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel22:
                return 22;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel3:
                return 30;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel31:
                return 31;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel32:
                return 32;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel4:
                return 40;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel41:
                return 41;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel42:
                return 42;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel5:
                return 50;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel51:
                return 51;
            default:
                return 0;
        }
    }

    protected void addDirectPlayProfile(List<DirectPlayProfile> profiles, String type){
        String[] parts = type.split("/");
        if (parts.length != 2) return;

        DirectPlayProfile profile = new DirectPlayProfile();

        if ("audio".equalsIgnoreCase(parts[0])) {
            profile.setType(DlnaProfileType.Audio);
        }
        else if ("video".equalsIgnoreCase(parts[0])) {
            profile.setType(DlnaProfileType.Video);
        }
        else if ("image".equalsIgnoreCase(parts[0])) {
            profile.setType(DlnaProfileType.Photo);
        }
        else{
            return;
        }

        String codecType = parts[1].toLowerCase();

        // Since we can't get supported codecs per container, we'll have to hardcode them
        if (profile.getType()==DlnaProfileType.Video){
            profile.setContainer(codecType);

            if (codecType.startsWith(ContainerTypes.MP4)){
                profile.setContainer(Utils.join(",", ContainerTypes.MP4, ContainerTypes.M4V));
                profile.setVideoCodec(Utils.join(",", CodecTypes.H264, CodecTypes.MPEG4));
                profile.setAudioCodec(CodecTypes.AAC);

                if (Defaults.SupportsMkv){
                    profile.setContainer(Utils.join(",", profile.getContainer(), ContainerTypes.MKV));
                }
            }
            else if (CodecTypes.AVC.equalsIgnoreCase(codecType)){
                profile.setContainer(Utils.join(",", ContainerTypes.MP4, ContainerTypes.M4V));
                profile.setVideoCodec(Utils.join(",", CodecTypes.H264, CodecTypes.MPEG4));
                profile.setAudioCodec(CodecTypes.AAC);

                if (Defaults.SupportsMkv){
                    profile.setContainer(Utils.join(",", profile.getContainer(), ContainerTypes.MKV));
                }
            }
            else if (CodecTypes.HEVC.equalsIgnoreCase(codecType)){
                profile.setContainer(Utils.join(",", ContainerTypes.MP4, ContainerTypes.M4V));
                profile.setVideoCodec(CodecTypes.H265);
                profile.setAudioCodec(CodecTypes.AAC);

                if (Defaults.SupportsMkv){
                    profile.setContainer(Utils.join(",", profile.getContainer(), ContainerTypes.MKV));
                }
            }
            else if (codecType.contains(CodecTypes.VP8)){
                profile.setContainer(ContainerTypes.WEBM);
            }
            else if (codecType.contains(CodecTypes.VP9)){
                profile.setContainer(ContainerTypes.WEBM);
            }
            else if (ContainerTypes._3GPP.equalsIgnoreCase(codecType)){
                profile.setContainer(ContainerTypes._3GP);
            }
            else if (CodecTypes.MPEG2.equalsIgnoreCase(codecType)){
                profile.setContainer(Utils.join(",", ContainerTypes.TS, ContainerTypes.MPEGTS));
                profile.setVideoCodec(Utils.join(",", CodecTypes.MPEG2VIDEO, CodecTypes.MPEG2));
                profile.setAudioCodec(CodecTypes.AAC);

                // If we need to add mp4, m4v or mkv then they must be kept separate from mpegts and in their own DirectPlayProfile
                // This is because they have different rules regarding ac3 within the container
            }
            else {
                profile.setContainer(codecType);
            }
        }
        else if (profile.getType()==DlnaProfileType.Audio){
            if (codecType.startsWith(ContainerTypes.MP4)){
                profile.setContainer(CodecTypes.AAC);
            }
            else if (ContainerTypes.MPEG.equalsIgnoreCase(codecType)){
                profile.setContainer(CodecTypes.MP3);
            }
            else if (ContainerTypes._3GPP.equalsIgnoreCase(codecType)){
                profile.setContainer(ContainerTypes._3GP);
            }
            else if (CodecTypes.VORBIS.equalsIgnoreCase(codecType)){
                profile.setContainer(Utils.join(",", ContainerTypes.WEBM, ContainerTypes.WEBMA));
            }
            else if (CodecTypes.OPUS.equalsIgnoreCase(codecType)){
                profile.setContainer(Utils.join(",", ContainerTypes.OGA, ContainerTypes.OGG));
            }
            else {
                // Will cover flac, gsm, and others
                profile.setContainer(codecType);
            }
        }

        if (!containsDirectPlayProfile(profiles, profile)){
            profiles.add(profile);
        }
    }

    protected ArrayList<CodecProfile> getCodecProfiles(String type, MediaCodecInfo.CodecCapabilities codecCapabilities){
        ArrayList<CodecProfile> newlyAddedCodecProfiles = new ArrayList<>();

        String[] parts = type.split("/");
        if (parts.length != 2) return newlyAddedCodecProfiles;

        CodecProfile profile = new CodecProfile();
        ArrayList<ProfileCondition> conditions = new ArrayList<ProfileCondition>();

        if ("audio".equalsIgnoreCase(parts[0])) {
            profile.setType(CodecType.Audio);
        }
        else if ("video".equalsIgnoreCase(parts[0])) {
            profile.setType(CodecType.Video);
        }
        else{
            return newlyAddedCodecProfiles;
        }

        String codecType = parts[1].toLowerCase();

        if (profile.getType()==CodecType.Video) {
            conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"));
            conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"));
            conditions.add(new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true"));

            if (codecType.contains(CodecTypes.AVC)) {
                profile.setCodec(CodecTypes.H264);

                conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"));
            }
            else if (codecType.contains(CodecTypes.HEVC)) {
                profile.setCodec(CodecTypes.H265);
            }
            else if (codecType.contains(CodecTypes.VP8)) {
                profile.setCodec(CodecTypes.VP8);
            }
            else if (codecType.contains(CodecTypes.VP9)) {
                profile.setCodec(CodecTypes.VP9);
            }
            else{
                profile.setCodec(codecType);
            }
        }
        else if (profile.getType()==CodecType.Audio){
            if (codecType.startsWith(ContainerTypes.MP4)) {
                profile.setCodec(CodecTypes.AAC);
            }
            else if (ContainerTypes.MPEG.equalsIgnoreCase(codecType)) {
                profile.setCodec(CodecTypes.MP3);

                conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioBitrate, "320000"));
            }
            else if (CodecTypes.OPUS.equalsIgnoreCase(codecType)) {
                profile.setCodec(CodecTypes.VORBIS);
            }
            else{
                profile.setCodec(codecType);
            }

            conditions.add(new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2"));
        }

        profile.setConditions(conditions.toArray(new ProfileCondition[conditions.size()]));

        newlyAddedCodecProfiles.add(profile);

        if (profile.getType()==CodecType.Audio && CodecTypes.AAC.equalsIgnoreCase(profile.getCodec())) {
            // Create a duplicate under VideoAudioType
            CodecProfile videoAudioProfile = new CodecProfile();
            videoAudioProfile.setType(CodecType.VideoAudio);
            videoAudioProfile.setCodec(CodecTypes.AAC);

            ArrayList<ProfileCondition> videoAudioProfileConditions = new ArrayList<>(
                    Arrays.asList(profile.getConditions())
            );

            videoAudioProfile.setConditions(videoAudioProfileConditions.toArray(new ProfileCondition[videoAudioProfileConditions.size()]));

            newlyAddedCodecProfiles.add(videoAudioProfile);
        }

        return newlyAddedCodecProfiles;
    }

    private boolean containsDirectPlayProfile(List<DirectPlayProfile> profiles, DirectPlayProfile newProfile){
        for (DirectPlayProfile profile : profiles){
            if (profile.getType() == newProfile.getType()) {
                if (Utils.equalsIgnoreCase(profile.getContainer(), newProfile.getContainer())) {
                    if (profile.getType() == DlnaProfileType.Audio){
                        if (Utils.equalsIgnoreCase(profile.getAudioCodec(), newProfile.getAudioCodec())) {
                            return true;
                        }
                    }
                    else if (profile.getType() == DlnaProfileType.Video){
                        if (Utils.equalsIgnoreCase(profile.getVideoCodec(), newProfile.getVideoCodec())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean containsCodecProfile(List<CodecProfile> profiles, CodecProfile newProfile){
        for (CodecProfile profile : profiles){
            if (profile.getType() == newProfile.getType()) {
                if (Utils.equalsIgnoreCase(profile.getCodec(), newProfile.getCodec())) {
                    return true;
                }
            }
        }
        return false;
    }
}
