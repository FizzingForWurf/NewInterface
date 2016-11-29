package itrans.newinterface.Nearby;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import itrans.newinterface.BusNumberDBAdapter;
import itrans.newinterface.Internet.VolleySingleton;
import itrans.newinterface.R;

public class NearbyMap extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private LatLng mLatLng;
    private Marker selectedMarker;
    private ArrayList<Integer> visibleBusStopsIndex = new ArrayList<>();
    private ArrayList<LatLng> visibleBusStops = new ArrayList<>();

    private FloatingSearchView mSearchView;
    private List<NearbySuggestions> mSuggestionsList = new ArrayList<>();
    private List<NearbySuggestions> mResultsList = new ArrayList<>();
    private FloatingActionButton nearbyFab;

    private boolean buttonZoom = false;
    private boolean startAddingMarkers = false;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.nearbyMap);
        mapFragment.getMapAsync(this);

        mSearchView = (FloatingSearchView) findViewById(R.id.floating_search_view);
        nearbyFab = (FloatingActionButton) findViewById(R.id.nearbyFab);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {
                NearbyMap.this.finish();
            }
        });

        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                //query = newQuery;
                if (!mSuggestionsList.isEmpty()) {
                    mSuggestionsList.clear();
                }
                if (!mResultsList.isEmpty()) {
                    mResultsList.clear();
                }
                String hi = Integer.toString(mSuggestionsList.size());
                Log.i("AUTOCOMPLETE CLEAR", hi);
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {
                    mSearchView.showProgress();
                    String url = getPlaceAutoCompleteUrl(newQuery);
                    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET, url, null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    String secondaryName;
                                    JSONArray ja;
                                    try {
                                        ja = response.getJSONArray("predictions");
                                        for (int i = 0; i < ja.length(); i++) {
                                            secondaryName = null;
                                            JSONObject c = ja.getJSONObject(i);
                                            String placeid = c.getString("place_id");
                                            Log.i("AUTOCOMPLETE", placeid);

                                            JSONArray description = c.getJSONArray("terms");
                                            JSONObject primaryDescription = description.getJSONObject(0);
                                            String primaryDescriptionName = primaryDescription.getString("value");
                                            Log.i("AUTOCOMPLETE", primaryDescriptionName);
                                            if (description.length() > 1) {
                                                for (int s = 1; s < description.length(); s++) {
                                                    JSONObject secondaryDescription = description.getJSONObject(s);
                                                    String secondaryDescriptionName = secondaryDescription.getString("value");
                                                    if (secondaryName == null) {
                                                        secondaryName = secondaryDescriptionName;
                                                    } else {
                                                        secondaryName = secondaryName + ", " + secondaryDescriptionName;
                                                    }
                                                }
                                            } else if (description.length() == 1) {
                                                secondaryName = primaryDescriptionName;
                                            }
                                            Log.i("AUTOCOMPLETE 2nd", secondaryName);

                                            NearbySuggestions nearbySuggestions = new NearbySuggestions();
                                            nearbySuggestions.setmPlaceID(placeid);
                                            nearbySuggestions.setmPlaceName(primaryDescriptionName);
                                            nearbySuggestions.setmSecondaryPlaceName(secondaryName);
                                            mResultsList.add(nearbySuggestions);

                                            String finalPlaceResult = "(" + primaryDescriptionName + ")+" + secondaryName + "=";
                                            mSuggestionsList.add(new NearbySuggestions(finalPlaceResult));
                                            String hi = Integer.toString(mSuggestionsList.size());
                                            Log.i("AUTOCOMPLETE TEST", hi);
                                        }
                                        String hi = Integer.toString(mSuggestionsList.size());
                                        Log.i("AUTOCOMPLE AFTER LOOP", hi);
                                        mSearchView.swapSuggestions(mSuggestionsList);
                                        mSearchView.hideProgress();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Snackbar.make(findViewById(R.id.parent_view),
                                    "Oh no! Something went wrong. Please ensure that you are connected to the internet.",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    });
                    requestQueue.add(jsonObjReq);
                }
            }
        });

        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView,
                                         SearchSuggestion item, int itemPosition) {
                leftIcon.setImageResource(R.drawable.ic_search_black_24dp);

                String texttest = textView.getText().toString();
                String primaryText1 = texttest.substring(texttest.indexOf("(") + 1, texttest.indexOf(")"));
                String primaryText = capitalisePhrase(primaryText1);

                String secondaryText1 = texttest.substring(texttest.indexOf("+") + 1, texttest.indexOf("="));
                String secondaryText = capitalisePhrase(secondaryText1);

                textView.setText(Html.fromHtml("<b>" + primaryText + "</b>" + "<br />" +
                        "<small>" + secondaryText + "</small>"));
            }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                if (map != null) {
                    map.clear();
                }

                String selectedPlace1 = searchSuggestion.getBody();
                String selectedPlace2 = selectedPlace1.substring(selectedPlace1.indexOf("(") + 1, selectedPlace1.indexOf(")"));
                final String selectedPlace = capitalisePhrase(selectedPlace2);

                int suggestionPosition = mSuggestionsList.indexOf(searchSuggestion);
                Collections.reverse(mResultsList);
                String placeId = mResultsList.get(suggestionPosition).getmPlaceID();

                String placeUrl = getPlaceDetailsUrl(placeId);
                JsonObjectRequest placeDetailsRequest = new JsonObjectRequest(Request.Method.GET, placeUrl, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONObject detailsJO;
                                try {
                                    mSearchView.setSearchText(selectedPlace);
                                    detailsJO = response.getJSONObject("result");
                                    String placeName = detailsJO.getString("name");
                                    JSONObject latlngObject = detailsJO.getJSONObject("geometry");
                                    JSONObject latlngLocation = latlngObject.getJSONObject("location");
                                    double placeLat = latlngLocation.getDouble("lat");
                                    double placeLng = latlngLocation.getDouble("lng");

                                    if (map != null) {
                                        if (selectedMarker != null) {
                                            selectedMarker.remove();
                                        }
                                        selectedMarker = map.addMarker(new MarkerOptions()
                                                .position(new LatLng(placeLat, placeLng))
                                                .title(placeName));

                                        float zoom = map.getCameraPosition().zoom;
                                        if (zoom >= 16) {
                                            map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(placeLat, placeLng)));
                                        } else {
                                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(placeLat, placeLng), 16));
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Snackbar.make(findViewById(R.id.parent_view),
                                "Oh no! Something went wrong. Please ensure that you are connected to the internet.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
                requestQueue.add(placeDetailsRequest);
            }

            @Override
            public void onSearchAction(String currentQuery) {

            }
        });
    }

    private String capitalisePhrase(String phrase) {
        String[] strArray = phrase.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap + " ");
        }
        return builder.toString();
    }

    private String getPlaceAutoCompleteUrl(String input) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/autocomplete/json");
        urlString.append("?input=");
        try {
            urlString.append(URLEncoder.encode(input, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&language=en");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        return urlString.toString();
    }

    private String getPlaceDetailsUrl(String placeid) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/details/json");
        urlString.append("?placeid=");
        try {
            urlString.append(URLEncoder.encode(placeid, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&language=en");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        return urlString.toString();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });

        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE ||
                        reason == GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
                    startAddingMarkers = true;
                }
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (startAddingMarkers) {
                    if (map.getCameraPosition().zoom >= 16) {
                        startAddingMarkers = false;
                        final LatLngBounds screen = map.getProjection().getVisibleRegion().latLngBounds;

                        visibleBusStops.clear();
                        visibleBusStopsIndex.clear();

                        Thread thread = new Thread() {
                            public void run() {
                                int counter = 1;
                                InputStream data = getResources().openRawResource(R.raw.bus_stop_coordinates);
                                Scanner sc = new Scanner(data);

                                while (sc.hasNextLine()) {
                                    String raw = sc.nextLine();
                                    String noBraces = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
                                    String[] split = noBraces.split(",");
                                    double latitude = Double.parseDouble(split[0]);
                                    double longitude = Double.parseDouble(split[1]);
                                    LatLng busStop = new LatLng(latitude, longitude);

                                    if (screen.contains(busStop)) {
                                        visibleBusStopsIndex.add(counter);
                                        visibleBusStops.add(busStop);
                                    }
                                    counter++;
                                }
                                Log.e("TEST", String.valueOf(visibleBusStops.size()));

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
                                        db.open();
                                        String title = null;
                                        LatLng selectedPosition = null;
                                        if (selectedMarker != null) {
                                            title = selectedMarker.getTitle();
                                            selectedPosition = selectedMarker.getPosition();
                                            selectedMarker.remove();
                                        }

                                        map.clear();

                                        if (title != null && selectedPosition != null) {
                                            selectedMarker = map
                                                    .addMarker(new MarkerOptions()
                                                            .position(selectedPosition).title(title));
                                        }

                                        int height = 45;
                                        int width = 45;
                                        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.bus_stop_marker);
                                        Bitmap b = bitmapdraw.getBitmap();
                                        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

                                        for (int i = 0; i < visibleBusStops.size(); i++) {
                                            int position = visibleBusStopsIndex.get(i);

                                            String id = db.getID(position);

                                            if (id.length() < 5) {
                                                id = "0" + id;
                                            }

                                            map.addMarker(new MarkerOptions()
                                                    .position(visibleBusStops.get(i))
                                                    .title(db.getName(position))
                                                    .snippet(id)
                                                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                                        }
                                        db.close();
                                    }
                                });
                            }
                        };
                        thread.start();
                    } else {
                        String title = null;
                        LatLng selectedPosition = null;
                        if (selectedMarker != null) {
                            title = selectedMarker.getTitle();
                            selectedPosition = selectedMarker.getPosition();
                            selectedMarker.remove();
                        }

                        map.clear();

                        if (title != null && selectedPosition != null) {
                            selectedMarker = map
                                    .addMarker(new MarkerOptions()
                                            .position(selectedPosition).title(title));
                        }
                    }
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
    }

    @Override
    protected void onStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        300);
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
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 300:
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                        if (showRationale) {
                            // user did NOT check "never ask again"
                            new AlertDialog.Builder(this)
                                    .setTitle("Denying permission")
                                    .setMessage("iTrans requires your location permission to track your position. Are you sure you want to deny permission?")
                                    .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                ActivityCompat.requestPermissions(NearbyMap.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                        300);
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
                            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.nearbyMapParent);
                            Snackbar snackbar = Snackbar
                                    .make(coordinatorLayout, "Grant location access in permission settings to find nearby bus stops. ", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Settings", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        }
                                    });
                            snackbar.show();
                        }
                    } else {
                        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.nearbyMapParent);
                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout, "Grant location access in permission settings to find nearby bus stops. ", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Settings", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                });
                        snackbar.show();
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        if (!nearbyFab.hasOnClickListeners()) {
            nearbyFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContextCompat.checkSelfPermission(NearbyMap.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            ActivityCompat.requestPermissions(NearbyMap.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    300);
                        }
                    } else {
                        buttonZoom = true;
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

                    if (selectedMarker != null) {
                        selectedMarker.remove();
                    }
                }
            });
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
                mGoogleApiClient.isConnected();
            }
        }
        super.onDestroy();
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

                                if (map != null) {
                                    if (ActivityCompat.checkSelfPermission(NearbyMap.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        return;
                                    }
                                    map.setMyLocationEnabled(true);
                                    map.getUiSettings().setCompassEnabled(true);
                                }
                            }
                        }

                        if (buttonZoom) {
                            if (map != null) {
                                if (mLocation != null) {
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
                                } else {
                                    Toast.makeText(NearbyMap.this, "Location not found.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            buttonZoom = false;
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            status.startResolutionForResult(NearbyMap.this, 98);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 98:
                switch (resultCode) {
                    case RESULT_OK:
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
                        break;
                    case RESULT_CANCELED:
                        buttonZoom = false;
                        break;
                }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        if (map != null) {
            map.setMyLocationEnabled(false);
            map.getUiSettings().setCompassEnabled(false);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        }

        createLocationRequest();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkGPSEnabled();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
    }
}
