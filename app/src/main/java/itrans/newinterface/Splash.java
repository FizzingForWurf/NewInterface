package itrans.newinterface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import itrans.newinterface.Internet.VolleySingleton;

public class Splash extends AppCompatActivity {
    private TextView splashDescription;
    private ProgressBar circleProgress;

    Thread setUpThread;

    HashMap<Integer, LatLngBounds> busStopsContainers = new HashMap<>();

    private boolean downloading = false;
    private boolean stopSettingUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashDescription = (TextView) findViewById(R.id.splashDescription);
        circleProgress = (ProgressBar) findViewById(R.id.circleProgress);
    }

    private void addBusServices() {
        Log.e("BUS SERVICE", "BUSSERVICE");
        BusServiceDBAdapter db = new BusServiceDBAdapter(getApplicationContext());
        db.open();
        db.removeAllEntries();

        InputStream busServiceData = getResources().openRawResource(R.raw.bus_service_number);
        InputStream directionOneData = getResources().openRawResource(R.raw.direction_one);
        InputStream directionTwoData = getResources().openRawResource(R.raw.direction_two);

        Scanner scBusNumber = new Scanner(busServiceData);
        Scanner scDirectionOne = new Scanner(directionOneData);
        Scanner scDirectionTwo = new Scanner(directionTwoData);

        int counter = 1;
        while (scBusNumber.hasNextLine()) {
            String data = scBusNumber.nextLine();
            db.insertEntry(data, "WAIT", "WAIT");
            Log.e("TEST", String.valueOf(counter));
            counter++;
        }

        counter = 1;
        while (scDirectionOne.hasNextLine()) {
            String data = scDirectionOne.nextLine();
            db.updateDirectionOneEntry(counter, data);
            Log.e("TEST", String.valueOf(counter));
            counter++;
        }

        counter = 1;
        while (scDirectionTwo.hasNextLine()) {
            String data = scDirectionTwo.nextLine();
            db.updateDirectionOneEntry(counter, data);
            Log.e("TEST", String.valueOf(counter));
            counter++;
        }

        Log.e("COMPLETE TRANSFER", String.valueOf(db.getNumberOfRows()));
        db.close();
        //startSortingBusStops();
        if (!stopSettingUp) {
            addBusStops();
        }
    }

    private void startSortingBusStops() {
        ArrayList<Double> longitudinalArray = new ArrayList<>();
        longitudinalArray.add(103.597870);
        ArrayList<Double> latitudinalArray = new ArrayList<>();
        latitudinalArray.add(1.476153);
        final double latitudeIncrement = 4 / 110.574; //in kilometers, 0.036175
        final double longitudeIncrement = 4 / (111.320 * Math.cos(Math.toRadians(1.476153))); //in kilometers, 0.035944376
        double lastDistance1 = 0;
        double lastDistance2 = 0;

        //Boundaries of Singapore
        //LatLng Northwest = new LatLng(1.476153, 103.597870);
        //LatLng Southeast = new LatLng(1.216673, 104.102554);

        Location topLeft = new Location("topleft");
        topLeft.setLatitude(1.476153);
        topLeft.setLongitude(103.597870);

        Location topRight = new Location("topright");
        topRight.setLatitude(1.476153);
        topRight.setLongitude(104.102554);

        Location bottomLeft = new Location("bottomleft");
        bottomLeft.setLatitude(1.216673);
        bottomLeft.setLongitude(103.597870);

        double longitudinalDistance = topLeft.distanceTo(topRight);
        double latitudinalDistance = topLeft.distanceTo(bottomLeft);
        Log.e("DISTANCES LATLNG", String.valueOf(latitudinalDistance) + ", " + String.valueOf(longitudinalDistance));

        //while loop for longitude
        double variable1 = 103.597870;
        while (lastDistance1 <= longitudinalDistance) {
            variable1 += longitudeIncrement;
            longitudinalArray.add(variable1);
            Location newTemporaryPoint = new Location("TemporaryPoint");
            newTemporaryPoint.setLatitude(1.476153);
            newTemporaryPoint.setLongitude(variable1);
            lastDistance1 = topLeft.distanceTo(newTemporaryPoint);
            Log.e("SORTING LONGITUDE", String.valueOf(variable1));
            Log.e("SORTING DISTANCE1", String.valueOf(lastDistance1));
        }

        //while loop for latitude
        double variable2 = 1.476153;
        while (lastDistance2 <= latitudinalDistance) {
            variable2 -= latitudeIncrement;
            latitudinalArray.add(variable2);
            Location newPoint = new Location("newPoint");
            newPoint.setLatitude(variable2);
            newPoint.setLongitude(103.597870);
            lastDistance2 = topLeft.distanceTo(newPoint);
            Log.e("SORTING LATITUDE", String.valueOf(variable2));
            Log.e("SORTING DISTANCE2", String.valueOf(lastDistance2));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("latitudinalArray", latitudinalArray.toString());
        editor.putString("longitudinalArray", longitudinalArray.toString());
        editor.apply();

        int number = -1;
        Log.e("SORTING SIZES", String.valueOf(latitudinalArray.size()) + ", " + String.valueOf(longitudinalArray.size()));
        for (int i = 0; i < latitudinalArray.size() - 2; i++) {
            for (int a = 0; a < longitudinalArray.size() - 2; a++) {
                number++;
                LatLng Southwest = new LatLng(latitudinalArray.get(i + 1), longitudinalArray.get(a));
                LatLng Northeast = new LatLng(latitudinalArray.get(i), longitudinalArray.get(a + 1));
                LatLngBounds container = new LatLngBounds(Southwest, Northeast);
                busStopsContainers.put(number, container);
                Log.e("SORTING HASHMAP", Southwest.toString() + ", " + Northeast.toString());
            }
        }
        Log.e("HASHMAP SIZE", String.valueOf(busStopsContainers.size()));

//        for (int r = 0; r < busStopsContainers.size(); r++){
//            LatLngBounds point = busStopsContainers.get(r);
//            LatLng southwest = point.southwest;
//            LatLng northeast = point.northeast;
//            BusStopContainerDBAdapter db = new BusStopContainerDBAdapter(getApplicationContext());
//            db.open();
//            db.insertEntry(southwest.toString(), northeast.toString(), "EMPTY");
//            db.close();
//        }

        addBusStops();
    }

    private void addBusStops() {
        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
        db.open();
        db.removeAllEntries();

        InputStream dataStream = getResources().openRawResource(R.raw.bus_stop_data);

        Scanner sc = new Scanner(dataStream);

        StringBuilder entry = new StringBuilder();
        int counter = 1;
        while (sc.hasNextLine()) {
            if (!stopSettingUp) {
                String data = sc.nextLine();
                if (data.equals("")) {
                    String[] split = entry.toString().split("[\\r\\n]");
                    String id = split[0];
                    String name = split[1];
                    String road = split[2];
                    String coordinates = split[3];

                    db.insertBusStop(id, name, road, coordinates);

                    entry.setLength(0);
                    Log.e("TEST", String.valueOf(counter));
                    counter++;
                } else {
                    entry.append(data).append('\n');
                }
            }else{
                break;
            }
        }

        Log.e("COMPLETE TRANSFER", String.valueOf(db.getNumberOfRows()));
        db.close();
        sc.close();

        if (!stopSettingUp) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("SETUPCOMPLETE", true);
            editor.apply();

            boolean hasCompletedSetUp = prefs.getBoolean("SETUPCOMPLETE", false);
            Log.e("NUMBER OF ENTRIES", String.valueOf(hasCompletedSetUp));

            Intent mainActivity = new Intent(Splash.this, MainActivity.class);
            startActivity(mainActivity);
            this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (downloading) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
            builder.setTitle("Confirm exit");
            builder.setCancelable(true);
            builder.setMessage("iTrans needs a while to set up. " +
                    "Are you sure you would like to exit?");

            builder.setPositiveButton(
                    "Exit",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            stopSettingUp = true;
                            Splash.super.onBackPressed();
                        }
                    });

            builder.setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
        db.open();
        int number2 = db.getNumberOfRows();
        db.close();

        BusServiceDBAdapter db1 = new BusServiceDBAdapter(getApplicationContext());
        db1.open();
        int number1 = db1.getNumberOfRows();
        db1.close();

        Log.e("NUMBER OF ENTRIES", String.valueOf(number1));
        Log.e("NUMBER OF ENTRIES", String.valueOf(number2));

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean hasCompletedSetUp = sharedPref.getBoolean("SETUPCOMPLETE", false);
        Log.e("NUMBER OF ENTRIES", String.valueOf(hasCompletedSetUp));

        if ((number1 <= 453 || number2 <= 5282) || !hasCompletedSetUp) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("SETUPCOMPLETE", false);
            editor.apply();

            downloading = true;
            splashDescription.setText("Setting up... Please wait a minute.");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    splashDescription.setText("Almost there...");
                }
            }, 35000);

            final Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    splashDescription.setText("Done");
                }
            }, 48000);

            BusNumberDBAdapter dbi = new BusNumberDBAdapter(getApplicationContext());
            dbi.open();
            dbi.removeAllEntries();
            Log.e("AFTER DELETE 1", String.valueOf(dbi.getNumberOfRows()));
            dbi.close();

            BusServiceDBAdapter db2 = new BusServiceDBAdapter(getApplicationContext());
            db2.open();
            db2.removeAllEntries();
            Log.e("AFTER DELETE 2", String.valueOf(db2.getNumberOfRows()));
            db2.close();

            setUpThread = new Thread() {
                public void run() {
                    addBusServices();
                }
            };
            setUpThread.start();
        } else {
            splashDescription.setVisibility(View.VISIBLE);
            circleProgress.setVisibility(View.INVISIBLE);

            Thread timer = new Thread() {
                public void run() {
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        Intent intent = new Intent(Splash.this, MainActivity.class);
                        startActivity(intent);
                        Splash.this.finish();
                    }
                }
            };
            timer.start();
        }
    }
}
