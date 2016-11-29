package itrans.newinterface.Nearby;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import itrans.newinterface.Bookmarks.BusStopBookmarks;
import itrans.newinterface.Internet.VolleySingleton;
import itrans.newinterface.R;

public class FragmentNearby extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        AbsListView.OnScrollListener, ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener,
        NearbyExpandListAdapter.NearbyAdapterInterface {
    public static final int GPS_REQUEST_CODE = 50;

    private NearbyExpandListAdapter adapter;
    private ArrayList<NearbyBusStops> nearbyBusStops = new ArrayList<NearbyBusStops>();
    private ArrayList<NearbyBusTimings> arrivalTimings = new ArrayList<NearbyBusTimings>();
    private ArrayList<Integer> distanceArray = new ArrayList<>();

    private ProgressBar searchingProgress;
    private AnimatedExpandableListView lvNearby;
    private TextView tvNearbyError;
    private TextView tvNearbyNoResults;
    private SwipeRefreshLayout nearbySwipe;
    private CoordinatorLayout coordinatorLayout;
    private Snackbar snackbar;

    private Location mLocation;
    private LatLng mLatLng;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private boolean stopFindingNearby = false;
    private boolean isViewShown = true;
    private boolean refreshDontExpand = false;

    public FragmentNearby() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static FragmentNearby newInstance() {
        return new FragmentNearby();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_nearby, container, false);
        searchingProgress = (ProgressBar) v.findViewById(R.id.searchingProgress);
        lvNearby = (AnimatedExpandableListView) v.findViewById(R.id.lvNearby);
        nearbySwipe = (SwipeRefreshLayout) v.findViewById(R.id.nearbySwipe);
        tvNearbyError = (TextView) v.findViewById(R.id.tvNearbyError);
        tvNearbyNoResults = (TextView) v.findViewById(R.id.tvNearbyNoResults);
        coordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.nearbyParent);

        tvNearbyNoResults.setVisibility(View.INVISIBLE);
        tvNearbyError.setVisibility(View.INVISIBLE);
        searchingProgress.setVisibility(View.INVISIBLE);
        lvNearby.setVisibility(View.VISIBLE);

        nearbySwipe.setOnRefreshListener(this);
        lvNearby.setOnScrollListener(this);
        lvNearby.setOnGroupClickListener(this);
        lvNearby.setOnChildClickListener(this);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();
        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            if (getView() != null) { //beside fragment
                isViewShown = true;
                if (nearbyBusStops.isEmpty() && !nearbySwipe.isRefreshing()) {
                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }
                    if (!mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.connect();
                    }
                }
            } else {
                isViewShown = false;
            }
        }

        if (!isVisibleToUser) {
            if (snackbar != null) {
                snackbar.dismiss();
            }

            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    stopLocationUpdates();
                    mGoogleApiClient.disconnect();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        stopFindingNearby = true;
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
                mGoogleApiClient.disconnect();
            }
        }
        super.onDestroyView();
    }

    private boolean hasLocationPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
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
    public void onRefresh() {
        //check whether gps is enabled
        if (hasLocationPermissions()) {
            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    checkGPSEnabled();
                } else {
                    mGoogleApiClient.connect();
                }
            } else {
                buildGoogleApiClient();
                mGoogleApiClient.connect();
            }
        }
    }


    @Override
    public void onResume() {
        if (hasLocationPermissions()) {
            nearbySwipe.setEnabled(true);
            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("LOCATIONPERMISSION", true);
            editor.apply();

            if (nearbyBusStops != null) {
                adapter = new NearbyExpandListAdapter(getContext(), this);
                adapter.setData(nearbyBusStops);
                lvNearby.setAdapter(adapter);
            }

            if (!isViewShown) {
                if (nearbyBusStops.isEmpty() && !nearbySwipe.isRefreshing()) {
                    if (mGoogleApiClient != null) {
                        if (mGoogleApiClient.isConnected()) {
                            final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                startLocationUpdates();
                                if (mLocation == null) {
                                    tvNearbyError.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            mGoogleApiClient.connect();
                        }
                    } else {
                        buildGoogleApiClient();
                        mGoogleApiClient.connect();
                    }
                }
            }

            Log.e("LOCATIONREQUEST", String.valueOf(prefs.getBoolean("LOCATION REQUEST", false)));
            if (prefs.getBoolean("LOCATION REQUEST", false)) {
                Log.e("LOCATIONACCEPTED", String.valueOf(prefs.getBoolean("LOCATION ACCEPTED", false)));
                if (prefs.getBoolean("LOCATION ACCEPTED", false)) {
                    if (nearbyBusStops.isEmpty() && !nearbySwipe.isRefreshing()) {
                        if (mGoogleApiClient != null) {
                            if (mGoogleApiClient.isConnected()) {
                                final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    startLocationUpdates();
                                    if (mLocation == null) {
                                        tvNearbyError.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                mGoogleApiClient.connect();
                            }
                        } else {
                            buildGoogleApiClient();
                            mGoogleApiClient.connect();
                        }
                    }
                } else {
                    tvNearbyError.setVisibility(View.VISIBLE);
                    lvNearby.setVisibility(View.INVISIBLE);
                    searchingProgress.setVisibility(View.INVISIBLE);
                    nearbySwipe.setEnabled(true);
                    nearbySwipe.setRefreshing(false);
                }
                editor.putBoolean("LOCATION REQUEST", false);
                editor.apply();
            }
        } else {
            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            boolean hasCompletedSetUp = prefs.getBoolean("LOCATIONPERMISSION", true);
            if (!hasCompletedSetUp) {
                nearbySwipe.setEnabled(false);
                snackbar = Snackbar
                        .make(coordinatorLayout, "Allow location access in permission settings to find nearby bus stops. ", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                snackbar.show();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
                mGoogleApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        }

        createLocationRequest();
        if (hasLocationPermissions()) {
            checkGPSEnabled();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void checkGPSEnabled() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates test = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        if (mGoogleApiClient != null) {
                            if (mGoogleApiClient.isConnected()) {
                                startLocationUpdates();
                            }
                        }
                        if (mLocation != null) {
                            if (nearbyBusStops != null) {
                                if (nearbyBusStops.isEmpty() && !nearbySwipe.isRefreshing()) { //this means first enter view
                                    searchingProgress.setVisibility(View.VISIBLE);
                                    tvNearbyError.setVisibility(View.INVISIBLE);
                                    lvNearby.setVisibility(View.INVISIBLE);
                                    nearbySwipe.setEnabled(false);
                                    stopFindingNearby = false;
                                    findNearby();
                                    Toast.makeText(getContext(), "Finding...", Toast.LENGTH_SHORT).show();
                                } else {
                                    tvNearbyError.setVisibility(View.INVISIBLE);
                                    searchingProgress.setVisibility(View.INVISIBLE);
                                    lvNearby.setVisibility(View.INVISIBLE);
                                    stopFindingNearby = false;
                                    findNearby();
                                    Toast.makeText(getContext(), "Finding...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            tvNearbyError.setVisibility(View.VISIBLE);
                            lvNearby.setVisibility(View.INVISIBLE);
                            searchingProgress.setVisibility(View.INVISIBLE);
                            nearbySwipe.setRefreshing(false);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("LOCATION REQUEST", true);
                            editor.apply();

                            nearbySwipe.setRefreshing(false);
                            status.startResolutionForResult(getActivity(), GPS_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
    }

    protected void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (!parent.isGroupExpanded(groupPosition)) {
            //Expanded group
            nearbyBusStops.get(groupPosition).setProximity(-100);
            adapter.notifyDataSetChanged();
            String busStopId = nearbyBusStops.get(groupPosition).getBusStopID();
            findBusArrivalTimings(groupPosition, busStopId);
        } else {
            //Collapsed group
            lvNearby.collapseGroupWithAnimation(groupPosition);
        }
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, final int groupPosition,
                                int childPosition, long id) {
        ArrayList<NearbyBusTimings> chList = nearbyBusStops.get(groupPosition).getArrivalTimings();
        if (chList.size() <= childPosition) {
            Toast.makeText(getContext(), "LAST ITEM CLICKED", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void findNearby() {
        tvNearbyNoResults.setVisibility(View.INVISIBLE);
        tvNearbyError.setVisibility(View.INVISIBLE);
        distanceArray.clear();
        Thread thread = new Thread() {
            public void run() {
                nearbyBusStops.clear();

                InputStream busData = getResources().openRawResource(R.raw.bus_stop_data);
                Scanner scBusData = new Scanner(busData);
                int counter = 1;
                String name = "";
                String road = "";
                String id = "";
                BusStopBookmarks db = new BusStopBookmarks(getContext());
                db.open();
                String busStopString = db.getData(2);
                while (scBusData.hasNextLine()) {
                    if (!stopFindingNearby) {
                        String data = scBusData.nextLine();
                        if (data.equals("")) {
                            counter = 1;
                            name = "";
                            road = "";
                            id = "";
                        } else {
                            switch (counter) {
                                case 1: //bus stop id
                                    if (data.length() < 5) {
                                        data = "0" + data;
                                    }
                                    id = data;
                                    break;
                                case 2: //bus stop name
                                    name = data;
                                    break;
                                case 3: //bus stop road
                                    road = data;
                                    break;
                                case 4: //coordinates
                                    String withoutBraces = data.substring(data.indexOf("(") + 1, data.indexOf(")"));
                                    String[] latlong = withoutBraces.split(",");
                                    double latitude = Double.parseDouble(latlong[0]);
                                    double longitude = Double.parseDouble(latlong[1]);
                                    Location busStop = new Location("BUS Stop LOCATION");
                                    busStop.setLatitude(latitude);
                                    busStop.setLongitude(longitude);

                                    double latDifference;
                                    double lonDifference;
                                    if (latitude > mLocation.getLatitude()) {
                                        latDifference = latitude - mLocation.getLatitude();
                                    } else {
                                        latDifference = mLocation.getLatitude() - latitude;
                                    }

                                    if (longitude > mLocation.getLongitude()) {
                                        lonDifference = longitude - mLocation.getLongitude();
                                    } else {
                                        lonDifference = mLocation.getLongitude() - longitude;
                                    }
                                    if (latDifference + lonDifference <= 0.01202981126) {
                                        if (!name.equals("Non Stop")) {
                                            NearbyBusStops nearbyBusStops1 = new NearbyBusStops();
                                            nearbyBusStops1.setBusStopID(id);
                                            nearbyBusStops1.setBusStopName(name);
                                            nearbyBusStops1.setBusStopRoad(road);
                                            nearbyBusStops1.setProximity((int) mLocation.distanceTo(busStop));
                                            nearbyBusStops1.setArrivalTimings(new ArrayList<NearbyBusTimings>());

                                            if (!busStopString.equals("EMPTY")) {
                                                String[] split = busStopString.split(", ");
                                                for (String a : split) {
                                                    if (a.equals(id)) {
                                                        nearbyBusStops1.setChecked(true);
                                                    } else {
                                                        nearbyBusStops1.setChecked(false);
                                                    }
                                                }
                                            }

                                            nearbyBusStops.add(nearbyBusStops1);
                                            distanceArray.add((int) mLocation.distanceTo(busStop));
                                        }
                                    }
                                    break;
                            }
                            counter++;
                        }
                    }
                }
                db.close();
                Log.e("NEARBY", String.valueOf(nearbyBusStops.size()));
                Collections.sort(distanceArray);

                if (!stopFindingNearby) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            nearbySwipe.setEnabled(true);
                            nearbySwipe.setRefreshing(false);

                            Collections.sort(nearbyBusStops, new Comparator<NearbyBusStops>() {
                                @Override
                                public int compare(NearbyBusStops nearbyBusStops, NearbyBusStops t1) {
                                    return nearbyBusStops.getProximity() - t1.getProximity();
                                }
                            });
                            adapter = new NearbyExpandListAdapter(getContext(), FragmentNearby.this);
                            adapter.setData(nearbyBusStops);
                            lvNearby.setAdapter(adapter);

                            tvNearbyError.setVisibility(View.INVISIBLE);

                            if (lvNearby.getCount() > 0) {
                                tvNearbyNoResults.setVisibility(View.INVISIBLE);

                                lvNearby.setAlpha(0f);
                                lvNearby.setVisibility(View.VISIBLE);
                                lvNearby.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .setListener(null);

                                searchingProgress.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                searchingProgress.setVisibility(View.INVISIBLE);
                                            }
                                        });
                            } else {
                                tvNearbyNoResults.setAlpha(0f);
                                tvNearbyNoResults.setVisibility(View.VISIBLE);
                                tvNearbyNoResults.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .setListener(null);

                                searchingProgress.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                searchingProgress.setVisibility(View.INVISIBLE);
                                            }
                                        });
                            }
                            lvNearby.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        };
        thread.start();
    }

    private void findBusArrivalTimings(final int groupPosition, String busStopId) {
        arrivalTimings = new ArrayList<NearbyBusTimings>();
        JsonObjectRequest BusStopRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + busStopId + "&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String eta;
                        String load;
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                String inService = services.getString("Status");
                                switch (inService) {
                                    case "In Operation":
                                        JSONObject nextBus = services.getJSONObject("NextBus");
                                        eta = nextBus.getString("EstimatedArrival");
                                        load = nextBus.getString("Load");
                                        //String wheelC = nextBus.getString("Feature");
                                        break;
                                    case "Not In Operation":
                                        eta = "Not in Operation";
                                        load = "";
                                        break;
                                    default:
                                        eta = "No data available";
                                        load = "";
                                        break;
                                }
                                NearbyBusTimings timings = new NearbyBusTimings();
                                timings.setBusService(busNo);
                                timings.setBusLoad(load);
                                if (eta != null) {
                                    timings.setBusTiming(eta);
                                } else {
                                    timings.setBusTiming("Not available");
                                }
                                arrivalTimings.add(timings);
                            }
                            nearbyBusStops.get(groupPosition).setArrivalTimings(arrivalTimings);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                        nearbyBusStops.get(groupPosition).setProximity(distanceArray.get(groupPosition));
                        adapter.notifyDataSetChanged();
                        if (!refreshDontExpand) {
                            lvNearby.expandGroupWithAnimation(groupPosition);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        nearbyBusStops.get(groupPosition).setProximity(distanceArray.get(groupPosition));
                        adapter.notifyDataSetChanged();

                        lvNearby.collapseGroupWithAnimation(groupPosition);

                        Toast.makeText(getContext(), "Please ensure that you have stable network connection and try again.", Toast.LENGTH_LONG).show();
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

    @Override
    public void refreshNearbyTimings(int groupPosition) {
        refreshDontExpand = true;

        String id = nearbyBusStops.get(groupPosition).getBusStopID();
        findBusArrivalTimings(groupPosition, id);
        refreshDontExpand = false;
    }
}
