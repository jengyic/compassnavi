package org.compassnavi;

import java.util.Random;

import org.compassnavi.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CompassNaviActivity extends Activity implements SensorEventListener, LocationListener, GpsStatus.Listener
{
	private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagneticField;
	private LocationManager mLocationManager;
	private CompassNaviView mCompassNaviView;
	private SharedPreferences mSharedPreferences;
	private NavigationTarget mNavigationTarget;
    private BearingProvider mBearingProvider;
    private GravityMovingAverage mGravityMovingAverage;
	
	private Boolean mIsEmulator;

	// private static final String EXCEPTION_URL = "http://flux.dnsdojo.org/opengpx/trace/";

	// Preferences names and default values
	private static final String PREFS_KEY_BEARING_PROVIDER = "bearingProviderPref";
	private static final String PREFS_KEY_UNIT_DISTANCE = "distUnitPref";
	private static final String PREFS_KEY_UNIT_ALTITUDE = "altUnitPref";
	private static final String PREFS_KEY_KEEP_SCREEN_ON = "keepScreenOn";
	private static final String PREFS_KEY_GPS_MIN_TIME = "minTime";
	private static final String PREFS_KEY_GPS_MIN_DISTANCE = "minDistance";
	private static final String PREFS_KEY_AVG_WINDOW_SIZE = "compSmoothAvg";
	private static final String PREFS_KEY_USE_WEIGHTED_AVG = "useWeightedAverage";
	
	// Some default values
	private static final String PREFS_DEFAULT_BEARING_PROVIDER = BearingProvider.Compass.toString();
	private static final String PREFS_DEFAULT_UNIT_DISTANCE = Unit.meter.toString();
	private static final String PREFS_DEFAULT_UNIT_ALTITUDE = Unit.meter.toString();
	private static final boolean PREFS_DEFAULT_KEEP_SCREEN_ON = false;
	private static final int PREFS_DEFAULT_GPS_MIN_TIME = 500;
	private static final float PREFS_DEFAULT_GPS_MIN_DISTANCE = 0.5f;
	private static final int PREFS_DEFAULT_AVG_WINDOW_SIZE = 5;
	private static final boolean PREFS_DEFAULT_USE_WEIGHTED_AVG = false;
	
	private static final String DEFAULT_TARGET_NAME = "Unnamed";
	private static final float  DEFAULT_TARGET_LATITUDE = 0.0f;
	private static final float  DEFAULT_TARGET_LONGITUDE= 0.0f;

	// Sensor values
	private int mPreviousState = -1;	
	
	// Compass values
	float[] inR = new float[16];
	float[] I = new float[16];
	float[] gravity;
	float[] geomag = new float[3];
	float[] orientVals = new float[3];

	double azimuth = 0;
	
	static final float ALPHA = 0.2f;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

    	// Use some exception handler
    	// ExceptionHandler.register(this, EXCEPTION_URL);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Detect Android Emulator
        this.mIsEmulator = ApplicationSingleton.getInstance().isEmulator();
        
        // Set the default navigation target
        this.mNavigationTarget = new NavigationTarget();
        this.mNavigationTarget.setName(DEFAULT_TARGET_NAME);
        this.mNavigationTarget.setLatitude(DEFAULT_TARGET_LATITUDE);
        this.mNavigationTarget.setLongitude(DEFAULT_TARGET_LONGITUDE);

        final Bundle bunExtras = this.getIntent().getExtras();
        if (bunExtras != null)
        {
        	this.mNavigationTarget.setName(DEFAULT_TARGET_NAME);
        	
	        if (bunExtras.containsKey("name"))
	        	this.mNavigationTarget.setName(bunExtras.getString("name"));
	        if (bunExtras.containsKey("latitude"))
	        	this.mNavigationTarget.setLatitude(bunExtras.get("latitude"));
	        if (bunExtras.containsKey("longitude"))
	        	this.mNavigationTarget.setLongitude(bunExtras.get("longitude"));
        }

        this.mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        this.mSensorAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensorMagneticField = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        this.mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        setContentView(R.layout.navicompass);
        this.mCompassNaviView = (CompassNaviView) this.findViewById(R.id.CompassNaviView);
        this.mCompassNaviView.setTarget(this.mNavigationTarget);

        // Create new instance for compass smoothing
		this.mGravityMovingAverage = new GravityMovingAverage();

		// Create a random azimuth value in emulator
		if (ApplicationSingleton.getInstance().isEmulator())
		{
			final Random randomGenerator = new Random();
			this.mCompassNaviView.setAzimuth(randomGenerator.nextFloat() * 360);
			this.mCompassNaviView.invalidate();
		}
        
    }

    /**
     * 
     */
    @Override
    public void onStart()
    {
    	super.onStart();
    	
    	this.getPreferences();
    }

    /**
     * 
     */
    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	this.mSensorManager.unregisterListener(this, this.mSensorAccelerometer);
    	this.mSensorManager.unregisterListener(this, this.mSensorMagneticField);
		this.mLocationManager.removeGpsStatusListener(this);
    	this.mLocationManager.removeUpdates(this);

		this.mCompassNaviView.setKeepScreenOn(false);
    }

    /**
     * 
     */
    @Override
    public void onResume()
    {
    	super.onResume();

    	// Read GPS minimum time preferences
		final int intMinTime = this.getIntegerPreferenceValue(PREFS_KEY_GPS_MIN_TIME, PREFS_DEFAULT_GPS_MIN_TIME, R.string.pref_min_time);
		// Read GPS minimum distance preference
    	final float fltMinDistance = this.getFloatPreferenceValue(PREFS_KEY_GPS_MIN_DISTANCE, PREFS_DEFAULT_GPS_MIN_DISTANCE, R.string.pref_min_distance);
    	
    	// Orientation (compass)
    	this.mGravityMovingAverage.clear();
		final int intWindowSize = this.getIntegerPreferenceValue(PREFS_KEY_AVG_WINDOW_SIZE, PREFS_DEFAULT_AVG_WINDOW_SIZE, R.string.pref_comp_smooth_avg_window);
    	this.mGravityMovingAverage.setWindowSize(intWindowSize);
 		this.mGravityMovingAverage.setUseWeightedAverage(this.mSharedPreferences.getBoolean(PREFS_KEY_USE_WEIGHTED_AVG, PREFS_DEFAULT_USE_WEIGHTED_AVG));

    	this.mSensorManager.registerListener(this, this.mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	this.mSensorManager.registerListener(this, this.mSensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);

    	// Location (GPS)
		this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intMinTime, fltMinDistance, this);

		// Add GpsStatusListener
		this.mLocationManager.addGpsStatusListener(this);

		// Set keep screen on property
		final boolean blnKeepScreenOn = this.mSharedPreferences.getBoolean(PREFS_KEY_KEEP_SCREEN_ON, PREFS_DEFAULT_KEEP_SCREEN_ON);
		this.mCompassNaviView.setKeepScreenOn(blnKeepScreenOn);		
    }

    /**
     * 
     * @param key
     * @param defValue
     * @param resid
     * @return
     */
    private int getIntegerPreferenceValue(String key, int defValue, int resid)
    {
    	final String stringValue = this.mSharedPreferences.getString(key, Integer.toString(defValue));
		int value = defValue;
		try 
		{
			value = Integer.parseInt(stringValue);
		} 
		catch (NumberFormatException nfe)
		{
			final String title = getString(resid);
			final String errorMessage = String.format("Invalid integer value for preference %s: %s (using default value)", title, stringValue);
			Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		}
    	return value;
    }

    /**
     * 
     * @param key
     * @param defValue
     * @param resid
     * @return
     */
    private float getFloatPreferenceValue(String key, float defValue, int resid)
    {
    	final String stringValue = this.mSharedPreferences.getString(key, Float.toString(defValue));
		float value = defValue;
		try 
		{
			value = Float.parseFloat(stringValue);
		} 
		catch (NumberFormatException nfe)
		{
			final String title = getString(resid);
			final String errorMessage = String.format("Invalid float value for preference %s: %s (using default value)", title, stringValue);
			Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		}
    	return value;
    }
    
    /**
     * 
     */
    private void getPreferences()
    {
        // Load preferences
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        final String strBearingProvider = this.mSharedPreferences.getString(PREFS_KEY_BEARING_PROVIDER, PREFS_DEFAULT_BEARING_PROVIDER);
        this.mBearingProvider = BearingProvider.valueOf(strBearingProvider);
        // this.mCompassNaviView.setBearingProvider(bearingProvider);

        final String strUnitDistance = this.mSharedPreferences.getString(PREFS_KEY_UNIT_DISTANCE, PREFS_DEFAULT_UNIT_DISTANCE);
        final Unit unitDistance = this.getUnit(strUnitDistance);
        this.mCompassNaviView.setUnitForDistance(unitDistance);

        final String strUnitAltitude = this.mSharedPreferences.getString(PREFS_KEY_UNIT_ALTITUDE, PREFS_DEFAULT_UNIT_ALTITUDE);
        final Unit unitAltitude = this.getUnit(strUnitAltitude);
        this.mCompassNaviView.setUnitForAltitude(unitAltitude);
        
        // Log.d("Bearing provider", bearingProvider.toString());
        // Log.d("Unit (distance)", unitDistance.toString());
        // Log.d("Unit (altitude)", unitAltitude.toString());
    }
    
    /**
     * 
     * @param unitSystem
     * @return
     */
    private Unit getUnit(final String unitSystem)
    {
    	Unit unit;
    	if (unitSystem.equalsIgnoreCase("metric"))
    		unit = Unit.meter;
    	else
    		unit = Unit.feet;
    	return unit;
    }
    
    /**
     * 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    /**
     * 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
    	{
    		case R.id.menu_target:
    			this.showTargetDialog();
    			return true;
    		case R.id.menu_preferences:
    			final Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
    			startActivity(settingsActivity);
    			return true;
    		case R.id.menu_sat_info:
    			this.showSatelliteInformation();
    			return true;
    	}
    	return false;
    }

    /**
     * 
     * @param sb
     */
    private void addHtmlTableHeaderRow(final StringBuilder sb)
    {
    	sb.append("<table>");
    	sb.append("  <tr>");
    	sb.append("    <th>PRN</th>");
    	sb.append("    <th>Fix</th>");
    	sb.append("    <th>Azimuth</th>");
    	sb.append("    <th>Elevation</th>");
    	sb.append("    <th>SNR</th>");
    	sb.append("  </tr>");
    }
    
    /**
     * 
     * @param sb
     * @param azimuth
     * @param elevation
     * @param prn
     * @param snr
     * @param usedInFix
     */
    private void addHtmlTableGpsSatRow(final StringBuilder sb, final float azimuth, final float elevation, final int prn, final float snr, final boolean usedInFix)
    {
    	final String fix = (usedInFix ? "Yes" : "No");
    	
    	String textStyle;
    	if (!usedInFix)
    		textStyle = "text-decoration:line-through;color:grey";
    	else
    		textStyle = "color:" + ColorGradientHelper.getRedGreenGradientHtml(0, 60, snr);
    	
    	sb.append(String.format("<tr style=\"%s\">", textStyle));
    	sb.append(String.format("<td align=\"right\">%d</td>", prn));
    	sb.append(String.format("<td align=\"center\">%s</td>", fix));
    	sb.append(String.format("<td align=\"right\">%.0f</td>", azimuth));
    	sb.append(String.format("<td align=\"right\">%.0f</td>", elevation));
    	sb.append(String.format("<td align=\"right\">%.2f</td>", snr));
    	sb.append("</tr>");
    }
    
    /**
     * 
     */
    private void showSatelliteInformation()
    {
        final GpsStatus gpsStatus = this.mLocationManager.getGpsStatus(null);
        
        final StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<body style=\"background-color:#000000; color:#d8d8d8\">");

        boolean firstRow = true;
        boolean rowsAdded = false;
        for (final GpsSatellite gpsSatellite : gpsStatus.getSatellites())
        {
        	// Add table header
        	if (firstRow)
        	{
        		this.addHtmlTableHeaderRow(html);
        		firstRow = false;
        	}
        	
        	final float azimuth = gpsSatellite.getAzimuth();
        	final float elevation = gpsSatellite.getElevation();
        	final int prn = gpsSatellite.getPrn();
        	final float snr = gpsSatellite.getSnr();
        	final boolean usedInFix = gpsSatellite.usedInFix();

        	this.addHtmlTableGpsSatRow(html, azimuth, elevation, prn, snr, usedInFix);

        	rowsAdded = true;
        }

        // Add some test data in the emulator
        if (this.mIsEmulator && !rowsAdded)
        {
    		this.addHtmlTableHeaderRow(html);

        	this.addHtmlTableGpsSatRow(html, 187.0f, 53.0f, 1, 39.3f, true);
        	this.addHtmlTableGpsSatRow(html, 234.0f, 11.0f, 2, 17.2f, false);

        	rowsAdded = true;
        }
        
        if (rowsAdded) 
        	html.append("</table>");
        else
        	html.append("<center><font color=\"red\">No satellites found.</font></center>");

        html.append("</html>");

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_sat_info);
        
        // final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		// alertDialog.setTitle(R.string.menu_sat_info);

		final WebView message = new WebView(this);
		message.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null);
		builder.setView(message);
		// alertDialog.setView(message);
		builder.setCancelable(false);
		// alertDialog.setButton("OK", new DialogInterface.OnClickListener()
		builder.setNegativeButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
				// return;
			}
		});
		
		final AlertDialog alertDialog = builder.create();
		alertDialog.show();
    }
    
    /**
     * 
     */
    private void showTargetDialog()
    {
    	final CoordinateHelper coordinateHelper = new CoordinateHelper(this.mNavigationTarget.getLatitude(), this.mNavigationTarget.getLongitude());
        final View viewTarget = LayoutInflater.from(this).inflate(R.layout.settarget, null);
        
        // Set existing values for name and coordinates
        final EditText etName = (EditText) viewTarget.findViewById(R.id.TargetName);
        etName.setText(this.mNavigationTarget.getName());
        final EditText etLatitude = (EditText) viewTarget.findViewById(R.id.TargetLatitude);
        etLatitude.setText(coordinateHelper.getLatitudeString());
        final EditText etLongitude = (EditText) viewTarget.findViewById(R.id.TargetLongitude);
        etLongitude.setText(coordinateHelper.getLongitudeString());
        
        final AlertDialog dialog = new AlertDialog.Builder(this)
        	.setTitle("Set target")
        	.setView(viewTarget)
        	.setPositiveButton(R.string.ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) 
				{
					setNavigationTarget(etName.getText().toString(), etLatitude.getText().toString(), etLongitude.getText().toString());
				}
			})
			.setNegativeButton(R.string.cancel, new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which) 
				{
				}				
			})
        	.create();
        dialog.show();
    }

    /**
     * 
     * @param name
     * @param latitude
     * @param longitude
     */
    private void setNavigationTarget(String name, String latitude, String longitude)
    {
    	CoordinateHelper coordinateHelper = new CoordinateHelper();
    	if (coordinateHelper.ParseFromText(String.format("%s %s", latitude, longitude)))
		{
        	this.mNavigationTarget.setName(name);
        	this.mNavigationTarget.setLatitude(coordinateHelper.getLatitude());
        	this.mNavigationTarget.setLongitude(coordinateHelper.getLongitude());

        	// this.saveNavigationTarget();
        	
            this.mCompassNaviView.setTarget(this.mNavigationTarget);
		}
    	else 
    	{
    		Toast.makeText(this, "Unable to parse coordinates.", Toast.LENGTH_LONG).show();
    	}
    }

	@Override
	public void onGpsStatusChanged(int event) 
	{
		switch (event) 
		{
	        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	        	/*
	            if (this.mCurrentLocation != null)
	                isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
	        	*/
	            if (this.mLocationManager != null)
	            {
		            final GpsStatus gpsStatus = this.mLocationManager.getGpsStatus(null);
	                Integer numberOfSatellites = 0; 
		            for (final GpsSatellite gpsSatellite : gpsStatus.getSatellites())
		            {
			            if (gpsSatellite.usedInFix())	
			            	numberOfSatellites++;
		            }
		            this.mCompassNaviView.setSatelliteCount(numberOfSatellites);
		            this.mCompassNaviView.invalidate();
	            }

	            /*
	            if (isGPSFix) 
	            { 
	            	// A fix has been acquired.
	            }
	            else
	            { 
	            	// The fix has been lost.
	            }
				*/
	            break;
	        case GpsStatus.GPS_EVENT_FIRST_FIX:
	            // Do something.
	            // isGPSFix = true;
	
	            break;
		}
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		if (location != null)
		{
			this.mCompassNaviView.setLocation(location);
			// this.mLastLocationMillis = SystemClock.elapsedRealtime();
			if (this.mBearingProvider.equals(BearingProvider.GPS))
				this.mCompassNaviView.setAzimuth(location.getBearing());
			this.mCompassNaviView.invalidate();
		}
	}

	@Override
	public void onProviderDisabled(String provider) 
	{
		// TODO Auto-generated method stub	
	}

	@Override
	public void onProviderEnabled(String provider) 
	{
		// TODO Auto-generated method stub	
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		if (status != this.mPreviousState)
		{
			String strNewStatus = String.format("GPS Status: ", provider);
			if (status == LocationProvider.AVAILABLE)
				strNewStatus += "Available";
			else if (status == LocationProvider.OUT_OF_SERVICE)
				strNewStatus += "Out of service";
			else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				strNewStatus += "Temporarily unavailable";
			
			Toast.makeText(this, strNewStatus, Toast.LENGTH_SHORT).show();
			this.mPreviousState = status;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
	 * @see http://developer.android.com/reference/android/hardware/Sensor.html#TYPE_ACCELEROMETER
	 */
	protected float[] lowPass( float[] input, float[] output ) {
	    if ( output == null ) return input;

	    for ( int i=0; i<input.length; i++ ) {
	        output[i] = output[i] + ALPHA * (input[i] - output[i]);
	    }
	    return output;
	}
	
	/**
	 * 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) 
	{
	    if (!this.mBearingProvider.equals(BearingProvider.Compass))
	    	return;

		// If the sensor data is unreliable return
	    if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
	        return;

	    // Gets the value of the sensor that has been changed
	    switch (event.sensor.getType()) {  
	        case Sensor.TYPE_ACCELEROMETER:
	            // gravity = event.values.clone();
	            // gravity = lowPass(event.values, gravity);
	            gravity = this.mGravityMovingAverage.add(event.values);
	            break;
	        case Sensor.TYPE_MAGNETIC_FIELD:
	            geomag = event.values.clone();
	            break;
	    }
	    
	 // If gravity and geomag have values then find rotation matrix
	    if (gravity != null && geomag != null) 
	    {
	        // checks that the rotation matrix is found
	        final boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
	        if (success) {
	        	// SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
	            SensorManager.getOrientation(inR, orientVals);
	            azimuth = Math.toDegrees(orientVals[0]);
	            azimuth = (azimuth + 360) % 360;
	            // pitch = Math.toDegrees(orientVals[1]);
	            // roll = Math.toDegrees(orientVals[2]);
	            
				this.mCompassNaviView.setAzimuth((float)azimuth);
				this.mCompassNaviView.invalidate();
	        }
	    }		
	}

    /**
     * 
     */
    /* private void saveNavigationTarget()
    {
        SharedPreferences.Editor editor = this.mSharedPreferences.edit();
        
        editor.putString(PREFS_KEY_TARGET_NAME, this.mNavigationTarget.getName());
        editor.putFloat(PREFS_KEY_TARGET_LATITUDE, (float) this.mNavigationTarget.getLatitude());
        editor.putFloat(PREFS_KEY_TARGET_LONGITUDE, (float) this.mNavigationTarget.getLongitude());
        
        editor.commit();
    } */
    
}
