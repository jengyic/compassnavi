package org.compassnavi;

import org.compassnavi.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class Preferences extends PreferenceActivity 
{
	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		// Invoke android gps preferences
		final Preference customPref = (Preference) findPreference("andGPSPref");
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{ 
			public boolean onPreferenceClick(final Preference preference) 
			{
    			final Intent intLocSrcSettings = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    			startActivity(intLocSrcSettings);
				return true;
			}		 
		});
	}
}
