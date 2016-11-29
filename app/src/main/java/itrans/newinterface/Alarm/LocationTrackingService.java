package itrans.newinterface.Alarm;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;

import itrans.newinterface.R;
import itrans.newinterface.Splash;

public class LocationTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean hasArrived = false;
    private boolean isNoticeActive = false;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private LatLng mLatLng;

    private String destinationLatLng;
    private String alertRadius;
    private String alertTitle;
    private Location selectedLocation;
    private float distance = 0;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder noticeBuilder;
    private int noticeID = 99;

    public LocationTrackingService() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alertRadius = intent.getStringExtra("AlertRadius");
        destinationLatLng = intent.getStringExtra("AlertDestination");
        alertTitle = intent.getStringExtra("AlertTitle");

        String[] latLng = destinationLatLng.split(",");
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);
        selectedLocation = new Location("SELECTED LOCATION");
        selectedLocation.setLatitude(latitude);
        selectedLocation.setLongitude(longitude);

        createNotification(round(distance / 1000, 2));

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }
        super.onCreate();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        createLocationRequest();
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(8000);
        mLocationRequest.setFastestInterval(4000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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

        if (destinationLatLng != null && alertRadius != null) {
            distance = location.distanceTo(selectedLocation);
            updateNotification(round(distance / 1000, 2));

            float radius = Float.parseFloat(alertRadius);
            hasArrived = (distance <= radius);
        }

        if (hasArrived) {
            hasArrived = false;
            alertRadius = null;

            SharedPreferences prefs = getSharedPreferences("ALARM RESTORE POSITION", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("ALARM RESTORE POSITION", -1);
            editor.apply();

            AlarmReceiver alarm = new AlarmReceiver();
            alarm.StartAlarm(this, alertTitle);
        }
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
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        deleteNotification();
        super.onDestroy();
    }

    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private void createNotification(BigDecimal distance) {
        isNoticeActive = true;
        noticeBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(alertTitle + " alarm")
                .setContentText("Distance left: " + distance + "km")
                .setTicker("Starting " + alertTitle + " alarm...")
                .setSmallIcon(R.drawable.ic_directions_bus_white_24dp)
                .setOngoing(true);

        Intent resultIntent = new Intent(this, Splash.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        noticeBuilder.setContentIntent(resultPendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(noticeID, noticeBuilder.build());
    }

    private void updateNotification(BigDecimal distance) {
        if (isNoticeActive) {
            if (distance != null) {
                noticeBuilder.setContentText("Distance left: " + distance + "km");
            } else {
                noticeBuilder.setContentText("Distance left: ");
            }
            notificationManager.notify(noticeID, noticeBuilder.build());
        }
    }

    private void deleteNotification() {
        notificationManager.cancel(noticeID);
        isNoticeActive = false;
    }
}
