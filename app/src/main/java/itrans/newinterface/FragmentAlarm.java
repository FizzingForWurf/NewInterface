package itrans.newinterface;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class FragmentAlarm extends Fragment implements OnMapReadyCallback{

    private MapView mapView;
    private GoogleMap map;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;


    public FragmentAlarm() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentAlarm.
     */
    public static FragmentAlarm newInstance(String param1, String param2) {
        FragmentAlarm fragment = new FragmentAlarm();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarm, container, false);

        mapView = (MapView) v.findViewById(R.id.fragmentMap);
        mapView.onCreate(savedInstanceState);

        mapView.onResume();// needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        return v;
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
