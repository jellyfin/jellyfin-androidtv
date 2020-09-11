package org.jellyfin.androidtv.data.compat;

import org.jellyfin.apiclient.model.mediainfo.MediaProtocol;
import org.jellyfin.apiclient.model.session.PlayMethod;

@Deprecated
public class StreamInfoSorterComparator extends BaseStreamInfoSorter {
    private int level;

    public StreamInfoSorterComparator(int level) {
        this.level = level;
    }

    @Override
    protected int getValue(StreamInfo info) {
        switch (level) {
            case 0: {
                // Nothing beats direct playing a file
                if (info.getPlayMethod() == PlayMethod.DirectPlay && info.getMediaSource().getProtocol() == MediaProtocol.File) {
                    return 0;
                }

                return 1;
            }
            case 1: {
                switch (info.getPlayMethod()) {
                    // Let's assume direct streaming a file is just as desirable as direct playing a remote url
                    case DirectStream:
                    case DirectPlay:
                        return 0;
                    default:
                        return 1;
                }
            }
            case 2: {
                switch (info.getMediaSource().getProtocol()) {
                    case File:
                        return 0;
                    default:
                        return 1;
                }
            }
        }

        throw new IllegalArgumentException("Unrecognized level");
    }
}
