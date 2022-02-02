package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.session.PlayMethod;

@Deprecated
public class StreamBuilder
{
    public static SubtitleProfile GetSubtitleProfile(MediaStream subtitleStream, SubtitleProfile[] subtitleProfiles, PlayMethod playMethod)
    {
        if (playMethod != PlayMethod.Transcode && !subtitleStream.getIsExternal())
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

                if (subtitleStream.getIsTextSubtitleStream() == MediaStream.IsTextFormat(profile.getFormat()) && Utils.equalsIgnoreCase(profile.getFormat(), subtitleStream.getCodec()))
                {
                    return profile;
                }
            }
        }

        // Look for an external or hls profile that matches the stream type (text/graphical) and doesn't require conversion
        SubtitleProfile tempVar = new SubtitleProfile();
        tempVar.setMethod(SubtitleDeliveryMethod.Encode);
        tempVar.setFormat(subtitleStream.getCodec());
        SubtitleProfile tempVar2 = GetExternalSubtitleProfile(subtitleStream, subtitleProfiles, playMethod, false);
        SubtitleProfile tempVar3 = GetExternalSubtitleProfile(subtitleStream, subtitleProfiles, playMethod, true);
        return (tempVar2 != null) ? tempVar2 : (tempVar3 != null) ? tempVar3 : tempVar;
    }

    private static SubtitleProfile GetExternalSubtitleProfile(MediaStream subtitleStream, SubtitleProfile[] subtitleProfiles, PlayMethod playMethod, boolean allowConversion)
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

            if ((profile.getMethod() == SubtitleDeliveryMethod.External && subtitleStream.getIsTextSubtitleStream() == MediaStream.IsTextFormat(profile.getFormat())) || (profile.getMethod() == SubtitleDeliveryMethod.Hls && subtitleStream.getIsTextSubtitleStream()))
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

                if (subtitleStream.getIsTextSubtitleStream() && subtitleStream.getSupportsExternalStream() && subtitleStream.SupportsSubtitleConversionTo(profile.getFormat()))
                {
                    return profile;
                }
            }
        }

        return null;
    }
}
