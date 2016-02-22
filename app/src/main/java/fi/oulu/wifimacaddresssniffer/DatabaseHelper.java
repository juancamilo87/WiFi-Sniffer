package fi.oulu.wifimacaddresssniffer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JuanCamilo on 2/22/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "mac_addresses.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String TABLES_FIELDS =
            MacAddress._ID + " integer primary key autoincrement," +
                    MacAddress.MAC_ADDRESS + " text default ''," +
                    MacAddress.SIGNAL_STRENGTH + " integer default 0," +
                    MacAddress.LAST_SEEN + " text default ''," +
                    MacAddress.LAST_SEEN_EPOCH + " real default 0," +
                    "UNIQUE (" + MacAddress.MAC_ADDRESS + ")";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MacAddress.TABLE_NAME + " (" +
                    TABLES_FIELDS +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MacAddress.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static abstract class MacAddress implements BaseColumns {

        public static final String TABLE_NAME = "mac_addresses";

        public static final String _ID = "_id";
        public static final String MAC_ADDRESS = "mac_address";
        public static final String SIGNAL_STRENGTH = "signal_strength";
        public static final String LAST_SEEN = "last_seen";
        public static final String LAST_SEEN_EPOCH = "last_seen_epoch";
    }

    public static long insert(Context context, String mac_address, int signal_strength, String last_seen)
    {
        DatabaseHelper mDbHelper = new DatabaseHelper(context);
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MacAddress.MAC_ADDRESS, mac_address);
        values.put(MacAddress.SIGNAL_STRENGTH, signal_strength);
        values.put(MacAddress.LAST_SEEN, last_seen);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(last_seen);
            long epoch = convertedDate.getTime()/1000;
            values.put(MacAddress.LAST_SEEN_EPOCH, epoch);

            // Which row to update, based on the ID
            String selection = MacAddress.MAC_ADDRESS + " LIKE ?";
            String[] selectionArgs = { mac_address };

            int count = db.update(
                    MacAddress.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            if(count==0)
            {
                // Insert the new row, returning the primary key value of the new row
                long newRowId;
                newRowId = db.insert(
                        MacAddress.TABLE_NAME,
                        null,
                        values);
                return newRowId;
            }
            return -2;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static Cursor read(Context context, long timestamp)
    {
        DatabaseHelper mDbHelper = new DatabaseHelper(context);
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                MacAddress._ID,
                MacAddress.MAC_ADDRESS,
                MacAddress.SIGNAL_STRENGTH,
                MacAddress.LAST_SEEN_EPOCH
        };

        Cursor c = db.query(
                MacAddress.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                MacAddress.LAST_SEEN_EPOCH + " > ?",                                // The columns for the WHERE clause
                new String[]{String.valueOf(timestamp)},                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        return c;
    }
}
