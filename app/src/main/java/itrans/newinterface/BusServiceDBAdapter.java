package itrans.newinterface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class BusServiceDBAdapter {
    private static final String DATABASE_NAME = "buses.db";
    private static final String DATABASE_TABLE = "busServices";
    private static final int DATABASE_VERSION =1;
    private SQLiteDatabase _db;
    private final Context context;
    private MyDBOpenHelper dbHelper;

    public static final String KEY_ID ="_id";
    public static final String BUS_NO = "bus_no";
    public static final String BUS_DIRECTION_ONE = "bus_one";
    public static final String BUS_DIRECTION_TWO = "bus_two";
    protected static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " " + "(" + KEY_ID +
            " integer primary key autoincrement, " + BUS_NO + " text not null, " + BUS_DIRECTION_ONE +
            " text not null, " + BUS_DIRECTION_TWO + " text not null);";

    public class MyDBOpenHelper extends SQLiteOpenHelper {
        public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
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

    public BusServiceDBAdapter(Context _context){
        this.context = _context;

        dbHelper = new MyDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void close(){
        _db.close();
    }

    public void open() throws SQLiteException{
        try{
            _db = dbHelper.getWritableDatabase();
        }
        catch(SQLiteException e){
            _db = dbHelper.getReadableDatabase();
        }
    }

    public long insertEntry(String entryNo, String directionOne, String directionTwo){
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(BUS_NO, entryNo);
        newEntryValues.put(BUS_DIRECTION_ONE, directionOne);
        newEntryValues.put(BUS_DIRECTION_TWO, directionTwo);

        return _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public void updateDirectionOneEntry(int position, String busStops){
        ContentValues updatedValues = new ContentValues();

        updatedValues.put(BUS_DIRECTION_ONE, busStops);

        _db.update(DATABASE_TABLE, updatedValues, KEY_ID + "=" + position, null);
    }

    public void updateDirectionTwoEntry(int position, String busStops){
        ContentValues updatedValues = new ContentValues();

        updatedValues.put(BUS_DIRECTION_TWO, busStops);

        _db.update(DATABASE_TABLE, updatedValues, KEY_ID + "=" + position, null);
    }

    public boolean removeEntry(long _rowIndex){
        if(_db.delete(DATABASE_TABLE, KEY_ID + "-" + _rowIndex, null) <= 0){
            return false;
        }
        return true;
    }

    public void removeAllEntries(){
        _db.delete(DATABASE_TABLE, null, null);
    }

    public ArrayList<String> retrieveBusServices() {
        String myString = "";
        Cursor c = null;
        ArrayList<String> BusNumbers = new ArrayList<>();
        try{
            c = _db.query(DATABASE_TABLE, new String[] {KEY_ID, BUS_NO},null, null, null, null, null);
        }
        catch(SQLiteException e){
            Log.e("DBADAPTER","Retrieval error");
        }

        if(c != null && c.getCount()>0){
            c.moveToFirst();
            do{
                myString = c.getString(1);
                BusNumbers.add(myString);
            }while(c.moveToNext());
        }

        return BusNumbers;
    }

    public int getNumberOfRows(){
        return (int) DatabaseUtils.queryNumEntries(_db, DATABASE_TABLE);
    }

    public String getdirectionone(int position){
        String[] columns = {KEY_ID, BUS_NO, BUS_DIRECTION_ONE, BUS_DIRECTION_TWO};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iONE = c.getColumnIndex(BUS_DIRECTION_ONE);
        if (c != null){
            c.moveToFirst();
            return c.getString(iONE);
        }
        return null;
    }

    public String getdirectiontwo(int position){
        String[] columns = {KEY_ID, BUS_NO, BUS_DIRECTION_ONE, BUS_DIRECTION_TWO};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iTWO = c.getColumnIndex(BUS_DIRECTION_TWO);
        if (c != null){
            c.moveToFirst();
            return c.getString(iTWO);
        }
        return null;
    }

    public String getBusService(int position){
        String[] columns = {KEY_ID, BUS_NO, BUS_DIRECTION_ONE, BUS_DIRECTION_TWO};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iTWO = c.getColumnIndex(BUS_NO);
        if (c != null){
            c.moveToFirst();
            return c.getString(iTWO);
        }
        return null;
    }
}
