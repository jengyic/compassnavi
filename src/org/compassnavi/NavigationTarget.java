package org.compassnavi;

import android.location.Location;
import org.compassnavi.R;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class NavigationTarget extends Location
{
	
	private static final int NAME_MAX_CHARS = 16;

	private String mstrName = "";

	/**
	 * 
	 */
	public NavigationTarget()
	{
		this("Dummy");
	}
	
	/**
	 * 
	 * @param provider
	 */
	public NavigationTarget(String provider) 
	{
		super(provider);
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
    	if (name.length() > NAME_MAX_CHARS)
    	{
    		name = name.substring(0, NAME_MAX_CHARS);
    		int intIndexOfBlank = name.lastIndexOf(" ");
    		if (intIndexOfBlank != -1)
    			name = name.substring(0, intIndexOfBlank);
    	}
		this.mstrName = name;
	}

	/**
	 * 
	 * @param latitude
	 */
	public void setLatitude(Object latitude)
	{
		if (latitude instanceof Double)
		{
			super.setLatitude((Double) latitude);
		}
		else if (latitude instanceof Float)
		{
			super.setLatitude(((Float) latitude).doubleValue());
		}
	}
	
	/**
	 * 
	 * @param longitude
	 */
	public void setLongitude(Object longitude)
	{
		if (longitude instanceof Double)
		{
			super.setLongitude((Double) longitude);
		}
		else if (longitude instanceof Float)
		{
			super.setLongitude(((Float) longitude).doubleValue());
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String getName()
	{
		return this.mstrName;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLatitudeString()
	{
		return this.mstrName;
	}
}
