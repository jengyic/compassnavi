package org.compassnavi;

import android.os.Build;
import android.provider.Settings;
import org.compassnavi.R;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class ApplicationSingleton 
{
	private final static ApplicationSingleton mInstance = new ApplicationSingleton();
	private final static String AndroidId2_2OrLater = "9774D56D682E549C";
	
	private boolean mIsEmulator;
	
	/**
	 * 
	 */
	private ApplicationSingleton()
	{
		final String androidId = Settings.Secure.ANDROID_ID;
		this.mIsEmulator = 
				"sdk".equals(Build.PRODUCT) || 
				"google_sdk".equals(Build.PRODUCT) || 
				androidId == null || 
				androidId == AndroidId2_2OrLater;
	}

	/**
	 * 
	 * @return
	 */
	public static ApplicationSingleton getInstance()
	{
		return mInstance;
	}
	
	/**
	 * 
	 * @return
	 */
	public Boolean isEmulator()
	{
		return this.mIsEmulator;
	}
}
