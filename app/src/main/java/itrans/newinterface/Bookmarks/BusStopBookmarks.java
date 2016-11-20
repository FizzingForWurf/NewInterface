package itrans.newinterface.Bookmarks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class BusStopBookmarks {
    private static final String DATABASE_NAME = "busStopBookmarks.db";
    private static final String DATABASE_TABLE = "busStopBookmarks";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase _db;
    private final Context context;
    private MyDBOpenHelper dbHelper;

    private static final String KEY_ID = "_id";
    private static final String BOOKMARK_TYPE = "bookmark_type";
    private static final String BOOKMARK_DATA = "bookmark_data";

    private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " " + "(" + KEY_ID +
            " integer primary key autoincrement, " + BOOKMARK_TYPE + " text not null, " + BOOKMARK_DATA +
            " text not null);";

    private class MyDBOpenHelper extends SQLiteOpenHelper {
        public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVerison) {

        }
    }

    public BusStopBookmarks(Context _context) {
        this.context = _context;

        dbHelper = new MyDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void close() {
        _db.close();
    }

    public void open() throws SQLiteException {
        try {
            _db = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            _db = dbHelper.getReadableDatabase();
        }
    }

    public int getNumberOfRows() {
        return (int) DatabaseUtils.queryNumEntries(_db, DATABASE_TABLE);
    }

    public void removeAllEntries() {
        _db.delete(DATABASE_TABLE, null, null);
    }

    public long setUpBookmarksLayout(String busStopType, String busStopData) {
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(BOOKMARK_TYPE, busStopType);
        newEntryValues.put(BOOKMARK_DATA, busStopData);

        return _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public String getData(int position){
        String[] columns = {KEY_ID, BOOKMARK_TYPE, BOOKMARK_DATA};
        Cursor c = null;
        try {
            c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
            int iId = c.getColumnIndex(BOOKMARK_DATA);
            c.moveToFirst();
            return c.getString(iId);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public void updateBusStop(String newBusStopData){
        ContentValues newBusStops = new ContentValues();
        newBusStops.put(BOOKMARK_DATA, newBusStopData);
        _db.update(DATABASE_TABLE, newBusStops, KEY_ID + "=" + 2, null);
    }

    public void updateBusService(String newBusStopData){
        ContentValues newBusStops = new ContentValues();
        newBusStops.put(BOOKMARK_DATA, newBusStopData);
        _db.update(DATABASE_TABLE, newBusStops, KEY_ID + "=" + 1, null);
    }

    public void updateBusAndStop(String newBusStopData){
        ContentValues newBusStops = new ContentValues();
        newBusStops.put(BOOKMARK_DATA, newBusStopData);
        _db.update(DATABASE_TABLE, newBusStops, KEY_ID + "=" + 3, null);
    }

    public boolean isStopSaved(String id){
        Cursor cur = _db.rawQuery("SELECT * FROM " + DATABASE_TABLE + " WHERE busStop_id = '" + id + "'", null);
        boolean exist = (cur.getCount() > 0);
        cur.close();
        return exist;
    }
}