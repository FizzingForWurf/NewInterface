package itrans.newinterface.Alarm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import itrans.newinterface.BusNumberDBAdapter;
import itrans.newinterface.R;

public class FragmentAlarm extends Fragment implements OnMapReadyCallback{

    private MapView mapView;
    private GoogleMap map;

    private boolean isViewShown = true;

    public FragmentAlarm() {
        // Required empty public constructor
    }

    public static FragmentAlarm newInstance(String param1, String param2) {
        return new FragmentAlarm();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarm, container, false);

        mapView = (MapView) v.findViewById(R.id.fragmentMap);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getView() != null){
            isViewShown = true;
        }else{
            isViewShown = false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (map.getCameraPosition().zoom >= 15){
                    Thread thread = new Thread(){
                        public void run(){
                            final ArrayList<LatLng> busStops = new ArrayList<LatLng>();
                            BusNumberDBAdapter db = new BusNumberDBAdapter(getContext());
                            db.open();
                            int numOfRows = db.getNumberOfRows();
                            for (int a = 0; a < numOfRows; a++){
                                String latlng = db.getCoordinates(a + 1);
                                String noBrace = latlng.substring(latlng.indexOf("(") + 1, latlng.indexOf(")"));
                                String[] latlong = noBrace.split( ", ");
                                LatLng busStop = new LatLng(Double.parseDouble(latlong[0]), Double.parseDouble(latlong[1]));
                                busStops.add(busStop);
                            }
                            db.close();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < busStops.size(); i++){
                                        map.addMarker(new MarkerOptions().position(busStops.get(i)));
                                    }
                                }
                            });
                        }
                    };
                    thread.start();
                }else{
                    map.clear();
                }
            }
        });
    }
}
