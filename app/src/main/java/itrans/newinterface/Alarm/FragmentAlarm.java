package itrans.newinterface.Alarm;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import itrans.newinterface.R;

public class FragmentAlarm extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener, CustomAlarmAdapter.AdapterInterface {

    private MapView mapView;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private LatLng mLatLng;
    private Marker myLocationMarker;
    private Marker destinationMarker;
    private Circle rangeCircle;

    private TextView tvNoDest;
    private ListView lvDestinations;
    private CustomAlarmAdapter adapter;
    private ArrayList<Alarm> alarmArrayList = new ArrayList<>();

    private boolean reqPermsResume = false;
    private boolean isRestoreSwitchCalled = false;
    private boolean firstLoadLocation = true;

    private String alertLatLng;
    private String alertRadius;
    private String alertTitle;
    public int activeSwitchPosition = -1;

    public static final int GPS_REQUEST_CODE_ALARM = 100;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION_ALARM = 200;

    public FragmentAlarm() {
        // Required empty public constructor
    }

    public static FragmentAlarm newInstance() {
        return new FragmentAlarm();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarm, container, false);

        tvNoDest = (TextView) v.findViewById(R.id.tvNoDest);
        lvDestinations = (ListView) v.findViewById(R.id.lvDestinations);
        lvDestinations.setOnItemClickListener(null);
        lvDestinations.setOnScrollListener(this);
        registerForContextMenu(lvDestinations);

