package itrans.newinterface.Search;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import itrans.newinterface.BusNumberDBAdapter;
import itrans.newinterface.Nearby.AnimatedExpandableListView;
import itrans.newinterface.R;

public class SearchServices extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private AnimatedExpandableListView lvBusServices;

    private String busService;
    private ArrayList<Marker> busStopsMarker = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_services);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            busService = extras.getString("SEARCH BUS SERVICE");
            if (actionBar != null) {
                actionBar.setTitle(busService);
            }
        }

        lvBusServices = (AnimatedExpandableListView) findViewById(R.id.lvBusService);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.searchServicesMap);
        mapFragment.getMapAsync(this);

        findBusStops();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                return true;
            }
        });
    }

    private void findBusStops() {
        busStopsMarker.clear();
        Thread thread = new Thread() {
            public void run() {
                InputStream data = getResources().openRawResource(R.raw.bus_service_number);
                Scanner sc = new Scanner(data);

                int counter = 1;
                while (sc.hasNextLine()) {
                    String busNumber = sc.nextLine();
                    if (busNumber.toLowerCase().equals(busService.toLowerCase())) {
                        InputStream data1 = getResources().openRawResource(R.raw.direction_one);
                        InputStream data2 = getResources().openRawResource(R.raw.direction_two);

                        Scanner sc1 = new Scanner(data1);
                        Scanner sc2 = new Scanner(data2);

                        int check = 1;
                        while (sc1.hasNextLine()) {
                            String busStops = sc1.nextLine();
                            if (check == counter) {
                                String something = busStops.substring(busStops.indexOf("[") + 1, busStops.indexOf("]"));
                                String[] resplit = something.split(",");

                                final BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
                                db.open();
                                for (String s : resplit) {
                                    String raw = db.getBusStopLatLng(s);
                                    final String name = db.getBusStopName(s);
                                    String without = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
                                    String[] split = without.split(",");
                                    final LatLng latLng = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (map != null) {
                                                Marker marker = map.addMarker(new MarkerOptions()
                                                        .position(latLng)
                                                        .title(name));
                                                busStopsMarker.add(marker);
                                            }
                                        }
                                    });
                                }
                                db.close();
                                break;
                            }
                            check++;
                        }

                        check = 1;
                        while (sc2.hasNextLine()) {
                            String busStops = sc2.nextLine();
                            if (check == counter) {
                                if (!busStops.equals("LOOP")) {
                                    String something = busStops.substring(busStops.indexOf("[") + 1, busStops.indexOf("]"));
                                    String[] resplit = something.split(",");

                                    BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
                                    db.open();
                                    for (String s : resplit) {
                                        String raw = db.getBusStopLatLng(s);
                                        final String name = db.getBusStopName(s);
                                        String without = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
                                        String[] split = without.split(",");
                                        final LatLng latLng = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (map != null) {
                                                    Marker marker = map.addMarker(new MarkerOptions()
                                                            .position(latLng)
                                                            .title(name));
                                                    busStopsMarker.add(marker);
                                                }
                                            }
                                        });
                                    }
                                    db.close();
                                }
                                break;
                            }
                            check++;
                        }
                        sc1.close();
                        sc2.close();
                        break;
                    }
                    counter++;
                }
                Log.e("TEST1", "END");
            }
        };
        thread.start();
    }
}
