package tv.mediabrowser.mediabrowsertv.settings;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.widget.TextView;

import org.acra.ACRA;

import java.io.IOException;

import tv.mediabrowser.mediabrowsertv.startup.LogonCredentials;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.util.Utils;


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
        ver.setText(Utils.VersionString() + " " + TvApp.getApplication().getRegistrationString());

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
                    Utils.SaveLoginCredentials(new LogonCredentials(TvApp.getApplication().getApiClient().getServerInfo(), TvApp.getApplication().getCurrentUser()));
                } catch (IOException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
            }
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
    }
}

