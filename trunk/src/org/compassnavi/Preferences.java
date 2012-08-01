package org.compassnavi;

import org.compassnavi.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		// Invoke android gps preferences
		Preference customPref = (Preference) findPreference("andGPSPref");
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{ 
			public boolean onPreferenceClick(Preference preference) 
			{
    			Intent intLocSrcSettings = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    			startActivity(intLocSrcSettings);
				return true;
			}		 
		});
	}
}
