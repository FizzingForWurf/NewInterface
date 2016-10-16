package itrans.newinterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import itrans.newinterface.Internet.VolleySingleton;

public class Splash extends AppCompatActivity {

    private int count;
    private Boolean end;
    private Boolean isFirstNumber = true;
    private Boolean isFinalBus = false;
    private String lastNo;
    private int label;

    private TextView splashDescription;
    private ProgressBar downloadProgress;
    private int downloadProgressInteger = 0;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private ArrayList<String> firstDirection = new ArrayList<>();
    private ArrayList<String> secondDirection = new ArrayList<>();

    HashMap<Integer, LatLngBounds> busStopsContainers = new HashMap<>();

    private boolean downloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        end = false;
        count = -50;
        label = 0;

        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
        db.open();
        int number2 = db.getNumberOfRows();
        db.close();

        BusServiceDBAdapter db1 = new BusServiceDBAdapter(getApplicationContext());
        db1.open();
        int number1 = db1.getNumberOfRows();
        db1.close();

        splashDescription = (TextView) findViewById(R.id.splashDescription);
        downloadProgress = (ProgressBar) findViewById(R.id.downloadProgress);
        ProgressBar circleProgress = (ProgressBar) findViewById(R.id.circleProgress);

        Log.e("NUMBER OF ENTRIES", String.valueOf(number1));
        Log.e("NUMBER OF ENTRIES2", String.valueOf(number2));
        Toast.makeText(this, String.valueOf(number1), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, String.valueOf(number2), Toast.LENGTH_SHORT).show();

