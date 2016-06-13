package tv.emby.embyatv.settings;


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

import java.io.IOException;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.livetv.TvManager;
import tv.emby.embyatv.startup.LogonCredentials;
import tv.emby.embyatv.util.Utils;


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
        ver.setText(String.format("%s %s", Utils.VersionString(), TvApp.getApplication().getRegistrationString()));

        // conditionally hide options that don't apply
        PreferenceCategory cat = (PreferenceCategory) findPreference("pref_playback_category");
        if (Utils.isFireTv() && !Utils.is50()) cat.removePreference(findPreference("pref_audio_option"));
        if (Utils.is60()) {
            cat.removePreference(findPreference("pref_bitstream_ac3"));
        } else {
            cat.removePreference(findPreference("pref_refresh_switching"));
        }
        if (!TvApp.getApplication().isRegistered()) {
            //Indicate that cinema mode requires premiere
            CheckBoxPreference cm = (CheckBoxPreference) cat.findPreference("pref_enable_cinema_mode");
            cm.setEnabled(false);
            cm.setSummary(R.string.lbl_cm_premiere);
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
                    Utils.SaveLoginCredentials(new LogonCredentials(TvApp.getApplication().getApiClient().getServerInfo(), TvApp.getApplication().getCurrentUser()), "tv.mediabrowser.login.json");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (key.equals("pref_guide_sort_date")) {
            TvManager.resetChannels();
        }

        updatePreference(findPreference(key));
    }

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
            }
        }
    }
}

