package org.jellyfin.androidtv.constant;

// Names used in jellyfin-web
// Only uses those that are configurable in web settings
public enum HomeSectionType {
    LATEST_MEDIA("latestmedia"),
    LIBRARY_TILES_SMALL("smalllibrarytiles"),
    LIBRARY_BUTTONS("librarybuttons"),
    RESUME("resume"),
    RESUME_AUDIO("resumeaudio"),
    ACTIVE_RECORDINGS("activerecordings"),
    NEXT_UP("nextup"),
    LIVE_TV("livetv"),

    NONE("none");

    public static HomeSectionType getByName(String name) {
        for (HomeSectionType type : HomeSectionType.values()) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }

        return null;
    }

    public final String name;

    HomeSectionType(String name) {
        this.name = name;
    }
}
