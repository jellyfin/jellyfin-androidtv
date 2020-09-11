package org.jellyfin.androidtv.ui.livetv;

import org.jellyfin.androidtv.ui.ProgramGridCell;

/**
 * Created by Eric on 9/6/2015.
 */
public interface ILiveTvGuide {
    public void displayChannels(int start, int max);
    public long getCurrentLocalStartDate();
    public void showProgramOptions();
    public void setSelectedProgram(ProgramGridCell programView);
}
