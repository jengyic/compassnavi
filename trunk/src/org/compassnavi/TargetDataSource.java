package org.compassnavi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class TargetDataSource 
{
	// Database fields
	private SQLiteDatabase mDatabase;
	private MySQLiteHelper mDbHelper;
	  
	// DateTime Format
	private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

	/**
	 * 
	 * @param context
	 */
	public TargetDataSource(Context context) 
	{
		this.mDbHelper = new MySQLiteHelper(context);
	}

	/**
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException 
	{
		this.mDatabase = this.mDbHelper.getWritableDatabase();
	}

	/**
	 * 
	 */
	public void close() 
	{
		this.mDbHelper.close();
	}

	/**
	 * 
	 * @param target
	 * @return
	 */
	public long addTarget(final NavigationTarget target)
	{
		final ContentValues values = new ContentValues();
		  
		// values.put(MySQLiteHelper.COLUMN_TARGET_ID, comment);
		values.put(MySQLiteHelper.COLUMN_TARGET_NAME, target.getName());
		values.put(MySQLiteHelper.COLUMN_TARGET_LATITUDE, target.getLatitude());
		values.put(MySQLiteHelper.COLUMN_TARGET_LONGITUDE, target.getLongitude());
		values.put(MySQLiteHelper.COLUMN_TARGET_DATE_CREATED, this.mDateFormat.format(target.getDateCreated()));
		values.put(MySQLiteHelper.COLUMN_TARGET_DATE_LAST_ACCESS, this.mDateFormat.format(target.getDateLastAccess()));
		  
		long insertId = this.mDatabase.insert(MySQLiteHelper.TABLE_TARGET, null, values);
		  
		target.setId(insertId);
		  
		return insertId;
	}
	  
	/**
	 * 
	 * @param target
	 */
	public void deleteTarget(final NavigationTarget target) 
	{
		this.mDatabase.delete(MySQLiteHelper.TABLE_TARGET, MySQLiteHelper.COLUMN_TARGET_ID + " = " + target.getId(), null);
	}

	/**
	 * 
	 * @param target
	 */
	public void updateTarget(final NavigationTarget target)
	{
		final ContentValues values = new ContentValues();
		  
		values.put(MySQLiteHelper.COLUMN_TARGET_NAME, target.getName());
		values.put(MySQLiteHelper.COLUMN_TARGET_LATITUDE, target.getLatitude());
		values.put(MySQLiteHelper.COLUMN_TARGET_LONGITUDE, target.getLongitude());
		values.put(MySQLiteHelper.COLUMN_TARGET_DATE_LAST_ACCESS, this.mDateFormat.format(target.getDateLastAccess()));
		
		this.mDatabase.update(MySQLiteHelper.TABLE_TARGET, values, MySQLiteHelper.COLUMN_TARGET_ID + " = " + target.getId(), null);
	}

	/**
	 * 
	 * @return
	 */
	public List<NavigationTarget> getAllTargets() 
	{
		final List<NavigationTarget> targets = new ArrayList<NavigationTarget>();

		final Cursor cursor = this.mDatabase.query(MySQLiteHelper.TABLE_TARGET, MySQLiteHelper.TABLE_TARGET_COLUMNS, null, null, null, null, MySQLiteHelper.COLUMN_TARGET_DATE_LAST_ACCESS + " DESC");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) 
		{
			final NavigationTarget target = this.cursorToTarget(cursor);
			targets.add(target);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return targets;
	}
	
	/**
	 * 
	 * @param cursor
	 * @return
	 */
	private NavigationTarget cursorToTarget(Cursor cursor ) 
	{
		final NavigationTarget target = new NavigationTarget();
		target.setId(cursor.getLong(0));
		target.setName(cursor.getString(1));
		target.setLatitude(cursor.getLong(2));
		target.setLongitude(cursor.getLong(3));
		// target.setDateCreated(cursor.get(4));
		// target.setDateLastAccess(cursor.get(4));
	    return target;
	}
}