        if(number1 <= 416 || number2 <= 5200){
            downloading = true;
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

            //getBusNo();
            startSortingBusStops();
        }else {
            splashDescription.setVisibility(View.VISIBLE);
            downloadProgress.setVisibility(View.INVISIBLE);
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
                    }
                }
            };
            timer.start();
        }
    }

    private void getBusNo(){
        count += 50;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusRoutes?$skip=" + String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("value");
                            if (jsonArray.length() < 50) {
                                //this means that its on the last page of bus routes...
                                end = true;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                int busDirection = services.getInt("Direction");
                                String busID = services.getString("BusStopCode");
                                if (isFirstNumber) {
                                    lastNo = busNo;
                                    isFirstNumber = false;
                                }
                                if (!busNo.equals(lastNo)) {
                                    label++;
                                    String busStopsOne = firstDirection.toString();
                                    String busStopsTwo = secondDirection.toString();
                                    if (secondDirection.isEmpty()) {
                                        //this means that the bus service is a loop
                                        busStopsTwo = "LOOP";
                                    }
                                    BusServiceDBAdapter db3 = new BusServiceDBAdapter(getApplicationContext());
                                    db3.open();
                                    db3.insertEntry(lastNo, busStopsOne, busStopsTwo);
                                    db3.close();
                                    lastNo = busNo;
                                    splashDescription.setText(Html.fromHtml("First time run initialisation" +
                                            "<br />" + "<small>" + "Please do not exit the application." + "</small>"));
                                    downloadProgressInteger = label;
                                    downloadProgress.setProgress(downloadProgressInteger);
                                    firstDirection.clear();
                                    secondDirection.clear();
                                    if (busDirection == 1) {
                                        firstDirection.add(busID);
                                    } else {
                                        secondDirection.add(busID);
                                    }
                                    if (end) {
                                        isFinalBus = true;
                                    }
                                } else {
                                    if (busDirection == 1) {
                                        firstDirection.add(busID);
                                    } else {
                                        secondDirection.add(busID);
                                    }
                                    if (isFinalBus) {
                                        if ((i + 1) == jsonArray.length()) {
                                            //this means that this is final entry and final stop.
                                            String busStopsOne = firstDirection.toString();
                                            String busStopsTwo = secondDirection.toString();
                                            if (secondDirection.isEmpty()) {
                                                //this means that the bus service is a loop
                                                busStopsTwo = "LOOP";
                                            }
                                            BusServiceDBAdapter db4 = new BusServiceDBAdapter(getApplicationContext());
                                            db4.open();
                                            db4.insertEntry(lastNo, busStopsOne, busStopsTwo);
                                            db4.close();
                                        }
                                    }
                                }
                            }
                            if (!end) {
                                getBusNo();
                            } else {
                                count = -50;
                                end = false;
                                startSortingBusStops();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        createErrorDialog();
                        end = true;
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "6oxHbzoDSzuXhgEvfYLqLQ==");
                headers.put("UniqueUserID", "2807eaf2-cf3e-4d9a-8468-edd50fd0c1cd");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    private void startSortingBusStops(){
        ArrayList<Double> longitudinalArray = new ArrayList<>();
        longitudinalArray.add(103.597870);
        ArrayList<Double> latitudinalArray = new ArrayList<>();
        latitudinalArray.add(1.476153);
        final double latitudeIncrement = 4/110.574; //in kilometers, 0.036175
        final double longitudeIncrement = 4/(111.320 * Math.cos(Math.toRadians(1.476153))); //in kilometers, 0.035944376
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
        while (lastDistance1 <= longitudinalDistance){
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
        while (lastDistance2 <= latitudinalDistance){
            variable2 -= latitudeIncrement;
            latitudinalArray.add(variable2);
            Location newPoint = new Location("newPoint");
            newPoint.setLatitude(variable2);
            newPoint.setLongitude(103.597870);
            lastDistance2 = topLeft.distanceTo(newPoint);
            Log.e("SORTING LATITUDE", String.valueOf(variable2));
            Log.e("SORTING DISTANCE2", String.valueOf(lastDistance2));
        }

        int number = -1;
        Log.e("SORTING SIZES", String.valueOf(latitudinalArray.size()) + ", " + String.valueOf(longitudinalArray.size()));
        for (int i = 0; i < latitudinalArray.size() - 2; i++){
            for (int a = 0; a < longitudinalArray.size() - 2; a++){
                number++;
                LatLng Southwest = new LatLng(latitudinalArray.get(i + 1), longitudinalArray.get(a));
                LatLng Northeast = new LatLng(latitudinalArray.get(i), longitudinalArray.get(a + 1));
                LatLngBounds container = new LatLngBounds(Southwest, Northeast);
                busStopsContainers.put(number, container);
                Log.e("SORTING HASHMAP", Southwest.toString() + ", " + Northeast.toString());
            }
        }
        Log.e("HASHMAP SIZE", String.valueOf(busStopsContainers.size()));

        for (int r = 0; r < busStopsContainers.size(); r++){
            LatLngBounds point = busStopsContainers.get(r);
            LatLng southwest = point.southwest;
            LatLng northeast = point.northeast;
            BusStopContainerDBAdapter db = new BusStopContainerDBAdapter(getApplicationContext());
            db.open();
            db.insertEntry(southwest.toString(), northeast.toString(), "EMPTY");
            db.close();
        }

        getBusStops();
    }

    private void getBusStops(){
        count += 50;
        JsonObjectRequest requestBusStop = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip=" + String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            JSONArray jsonArray = response.getJSONArray("value");
                            if(jsonArray.length() < 50) {
                                end = true;  //this means that its on the last page of bus routes...
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                label++;
                                splashDescription.setText(Html.fromHtml("First time run initialisation" +
                                        "<br />" + "<small>" + "Please do not exit the application." + "</small>"));
                                downloadProgressInteger = label;
                                downloadProgress.setProgress(downloadProgressInteger);
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busStopId = services.getString("BusStopCode");
                                String busStopName = services.getString("Description");
                                String roadName = services.getString("RoadName");
                                String lat = services.getString("Latitude");
                                String lng = services.getString("Longitude");

                                Double doubleLat = Double.parseDouble(lat);
                                Double doublelng = Double.parseDouble(lng);
                                LatLng coordinates = new LatLng(doubleLat, doublelng);

                                if (busStopId.startsWith("0")) {
                                    busStopId = busStopId.substring(1, 5);
                                }

                                String latitude = String.valueOf(round(coordinates.latitude, 5));
                                latitude = latitude.replaceAll("[.]","");
                                if (i == 0){
                                    Log.e("ENCODE NAME", latitude);
                                }

                                BusNumberDBAdapter db5 = new BusNumberDBAdapter(getApplicationContext());
                                db5.open();
                                db5.insertBusStop(busStopId, busStopName, latitude, roadName, coordinates.toString());
                                db5.close();

                                for (int t = 0; t < busStopsContainers.size(); t++){
                                    LatLngBounds container = busStopsContainers.get(t);
                                    if (container.contains(coordinates)){
                                        BusStopContainerDBAdapter db = new BusStopContainerDBAdapter(getApplicationContext());
                                        db.open();
                                        String initialBusStops = db.getInitialBusStops(t + 1);
                                        if (initialBusStops.equals("EMPTY")){
                                            db.updateBusStops(t + 1, busStopId);
                                        }else{
                                            db.updateBusStops(t + 1, initialBusStops + "," + busStopId);
                                        }
                                        db.close();
                                        break;
                                    }
                                }
                            }
                            if(!end){
                                getBusStops();
                            }else{
                                Intent mainActivity = new Intent(Splash.this, MainActivity.class);
                                startActivity(mainActivity);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                createErrorDialog();
                end = true;
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "6oxHbzoDSzuXhgEvfYLqLQ==");
                headers.put("UniqueUserID", "2807eaf2-cf3e-4d9a-8468-edd50fd0c1cd");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(requestBusStop);
    }

    private void createErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
        builder.setTitle("Error");
        builder.setCancelable(true);
        builder.setMessage("Oh no! Something went wrong during the download. Please ensure that you have a " +
                "stable network connection before trying again later.");

        builder.setPositiveButton(
                "Close app",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Splash.this.finish();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void cancelVolleyRequests(){
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (downloading) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
            builder.setTitle("Confirm exit");
            builder.setCancelable(true);
            builder.setMessage("iTrans needs to download external files and requires you to stay on this page. " +
                    "Are you sure you would like to exit?");

            builder.setPositiveButton(
                    "Exit",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Splash.this.finish();
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
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelVolleyRequests();
    }
}
