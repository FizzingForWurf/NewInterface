package itrans.newinterface;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import itrans.newinterface.Internet.VolleySingleton;

import static android.R.id.toggle;

public class FragmentNearby extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        AbsListView.OnScrollListener, ExpandableListView.OnGroupClickListener{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private NearbyExpandListAdapter ExpandableListAdapter;
    private ArrayList<NearbyBusStops> nearbyBusStops = new ArrayList<NearbyBusStops>();
    ArrayList<NearbyBusTimings> arrivalTimings = new ArrayList<>();

    private ProgressBar searchingProgress;
    private ExpandableListView lvNearby;
    private TextView tvNearbyError;
    private SwipeRefreshLayout nearbySwipe;

    private LocationManager locationManager;
    private Location mLocation;
    private LatLng mLatLng;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    public FragmentNearby() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentNearby.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentNearby newInstance(String param1, String param2) {
        FragmentNearby fragment = new FragmentNearby();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_nearby, container, false);
        searchingProgress = (ProgressBar) v.findViewById(R.id.searchingProgress);
        lvNearby = (ExpandableListView) v.findViewById(R.id.lvNearby);
        nearbySwipe = (SwipeRefreshLayout) v.findViewById(R.id.nearbySwipe);
        tvNearbyError = (TextView) v.findViewById(R.id.tvNearbyError);

        tvNearbyError.setVisibility(View.INVISIBLE);
        searchingProgress.setVisibility(View.INVISIBLE);
        lvNearby.setVisibility(View.VISIBLE);

