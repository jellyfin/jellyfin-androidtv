package org.jellyfin.androidtv.ui.livetv;

import android.widget.RelativeLayout;

public interface ILiveTvGuide {
    public void displayChannels(int start, int max);
    public long getCurrentLocalStartDate();
    public void showProgramOptions();
    public void setSelectedProgram(RelativeLayout programView);
    public void refreshFavorite(String channelId);
}
