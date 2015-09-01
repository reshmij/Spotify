package nanodegree.reshmi.com.spotify;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.Locale;

/**
 * Created by annupinju on 8/25/2015.
 */
public class PrefsFragment extends PreferenceFragment{

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        final ListPreference listPreference = (ListPreference) findPreference(getString(R.string.country_codes_preference_key));

        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        if(listPreference.getEntries()==null) {
            listPreference.setEntries(Locale.getISOCountries());
            listPreference.setEntryValues(Locale.getISOCountries());
            listPreference.setDefaultValue("US");
        }
    }
}
