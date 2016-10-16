package itrans.newinterface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BusStopContainerDBAdapter {

    private static final String DATABASE_NAME = "containers.db";
    private static final String DATABASE_TABLE = "busStopContainers";
    private static final int DATABASE_VERSION =1;
    private SQLiteDatabase _db;
    private final Context context;
    private MyDBOpenHelper dbHelper;

    private static final String KEY_ID ="_id";
    private static final String SOUTHWEST = "southwest";
    private static final String NORTHEAST = "northeast";
    private static final String BUS_STOPS_ARRAY = "busStops";
    private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " " + "(" + KEY_ID +
            " integer primary key autoincrement, " + SOUTHWEST + " text not null, " + NORTHEAST + " text not null, "
            + BUS_STOPS_ARRAY + " text not null);";

    private class MyDBOpenHelper extends SQLiteOpenHelper {
        MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVerison){

        }
    }

    public BusStopContainerDBAdapter(Context _context){
        this.context = _context;

        dbHelper = new MyDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void close(){
        _db.close();
    }

    public void open() throws SQLiteException {
        try{
            _db = dbHelper.getWritableDatabase();
        }
        catch(SQLiteException e){
            _db = dbHelper.getReadableDatabase();
        }
    }

    public long insertEntry(String southwest, String northeast, String busStops){
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(SOUTHWEST, southwest);
        newEntryValues.put(NORTHEAST, northeast);
        newEntryValues.put(BUS_STOPS_ARRAY, busStops);

        return _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public boolean removeEntry(long _rowIndex){
        return _db.delete(DATABASE_TABLE, KEY_ID + "-" + _rowIndex, null) > 0;
    }

    public void removeAllEntries(){
        _db.delete(DATABASE_TABLE, null, null);
    }

    public String getInitialBusStops(int position) {
        String[] columns = {KEY_ID, SOUTHWEST, NORTHEAST, BUS_STOPS_ARRAY};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null,null,null,null);
        int BusStops = c.getColumnIndex(BUS_STOPS_ARRAY);
        if (c != null){
            c.moveToFirst();
            return c.getString(BusStops);
        }
        return null;
    }

    public void updateBusStops(int position, String busStopId){
        ContentValues updatedValues = new ContentValues();

        updatedValues.put(BUS_STOPS_ARRAY, busStopId);

        _db.update(DATABASE_TABLE, updatedValues, KEY_ID + "=" + position, null);
    }
}
