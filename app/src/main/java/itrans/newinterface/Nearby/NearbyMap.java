package itrans.newinterface.Nearby;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import itrans.newinterface.BusNumberDBAdapter;
import itrans.newinterface.BusStopContainerDBAdapter;
import itrans.newinterface.Internet.VolleySingleton;
import itrans.newinterface.R;

public class NearbyMap extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LocationManager locationManager;
    private Location mLocation;
    private LatLng mLatLng;
    private Marker selectedMarker;

    private FloatingSearchView mSearchView;
    private List<NearbySuggestions> mSuggestionsList = new ArrayList<>();
    private List<NearbySuggestions> mResultsList = new ArrayList<>();
    private FloatingActionButton nearbyFab;

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
                                        if (selectedMarker != null){
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

    private int findPositionInDatabase(LatLng location) {
        ArrayList<String> latitudinalArray = new ArrayList<>();
        ArrayList<String> longitudinalArray = new ArrayList<>();


        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String latitudinalString = myPrefs.getString("latitudinalArray", "RIP");
        String longitudinalString = myPrefs.getString("longitudinalArray", "RIP");

        String noBraces1 = latitudinalString.substring(latitudinalString.indexOf("[") + 1, latitudinalString.indexOf("]"));
        String[] split1 = noBraces1.split(", ");
        for (String a : split1) {
            latitudinalArray.add(a);
            System.out.println(a);
        }

        String noBraces2 = longitudinalString.substring(longitudinalString.indexOf("[") + 1, longitudinalString.indexOf("]"));
        String[] split2 = noBraces2.split(", ");
        for (String a : split2) {
            longitudinalArray.add(a);
            System.out.println(a);
        }

        int positionInDatabase = -1;
        for (int y = 0; y < latitudinalArray.size(); y++) {
            String latString = latitudinalArray.get(y);
            double lat = Double.valueOf(latString);
            if (lat <= location.latitude) {
                positionInDatabase = y * 14;
                for (int u = 0; u < longitudinalArray.size(); u++) {
                    String longString = longitudinalArray.get(u);
                    double lon = Double.valueOf(longString);
                    if (lon >= location.longitude) {
                        positionInDatabase = positionInDatabase - (14 - u);
                        break;
                    }
                }
                break;
            }
        }
        return positionInDatabase;
    }

    private void addNearbyStopMarkers(){
        final ArrayList<String> busStopsIdArray = new ArrayList<String>();

        LatLngBounds curScreen = map.getProjection().getVisibleRegion().latLngBounds;
        final LatLng southwest = curScreen.southwest;
        final LatLng northeast = curScreen.northeast;
        map.clear();
        Thread thread = new Thread() {
            public void run() {
                int southwestPosition = findPositionInDatabase(southwest);
                int northeastPosition = findPositionInDatabase(northeast);

                BusStopContainerDBAdapter db = new BusStopContainerDBAdapter(getApplicationContext());
                db.open();
                if (southwestPosition == northeastPosition && southwestPosition != -1) {
                    String busStops = db.getInitialBusStops(southwestPosition);
                    String[] split = busStops.split(",");
                    for (String a : split) {
                        busStopsIdArray.add(a);
                        System.out.println(a);
                    }
                } else {
                    if (southwestPosition != -1) {
                        String busStops = db.getInitialBusStops(southwestPosition);
                        String[] split = busStops.split(",");
                        for (String a : split) {
                            busStopsIdArray.add(a);
                            System.out.println(a);
                        }

                        String busStops1 = db.getInitialBusStops(southwestPosition + 1);
                        String[] split1 = busStops1.split(",");
                        for (String a : split1) {
                            busStopsIdArray.add(a);
                            System.out.println(a);
                        }
                    }

                    if (northeastPosition != -1) {
                        String busStops = db.getInitialBusStops(northeastPosition);
                        String[] split = busStops.split(",");
                        for (String a : split) {
                            busStopsIdArray.add(a);
                            System.out.println(a);
                        }

                        String busStops1 = db.getInitialBusStops(northeastPosition - 1);
                        String[] split1 = busStops1.split(",");
                        for (String a : split1) {
                            busStopsIdArray.add(a);
                            System.out.println(a);
                        }
                    }
                }
                db.close();

                BusNumberDBAdapter db1 = new BusNumberDBAdapter(getApplicationContext());
                db1.open();
                for (int e = 0; e < busStopsIdArray.size(); e++) {
                    String stopID = busStopsIdArray.get(e);
                    char c = stopID.charAt(0);
                    if (!(c >= 'A' && c <= 'Z')) {
                        Log.e("BUS Number Test", stopID);
                        String stopLatLngString = db1.getBusStopLatLng(stopID);
                        final String stopName = db1.getBusStopName(stopID);
                        if (stopID.length() < 5) {
                            stopID = "0" + stopID;
                        }
                        Log.e("LATLNG", stopID + ", " + stopLatLngString + ", " + stopName);

                        String withoutBraces = stopLatLngString.substring(stopLatLngString.indexOf("(") + 1, stopLatLngString.indexOf(")"));
                        String[] split = withoutBraces.split(",");
                        Double lat = Double.parseDouble(split[0]);
                        Double lon = Double.parseDouble(split[1]);
                        final LatLng coordinates = new LatLng(lat, lon);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (map != null) {
                                    int height = 45;
                                    int width = 45;
                                    BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.bus_stop_marker);
                                    Bitmap b = bitmapdraw.getBitmap();
                                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

                                    map.addMarker(new MarkerOptions()
                                            .position(coordinates)
                                            .title(stopName)
                                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                                }
                            }
                        });
                    }
                }
                db1.close();
            }
        };
        thread.start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        nearbyFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float zoom = map.getCameraPosition().zoom;
                if (mLocation != null) {
                    if (selectedMarker != null){
                        selectedMarker.remove();
                    }

                    LatLng currLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                    if (zoom >= 16) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(currLatLng));
                    } else {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLatLng, 16));
                    }
                } else {
                    //Location cannot be found
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { //gps is not enabled
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
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
                    } else {
                        if (mLocation != null) {
                            requestForGPSUpdates();
                        }
                        Toast.makeText(NearbyMap.this, "Location cannot be detected.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (map.getCameraPosition().zoom >= 16) {
                    addNearbyStopMarkers();
                } else {
                    map.clear();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestForGPSUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListener);
        }
    }

    private void requestForGPSUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

            locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Cannot detect...", Toast.LENGTH_SHORT).show();
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
