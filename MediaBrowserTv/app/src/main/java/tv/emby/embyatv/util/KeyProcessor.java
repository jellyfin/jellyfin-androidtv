package tv.emby.embyatv.util;

import android.app.Activity;
import android.view.KeyEvent;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.ui.GridButton;

/**
 * Created by Eric on 4/17/2015.
 */
public class KeyProcessor {

    public static boolean HandleKey(int key, BaseRowItem rowItem, Activity activity) {
        switch (key) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (rowItem == null) return false;

                switch (rowItem.getItemType()) {

                    case BaseItem:
                        BaseItemDto item = rowItem.getBaseItem();
                        switch (item.getType()) {
                            case "Movie":
                            case "Episode":
                            case "TvChannel":
                            case "Program":
                                // give some audible feedback
                                Utils.Beep();
                                // retrieve full item and play
                                Utils.retrieveAndPlay(item.getId(), activity);
                                return true;
                        }
                        break;
                    case Person:
                        break;
                    case Server:
                        break;
                    case User:
                        break;
                    case Chapter:
                        break;
                    case SearchHint:
                        switch (rowItem.getSearchHint().getType()) {
                            case "Movie":
                            case "Episode":
                            case "TvChannel":
                            case "Program":
                                // give some audible feedback
                                Utils.Beep();
                                // retrieve full item and play
                                Utils.retrieveAndPlay(rowItem.getItemId(), activity);
                                return true;
                        }
                        break;
                    case LiveTvChannel:
                    case LiveTvRecording:
                        // give some audible feedback
                        Utils.Beep();
                        // retrieve full item and play
                        Utils.retrieveAndPlay(rowItem.getItemId(), activity);
                        return true;
                    case LiveTvProgram:
                        // give some audible feedback
                        Utils.Beep();
                        // retrieve channel this program belongs to and play
                        Utils.retrieveAndPlay(rowItem.getProgramInfo().getChannelId(), activity);
                        return true;
                    case GridButton:
                        break;
                }
                break;
        }
        return false;
    }

}

