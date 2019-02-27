package org.jellyfin.androidtv.settings;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.startup.LogonCredentials;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.Utils;

import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set version info
        TextView ver = (TextView) getActivity().findViewById(R.id.settings_version_info);
        ver.setText(Utils.VersionString());

        // conditionally hide options that don't apply
        PreferenceCategory cat = (PreferenceCategory) findPreference("pref_playback_category");
        if (DeviceUtils.isFireTv() && !DeviceUtils.is50()) {
            cat.removePreference(findPreference("pref_audio_option"));
        }
        if (DeviceUtils.is60()) {
            cat.removePreference(findPreference("pref_bitstream_ac3"));
        } else {
            cat.removePreference(findPreference("pref_refresh_switching"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    updatePreference(preferenceGroup.getPreference(j));
                }
            } else {
                updatePreference(preference);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //If logon setting is changed to this user, save credentials
        if (key.equals("pref_login_behavior")) {
            ListPreference listPreference = (ListPreference) findPreference(key);
            if (listPreference.getValue().equals("1")) {
                try {
                    Utils.SaveLoginCredentials(new LogonCredentials(TvApp.getApplication().getApiClient().getServerInfo(), TvApp.getApplication().getCurrentUser()), TvApp.CREDENTIALS_PATH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        updatePreference(findPreference(key));

        if (key.equals("pref_send_path_external") && ((CheckBoxPreference)findPreference(key)).isChecked()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("W A R N I N G")
                    .setMessage("This feature will only work if you have properly setup your library on the server with network paths or setup Path Substitution AND the external player app you are using can directly access these locations over the network.  If playback fails or you didn't understand any of that, disable this option.")
                    .setPositiveButton(R.string.btn_got_it, null)
                    .show();
        }
    }

    private String[] extPlayerVideoDep = new String[] {"pref_enable_cinema_mode","pref_refresh_switching","pref_audio_option","pref_bitstream_ac3","pref_bitstream_dts"};
    private String[] extPlayerLiveTvDep = new String[] {"pref_live_direct","pref_enable_vlc_livetv"};

    private void updatePreference(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            if (listPreference.getKey().equals("pref_login_behavior")) {
                CheckBoxPreference pwPrompt = (CheckBoxPreference) findPreference("pref_auto_pw_prompt");
                pwPrompt.setEnabled(TvApp.getApplication().getConfiguredAutoCredentials().getUserDto().getHasPassword());
                String extra = "";
                if (listPreference.getValue().equals("1")) {
                    if (!TvApp.getApplication().getConfiguredAutoCredentials().getUserDto().getId().equals(TvApp.getApplication().getCurrentUser().getId())) {
                        listPreference.setEnabled(false);
                        pwPrompt.setEnabled(false);
                        extra = getActivity().getString(R.string.lbl_paren_login_as)+TvApp.getApplication().getConfiguredAutoCredentials().getUserDto().getName()+getActivity().getString(R.string.lbl_to_change_paren);
                    }
                    listPreference.setSummary(getActivity().getString(R.string.lbl_login_as) + TvApp.getApplication().getConfiguredAutoCredentials().getUserDto().getName() + extra);
                } else {
                    listPreference.setSummary(listPreference.getEntry());
                    pwPrompt.setEnabled(false);
                }
            } else {
                listPreference.setSummary(listPreference.getEntry());
            }
        }

        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference cb = (CheckBoxPreference) preference;
            if (cb.getKey().equals("pref_live_direct")) {
                // enable other live tv direct only options
                Preference live = findPreference("pref_enable_vlc_livetv");
                if (live != null) live.setEnabled(cb.isChecked());
//                Preference shift = findPreference("pref_live_shift");
//                if (shift != null) shift.setEnabled(!cb.isChecked());
            } else
//            if (cb.getKey().equals("pref_live_shift")) {
//                // enable/disable related options
//                Preference direct = findPreference("pref_live_direct");
//                if (direct != null) direct.setEnabled(!cb.isChecked());
//            } else
            if (cb.getKey().equals("pref_video_use_external")) {
                // enable/disable other related items
                Preference direct = findPreference("pref_send_path_external");
                if (direct != null) direct.setEnabled(cb.isChecked());
                for (String key: extPlayerVideoDep) {
                    Preference pref = findPreference(key);
                    if (pref != null) pref.setEnabled(!cb.isChecked());
                }
            } else if (cb.getKey().equals("pref_live_tv_use_external")) {
                // enable/disable other related items
                for (String key: extPlayerLiveTvDep) {
                    Preference pref = findPreference(key);
                    if (pref != null) pref.setEnabled(!cb.isChecked());
                }
            }
        }

    }
}

