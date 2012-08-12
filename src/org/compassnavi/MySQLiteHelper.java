package org.compassnavi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class MySQLiteHelper extends SQLiteOpenHelper 
{
	public static final String TABLE_TARGET = "target";

	public static final String COLUMN_TARGET_ID = "id";
	public static final String COLUMN_TARGET_NAME = "name";
	public static final String COLUMN_TARGET_LATITUDE = "latitude";
	public static final String COLUMN_TARGET_LONGITUDE = "longitude";
	public static final String COLUMN_TARGET_DATE_CREATED = "date_created";
	public static final String COLUMN_TARGET_DATE_LAST_ACCESS = "date_last_access";
	
	public static String[] TABLE_TARGET_COLUMNS = 
		{ 
		COLUMN_TARGET_ID,
		COLUMN_TARGET_NAME,
		COLUMN_TARGET_LATITUDE,
		COLUMN_TARGET_LONGITUDE,
		COLUMN_TARGET_DATE_CREATED,
		COLUMN_TARGET_DATE_LAST_ACCESS 
		};
	
	private static final String DATABASE_NAME = "targets.db";
	private static final int DATABASE_VERSION = 1;

	/**
	 * 
	 * @param context
	 */
	public MySQLiteHelper(Context context) 
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		final String createStatement = 
				"CREATE TABLE " + TABLE_TARGET + "(" +
						COLUMN_TARGET_ID + " integer primary key autoincrement, " + 
						COLUMN_TARGET_NAME + " text text not null, " +
						COLUMN_TARGET_LATITUDE + " real not null, " +
						COLUMN_TARGET_LONGITUDE + " real not null, " +
						COLUMN_TARGET_DATE_CREATED + " date not null, " +
						COLUMN_TARGET_DATE_LAST_ACCESS + " date not null, " +
				");";
		db.execSQL(createStatement);
	}

	/**
	 * 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TARGET);
	    this.onCreate(db);
	}

}