        mapView = (MapView) v.findViewById(R.id.fragmentMap);
        mapView.onCreate(savedInstanceState);
        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        if (reqPermsResume) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_FINE_LOCATION_ALARM);
                }
            } else {
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

        return v;
    }

    @Override
    public void onResume() { //whatever code here has to be surrounded by
        Log.e("TEST", "OnResume");
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            if (prefs.getBoolean("Bookmark Location ACCEPTED", false)) {
                checkGPSEnabled();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("Bookmark Location ACCEPTED", false);
                editor.apply();
            } else {
                activeSwitchPosition = -1;
            }
        }

        alarmArrayList.clear();
        AlarmDBAdapter db = new AlarmDBAdapter(getContext());
        db.open();
        for (int i = 0; i < db.getNumberOfRows(); i++) {
            Alarm alarm = new Alarm();
            alarm.setTitle(db.getTitle(i + 1));
            alarm.setDestination(db.getDestination(i + 1));
            alarm.setDistance(-1.0f);
            if (i == 0 && mLocation != null) {
                alarm.setCurrentLocation(mLocation);
            } else {
                alarm.setCurrentLocation(null);
            }

            alarmArrayList.add(alarm);
        }
        db.close();

        adapter = new CustomAlarmAdapter(getContext(), alarmArrayList, this);
        if (lvDestinations != null && tvNoDest != null) {
            lvDestinations.setAdapter(adapter);

            if (lvDestinations.getCount() > 0) {
                tvNoDest.setVisibility(View.GONE);
                lvDestinations.setVisibility(View.VISIBLE);
            } else {
                lvDestinations.setVisibility(View.GONE);
                tvNoDest.setVisibility(View.VISIBLE);
            }
        }

        //restore state
        SharedPreferences prefs = getActivity().getSharedPreferences("ALARM RESTORE POSITION", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putInt("ALARM RESTORE POSITION", -1);
//        editor.apply();
        if (prefs.getInt("ALARM RESTORE POSITION", -1) > -1) {
            SharedPreferences prefs1 = getActivity().getSharedPreferences("ONRESUME RESTORE", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs1.edit();
            editor.putBoolean("ONRESUME RESTORE", true);
            editor.apply();

            activeSwitchPosition = prefs.getInt("ALARM RESTORE POSITION", -1);
            isRestoreSwitchCalled = true;

            lvDestinations.smoothScrollToPositionFromTop(activeSwitchPosition, 100);

            AlarmDBAdapter db1 = new AlarmDBAdapter(getContext());
            db1.open();
            alertTitle = db1.getTitle(activeSwitchPosition + 1);
            alertLatLng = db1.getLatLng(activeSwitchPosition + 1);
            alertRadius = db1.getRadius(activeSwitchPosition + 1);
            db1.close();
        }
        super.onResume();
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
    public void onDestroyView() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
                mGoogleApiClient.disconnect();
            }
            if (map != null) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                map.setMyLocationEnabled(false);
                map.getUiSettings().setCompassEnabled(false);
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION_ALARM:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission accepted
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
                } else {
                    //permission denied
                    activeSwitchPosition = -1;
                    boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                    if (showRationale) {
                        // user did NOT check "never ask again"
                        new AlertDialog.Builder(getContext())
                                .setTitle("Denying permission")
                                .setMessage("iTrans requires your location permission to track your position. Are you sure you want to deny permission?")
                                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                    PERMISSIONS_REQUEST_FINE_LOCATION_ALARM);
                                        }
                                    }
                                })
                                .setNegativeButton("I'm sure", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        // user CHECKED "never ask again"
                        new AlertDialog.Builder(getContext())
                                .setTitle("Location permission")
                                .setMessage("iTrans requires your location permission to track your position. Please allow permission if you would like to use this feature.")
                                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            Log.e("TEST", "Visible");
            if (getView() != null) {
                reqPermsResume = false;
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST_FINE_LOCATION_ALARM);
                    }
                } else {
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
            } else {
                reqPermsResume = true;
            }
        }

        if (!isVisibleToUser) {
            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    stopLocationUpdates();
                    mGoogleApiClient.disconnect();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);

        if (mLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
        }

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (activeSwitchPosition > -1) {
                    if (mLatLng != null && alertLatLng != null) {
                        String[] latANDlong = alertLatLng.split(",");
                        double latitude = Double.parseDouble(latANDlong[0]);
                        double longitude = Double.parseDouble(latANDlong[1]);
                        LatLng selectedLocation = new LatLng(latitude, longitude);

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(mLatLng);
                        builder.include(selectedLocation);
                        LatLngBounds bounds = builder.build();
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 130));
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

            if (alarmArrayList != null) {
                if (!alarmArrayList.isEmpty()) {
                    alarmArrayList.get(0).setCurrentLocation(mLocation);
                }
            }
        }

        createLocationRequest();
        checkGPSEnabled();
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

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
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
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            status.startResolutionForResult(getActivity(), GPS_REQUEST_CODE_ALARM);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
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

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        if (!alarmArrayList.isEmpty()) {
            alarmArrayList.get(0).setCurrentLocation(location);
        }

        if (activeSwitchPosition > -1) {
            String[] latANDlong = alertLatLng.split(",");
            double latitude = Double.parseDouble(latANDlong[0]);
            double longitude = Double.parseDouble(latANDlong[1]);
            LatLng latLng = new LatLng(latitude, longitude);
            Location destination = new Location("DESTINATION");
            destination.setLatitude(latitude);
            destination.setLongitude(longitude);

            float result = (location.distanceTo(destination)) / 1000;
            alarmArrayList.get(activeSwitchPosition).setDistance(result);
            adapter.notifyDataSetChanged();

            if (isRestoreSwitchCalled) {
                alarmArrayList.get(activeSwitchPosition).setChecked(true);
                adapter.notifyDataSetChanged();

                if (map != null) {
                    destinationMarker = map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(alertTitle));
                    rangeCircle = map.addCircle(new CircleOptions()
                            .center(latLng)
                            .fillColor(0x550000FF)
                            .strokeColor(Color.BLUE)
                            .strokeWidth(10.0f)
                            .radius(Double.parseDouble(alertRadius)));
                }
                isRestoreSwitchCalled = false;
            }
        }

        if (map != null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(mLatLng);
            markerOptions.title("You are here");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker());

            if (myLocationMarker != null) {
                myLocationMarker.remove();
            }
            myLocationMarker = map.addMarker(markerOptions);

            if (firstLoadLocation) {
                if (mLatLng != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
                }
                firstLoadLocation = false;
            }
        }
    }

    protected void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        if (map != null) {
            map.setMyLocationEnabled(false);
            map.getUiSettings().setCompassEnabled(false);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.delete_edit_alarm_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editAlarmMenu:
                AdapterView.AdapterContextMenuInfo information = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                AlarmDBAdapter db1 = new AlarmDBAdapter(getContext());
                db1.open();
                String updateTitle = db1.getTitle(information.position + 1);
                String updateDestination = db1.getDestination(information.position + 1);
                String updateLatLng = db1.getLatLng(information.position + 1);
                String updateRadius = db1.getRadius(information.position + 1);
                db1.close();
                Intent i = new Intent(getContext(), AddDestination.class);
                i.putExtra("updateRowNumber", information.position + 1);
                i.putExtra("updateTitle", updateTitle);
                i.putExtra("updateDestination", updateDestination);
                i.putExtra("updateLatLng", updateLatLng);
                i.putExtra("updateRadius", updateRadius);
                startActivity(i);
                break;
            case R.id.deleteAlarmMenu:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                final int positionOfDeletedEntry = info.position;
                if (positionOfDeletedEntry != activeSwitchPosition) {
                    AlarmDBAdapter db = new AlarmDBAdapter(getContext());
                    db.open();
                    Log.e("DELETE POSITION", String.valueOf(positionOfDeletedEntry));
                    int lastEntryNumber = db.getNumberOfRows();
                    Log.e("DELETE ACTUAL", String.valueOf(lastEntryNumber));
                    int numberOfEntriesAfterDeletedEntry = lastEntryNumber - (positionOfDeletedEntry + 1);
                    db.deleteEntry(positionOfDeletedEntry + 1);
                    int testing = db.getNumberOfRows();
                    Log.e("DELETE TEST", String.valueOf(testing));

                    for (int w = 0; w < numberOfEntriesAfterDeletedEntry; w++) {
                        int positionOfEntryNeededToBeChanged = w + positionOfDeletedEntry + 2;

                        db.updateUniqueId(positionOfEntryNeededToBeChanged);
                        Log.e("DELETE W", String.valueOf(positionOfEntryNeededToBeChanged));
                    }

                    //check row sequence for debugging
                    ArrayList<String> arrayList;
                    arrayList = db.getIdList();
                    Log.e("DELETE ARRAY", String.valueOf(arrayList));

                    final View view = lvDestinations.getChildAt(positionOfDeletedEntry);
                    view.animate().setDuration(400).alpha(0).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            alarmArrayList.remove(positionOfDeletedEntry);
                            adapter.notifyDataSetChanged();
                            view.setAlpha(1);
                        }
                    });

                    if (lvDestinations.getCount() > 0) {
                        tvNoDest.setVisibility(View.GONE);
                        lvDestinations.setVisibility(View.VISIBLE);
                    } else {
                        lvDestinations.setVisibility(View.GONE);
                        tvNoDest.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(getContext(), "Entry deleted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please turn off active alarm to delete entry.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onContextItemSelected(item);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void switchActivate(Float distance) {
        //Set distance in textview, start location tracking service, draw circle and marker on map
        SharedPreferences prefs = getActivity().getSharedPreferences("ALARM RESTORE POSITION", Context.MODE_PRIVATE);
        activeSwitchPosition = prefs.getInt("ALARM RESTORE POSITION", -1);

        alarmArrayList.get(activeSwitchPosition).setDistance(distance / 1000);
        adapter.notifyDataSetChanged();

        AlarmDBAdapter db = new AlarmDBAdapter(getContext());
        db.open();
        alertTitle = db.getTitle(activeSwitchPosition + 1);
        alertLatLng = db.getLatLng(activeSwitchPosition + 1);
        alertRadius = db.getRadius(activeSwitchPosition + 1);
        db.close();

        String[] latLng = alertLatLng.split(",");
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);
        LatLng latlng = new LatLng(latitude, longitude);

        if (map != null) {
            if (mLocation != null) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(mLatLng);
                builder.include(latlng);
                LatLngBounds bounds = builder.build();
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 130));
            }

            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title(alertTitle));
            rangeCircle = map.addCircle(new CircleOptions()
                    .center(latlng)
                    .fillColor(0x550000FF)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(10.0f)
                    .radius(Double.parseDouble(alertRadius)));
        }

        if (!isServiceRunning(LocationTrackingService.class)) {
            Intent serviceIntent = new Intent(getContext(), LocationTrackingService.class);
            serviceIntent.putExtra("AlertRadius", alertRadius);
            serviceIntent.putExtra("AlertDestination", alertLatLng);
            serviceIntent.putExtra("AlertTitle", alertTitle);
            getActivity().startService(serviceIntent);
        }
    }

    @Override
    public void disableSwitch(boolean isNearbyAlready, int position) {
        SharedPreferences prefs = getActivity().getSharedPreferences("ALARM RESTORE POSITION", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("ALARM RESTORE POSITION", -1);
        editor.apply();

        alarmArrayList.get(position).setDistance(-1.0f);
        adapter.notifyDataSetChanged();

        activeSwitchPosition = -1;

        alertTitle = null;
        alertRadius = null;
        alertLatLng = null;

        if (map != null && mLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
        }

        if (isNearbyAlready) {
            Toast.makeText(getContext(), "You are already near your destination!", Toast.LENGTH_SHORT).show();
        } else {
            //The switch is already checked, so we need to uncheck the switch. We need to reset the map, stop the location
            //tracking service and reset the distance in item.
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            if (rangeCircle != null) {
                rangeCircle.remove();
            }

            if (isServiceRunning(LocationTrackingService.class)) {
                getActivity().stopService(new Intent(getContext(), LocationTrackingService.class));
            }
        }
    }

    @Override
    public void checkPermissionAndLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_FINE_LOCATION_ALARM);
            }
        } else {
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
}