        nearbySwipe.setOnRefreshListener(this);
        lvNearby.setOnScrollListener(this);
        lvNearby.setOnGroupClickListener(this);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();
        return v;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (lvNearby.getChildAt(0) != null) {
            nearbySwipe.setEnabled(lvNearby.getFirstVisiblePosition() == 0 && lvNearby.getChildAt(0).getTop() == 0);
        }
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if(!parent.isGroupExpanded(groupPosition)){
            // Do your Staff
            String busStopId = nearbyBusStops.get(groupPosition).getBusStopID();
            Log.e("GROUP EXPAND", String.valueOf(groupPosition) + ", " + busStopId);
            arrivalTimings.clear();
            findBusArrivalTimings(groupPosition, busStopId);
        }
        return false;
    }

    @Override
    public void onRefresh() {
        if (!locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER)) {
            //check whether gps is enabled
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
            nearbySwipe.setRefreshing(false);
        }else{
            //if gps is enabled already
            requestForGPSUpdates();
            if (mLocation != null){
                nearbyBusStops.clear();
                findNearby();
            }else{
                //this means that location cannot be detected and show message
                tvNearbyError.setVisibility(View.VISIBLE);
                lvNearby.setVisibility(View.INVISIBLE);
                nearbySwipe.setRefreshing(false);
            }
        }
    }

    private void findBusArrivalTimings(final int groupPosition, String busStopId){
        JsonObjectRequest BusStopRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="  +busStopId + "&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String eta;
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                String inService = services.getString("Status");
                                if(inService.equals("In Operation")) {
                                    JSONObject nextBus = services.getJSONObject("NextBus");
                                    eta = nextBus.getString("EstimatedArrival");
                                    String wheelC = nextBus.getString("Feature");
                                    String load = nextBus.getString("Load");
                                }else{
                                    eta = "Not in Operation";
                                }
                                NearbyBusTimings timings = new NearbyBusTimings();
                                timings.setBusService(busNo);
                                if (eta != null) {
                                    timings.setBusTiming(eta);
                                }else{
                                    timings.setBusTiming("Not available");
                                }
                                arrivalTimings.add(timings);
                            }
                            nearbyBusStops.get(groupPosition).setArrivalTimings(arrivalTimings);
                            Log.e("CHILD UPDATE", String.valueOf(groupPosition));
                            ExpandableListAdapter.notifyDataSetChanged();
                            //lvNearby.expandGroup(groupPosition);
                            //nearbyBusStops.get(groupPosition).setArrivalTimings(null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        Toast.makeText(getContext(), "That did not work:(", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "3SnRYzr/X0eKp2HvwTYtmg==");
                headers.put("UniqueUserID", "0bf7760d-15ec-4a1b-9c82-93562fcc9798");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(BusStopRequest);
    }

    private void findNearby(){
        tvNearbyError.setVisibility(View.INVISIBLE);
        lvNearby.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Finding...", Toast.LENGTH_SHORT).show();
        Thread thread = new Thread(){
            public void run(){
                BusNumberDBAdapter db = new BusNumberDBAdapter(getContext());
                db.open();
                for (int a = 0; a < db.getNumberOfRows(); a++){
                    double latDifference;
                    double lonDifference;

                    String latlng = db.getCoordinates(a + 1);
                    Log.e("TEST", String.valueOf(latlng));
                    String noBrace = latlng.substring(latlng.indexOf("(") + 1, latlng.indexOf(")"));
                    String[] latlong = noBrace.split( ",");
                    LatLng busStop = new LatLng(Double.parseDouble(latlong[0]), Double.parseDouble(latlong[1]));

                    if (busStop.latitude > mLatLng.latitude){
                        latDifference = busStop.latitude - mLatLng.latitude;
                    }else{
                        latDifference = mLatLng.latitude - busStop.latitude;
                    }

                    if (busStop.longitude > mLatLng.longitude){
                        lonDifference = busStop.longitude - mLatLng.longitude;
                    }else{
                        lonDifference = mLatLng.longitude - busStop.longitude;
                    }

                    if (latDifference + lonDifference <= 0.01302981126){
                        //this means that the bus stop is within the radius...
                        String busStopName = db.getName(a + 1);
                        String busStopId = db.getID(a + 1);
                        String busStopRoad = db.getRoad(a + 1);
                        Location busStopLocation = new Location("Bus Stop");
                        busStopLocation.setLatitude(busStop.latitude);
                        busStopLocation.setLongitude(busStop.longitude);
                        int distance = (int) busStopLocation.distanceTo(mLocation);
                        Log.e("NEARBY", busStopName + ", " + busStopId + ", " + String.valueOf(distance));

                        NearbyBusStops nearbyBusStops1 = new NearbyBusStops();
                        nearbyBusStops1.setBusStopName(busStopName);
                        nearbyBusStops1.setBusStopRoad(busStopRoad);
                        nearbyBusStops1.setBusStopID(busStopId);
                        nearbyBusStops1.setProximity(distance);
                        nearbyBusStops1.setArrivalTimings(new ArrayList<NearbyBusTimings>());

                        nearbyBusStops.add(nearbyBusStops1);
                    }
                }
                db.close();
                Log.e("NEARBY", String.valueOf(nearbyBusStops.size()));

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (nearbySwipe.isRefreshing())
                            nearbySwipe.setRefreshing(false);
                        searchingProgress.setVisibility(View.INVISIBLE);
                        populateExpandableListView();
                    }
                });
            }
        };
        thread.start();
    }

    private void populateExpandableListView(){
        Collections.sort(nearbyBusStops, new Comparator<NearbyBusStops>() {
            @Override
            public int compare(NearbyBusStops nearbyBusStops, NearbyBusStops t1) {
                return nearbyBusStops.getProximity() - t1.getProximity();
            }
        });
        ExpandableListAdapter = new NearbyExpandListAdapter(getContext(), nearbyBusStops);
        lvNearby.setAdapter(ExpandableListAdapter);
    }

    private void requestForGPSUpdates(){
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_LOW);

        try {
            String provider = locationManager.getBestProvider(c, true);
            mLocation = locationManager.getLastKnownLocation(provider);
            if (mLocation != null) {
                mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            }

            locationManager.requestLocationUpdates(provider, 5000, 0, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Cannot detect...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        populateExpandableListView();
        requestForGPSUpdates();
        if (mLocation != null){
            if (nearbyBusStops.isEmpty() && !nearbySwipe.isRefreshing()) {
                searchingProgress.setVisibility(View.VISIBLE);
                findNearby();
            }
        }else{
            tvNearbyError.setVisibility(View.VISIBLE);
            lvNearby.setVisibility(View.INVISIBLE);
            nearbySwipe.setRefreshing(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Toast.makeText(getContext(), "onPause called", Toast.LENGTH_SHORT).show();
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListener);
        }
        if (locationManager == null){
            Toast.makeText(getContext(), "locationManager is null", Toast.LENGTH_SHORT).show();
        }
    }

    private final android.location.LocationListener locationListener = new android.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
}
