package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.session.PlayMethod;

public class StreamBuilder
{
    public static SubtitleProfile getSubtitleProfile(org.jellyfin.sdk.model.api.MediaStream subtitleStream, SubtitleProfile[] subtitleProfiles, PlayMethod playMethod)
    {
        if (playMethod != PlayMethod.Transcode && !subtitleStream.isExternal())
        {
            // Look for supported embedded subs
            for (SubtitleProfile profile : subtitleProfiles)
            {
                if (!profile.SupportsLanguage(subtitleStream.getLanguage()))
                {
                    continue;
                }

                if (profile.getMethod() != SubtitleDeliveryMethod.Embed)
                {
                    continue;
                }

                if (subtitleStream.isTextSubtitleStream() == MediaStream.IsTextFormat(profile.getFormat()) && Utils.equalsIgnoreCase(profile.getFormat(), subtitleStream.getCodec()))
                {
                    return profile;
                }
            }
        }

        // Look for an external or hls profile that matches the stream type (text/graphical) and doesn't require conversion
        SubtitleProfile tempVar = new SubtitleProfile();
        tempVar.setMethod(SubtitleDeliveryMethod.Encode);
        tempVar.setFormat(subtitleStream.getCodec());
        SubtitleProfile tempVar2 = getExternalSubtitleProfile(subtitleStream, subtitleProfiles, playMethod, false);
        SubtitleProfile tempVar3 = getExternalSubtitleProfile(subtitleStream, subtitleProfiles, playMethod, true);
        return (tempVar2 != null) ? tempVar2 : (tempVar3 != null) ? tempVar3 : tempVar;
    }

    private static SubtitleProfile getExternalSubtitleProfile(org.jellyfin.sdk.model.api.MediaStream subtitleStream, SubtitleProfile[] subtitleProfiles, PlayMethod playMethod, boolean allowConversion)
    {
        for (SubtitleProfile profile : subtitleProfiles)
        {
            if (profile.getMethod() != SubtitleDeliveryMethod.External && profile.getMethod() != SubtitleDeliveryMethod.Hls)
            {
                continue;
            }

            if (profile.getMethod() == SubtitleDeliveryMethod.Hls && playMethod != PlayMethod.Transcode)
            {
                continue;
            }

            if (!profile.SupportsLanguage(subtitleStream.getLanguage()))
            {
                continue;
            }

            if ((profile.getMethod() == SubtitleDeliveryMethod.External && subtitleStream.isTextSubtitleStream() == MediaStream.IsTextFormat(profile.getFormat())) || (profile.getMethod() == SubtitleDeliveryMethod.Hls && subtitleStream.isTextSubtitleStream()))
            {
                boolean requiresConversion = !Utils.equalsIgnoreCase(subtitleStream.getCodec(), profile.getFormat());

                if (!requiresConversion)
                {
                    return profile;
                }

                if (!allowConversion)
                {
                    continue;
                }

                if (subtitleStream.isTextSubtitleStream() && subtitleStream.getSupportsExternalStream() && supportsSubtitleConversionTo(subtitleStream, profile.getFormat()))
                {
                    return profile;
                }
            }
        }

        return null;
    }

    public static boolean supportsSubtitleConversionTo(org.jellyfin.sdk.model.api.MediaStream mediaStream, String codec)
    {
        if (!mediaStream.isTextSubtitleStream())
        {
            return false;
        }

        // Can't convert from this
        return !("ass".equalsIgnoreCase(mediaStream.getCodec()) || "ssa".equalsIgnoreCase(mediaStream.getCodec()) || "ass".equalsIgnoreCase(codec) || "ssa".equalsIgnoreCase(codec));
    }
}
