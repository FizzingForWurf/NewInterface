package itrans.newinterface.Internet;

import android.content.Context;
import android.database.Cursor;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends MultiDexApplication {

    private static MyApplication sInstance;

    private List<String> BusNumbers;
    private List<Integer> BusIDs;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static MyApplication getInstance(){
        return sInstance;
    }

    public static Context getAppContext(){
        return sInstance.getApplicationContext();
    }

    public MyApplication(){
        BusNumbers = new ArrayList<String>();
        BusIDs = new ArrayList<Integer>();
    }
}
