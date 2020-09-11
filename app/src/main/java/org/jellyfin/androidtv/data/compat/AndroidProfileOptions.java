package org.jellyfin.androidtv.data.compat;

@Deprecated
public class AndroidProfileOptions {
    public int DefaultH264Level = 40;
    public String DefaultH264Profile = "high|main|baseline|constrained baseline";
    public boolean SupportsAc3 = false;
    public boolean SupportsHls = true;
    public boolean SupportsDtsHdMa = false;
    public boolean SupportsDts = false;
    public boolean SupportsTrueHd = false;
    public boolean SupportsMkv = false;

    public AndroidProfileOptions() {}

    public AndroidProfileOptions(String deviceName) {
        deviceName = deviceName.toLowerCase().replace(" ", "");

        if (deviceName.startsWith("aft")) {
            DefaultH264Level = 40;
            SupportsAc3 = true;
        } else if (deviceName.contains("nexusplayer")) {
            DefaultH264Level = 41;
        } else if (deviceName.contains("adt-1")) {

            DefaultH264Level = 41;

        } else if (deviceName.contains("nvidiashield")) {
            SupportsDtsHdMa = true;
            SupportsDts = true;
            SupportsTrueHd = true;
            SupportsAc3 = true;
        }
    }
}
