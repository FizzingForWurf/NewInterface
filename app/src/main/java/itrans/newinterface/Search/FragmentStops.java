package itrans.newinterface.Search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import itrans.newinterface.Bookmarks.BusStopBookmarks;
import itrans.newinterface.Internet.VolleySingleton;
import itrans.newinterface.Nearby.AnimatedExpandableListView;
import itrans.newinterface.Nearby.NearbyBusTimings;
import itrans.newinterface.R;

public class FragmentStops extends Fragment implements ExpandableListView.OnGroupClickListener,
        SearchBusStopsAdapter.SearchStopsAdapterInterface {

    private String query;

    private ArrayList<BusStopsFound> busStopsFound = new ArrayList<BusStopsFound>();
    private ArrayList<NearbyBusTimings> arrivalTimings = new ArrayList<NearbyBusTimings>();
    private SearchBusStopsAdapter searchBusStopsAdapter;
    private AnimatedExpandableListView lvSearchStops;

    private TextView tvNoResults1;
    private ProgressBar searchStopsProgress;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private boolean refreshDontExpand = false;

    public FragmentStops() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        query = getArguments().getString("stopsQuery");

        View v = inflater.inflate(R.layout.fragment_search_stops, container, false);
        lvSearchStops = (AnimatedExpandableListView) v.findViewById(R.id.lvSearchStops);
        tvNoResults1 = (TextView) v.findViewById(R.id.tvNoResults1);
        searchStopsProgress = (ProgressBar) v.findViewById(R.id.searchStopsProgress);

        searchStopsProgress.setVisibility(View.VISIBLE);
        tvNoResults1.setVisibility(View.GONE);
        lvSearchStops.setVisibility(View.GONE);
        lvSearchStops.setOnGroupClickListener(this);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        searchForBusStops();
        return v;
    }

    private void searchForBusStops() {
        Thread thread = new Thread() {
            public void run() {
                InputStream data = getResources().openRawResource(R.raw.bus_stop_data_compress);
                Scanner sc = new Scanner(data);

                busStopsFound.clear();
                StringBuilder entry = new StringBuilder();

                BusStopBookmarks db = new BusStopBookmarks(getContext());
                db.open();
                String busStopString = db.getData(2);
                while (sc.hasNextLine()) {
                    String busStopData = sc.nextLine();
                    if (busStopData.equals("")) {
                        String[] split = entry.toString().split("[\\r\\n]");
                        String id = split[0];
                        String name = split[1];
                        String road = split[2];

                        if (id.length() < 5) {
                            id = "0" + id;
                        }

                        if (id.toLowerCase().contains(query)) {
                            if (!name.equals("Non Stop") && !name.equals("Non-Stop")) {
                                BusStopsFound busStop = new BusStopsFound();
                                busStop.setId(id);
                                busStop.setName(name);
                                busStop.setRoad(road);
                                busStop.setQuery("");
                                busStop.setTimings(new ArrayList<NearbyBusTimings>());
                                if (!busStopString.equals("EMPTY")) {
                                    String[] split1 = busStopString.split(", ");
                                    for (String a : split1) {
                                        if (a.equals(id)) {
                                            busStop.setChecked(true);
                                        } else {
                                            busStop.setChecked(false);
                                        }
                                    }
                                }
                                busStopsFound.add(busStop);
                            }
                        } else if (name.toLowerCase().contains(query)) {
                            if (!name.equals("Non Stop") && !name.equals("Non-Stop")) {
                                BusStopsFound busStop = new BusStopsFound();
                                busStop.setId(id);
                                busStop.setName(name);
                                busStop.setRoad(road);
                                busStop.setQuery("");
                                busStop.setTimings(new ArrayList<NearbyBusTimings>());
                                if (!busStopString.equals("EMPTY")) {
                                    String[] split1 = busStopString.split(", ");
                                    for (String a : split1) {
                                        if (a.equals(id)) {
                                            busStop.setChecked(true);
                                        } else {
                                            busStop.setChecked(false);
                                        }
                                    }
                                }
                                busStopsFound.add(busStop);
                            }
                        } else if (road.toLowerCase().contains(query)) {
                            if (!name.equals("Non Stop") && !name.equals("Non-Stop")) {
                                BusStopsFound busStop = new BusStopsFound();
                                busStop.setId(id);
                                busStop.setName(name);
                                busStop.setRoad(road);
                                busStop.setQuery("");
                                busStop.setTimings(new ArrayList<NearbyBusTimings>());
                                if (!busStopString.equals("EMPTY")) {
                                    String[] split1 = busStopString.split(", ");
                                    for (String a : split1) {
                                        if (a.equals(id)) {
                                            busStop.setChecked(true);
                                        } else {
                                            busStop.setChecked(false);
                                        }
                                    }
                                }
                                busStopsFound.add(busStop);
                            }
                        }

                        entry.setLength(0);
                    } else {
                        entry.append(busStopData).append('\n');
                    }
                }
                db.close();
                Log.e("SEARCH RESULTS", String.valueOf(busStopsFound.size()));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchBusStopsAdapter = new SearchBusStopsAdapter(getContext(), FragmentStops.this);
                        searchBusStopsAdapter.setSearchBusStopsResults(busStopsFound);
                        lvSearchStops.setAdapter(searchBusStopsAdapter);
                        if (busStopsFound.isEmpty()) {
                            lvSearchStops.setVisibility(View.INVISIBLE);

                            tvNoResults1.setAlpha(0f);
                            tvNoResults1.setVisibility(View.VISIBLE);
                            tvNoResults1.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .setListener(null);

                            searchStopsProgress.animate()
                                    .alpha(0f)
                                    .setDuration(300)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            searchStopsProgress.setVisibility(View.INVISIBLE);
                                        }
                                    });
                        } else {
                            busStopsFound.get(0).setQuery(query);
                            tvNoResults1.setVisibility(View.INVISIBLE);

                            lvSearchStops.setAlpha(0f);
                            lvSearchStops.setVisibility(View.VISIBLE);
                            lvSearchStops.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .setListener(null);

                            searchStopsProgress.animate()
                                    .alpha(0f)
                                    .setDuration(300)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            searchStopsProgress.setVisibility(View.INVISIBLE);
                                        }
                                    });
                        }
                    }
                });
            }
        };
        thread.start();
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id) {
        //Expanded group
        if (!parent.isGroupExpanded(groupPosition)) {
            //Expanded group
            String busStopId = busStopsFound.get(groupPosition).getId();
            findBusArrivalTimings(groupPosition, busStopId);
        } else {
            //Collapsed group
            lvSearchStops.collapseGroupWithAnimation(groupPosition);
        }
        return true;
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
                                        eta = "No data available from LTA";
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
                            busStopsFound.get(groupPosition).setTimings(arrivalTimings);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                        searchBusStopsAdapter.notifyDataSetChanged();
                        if (!refreshDontExpand) {
                            lvSearchStops.expandGroupWithAnimation(groupPosition);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        lvSearchStops.collapseGroupWithAnimation(groupPosition);
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
    public void refreshSearchStopsTimings(int groupPosition) {
        refreshDontExpand = true;

        String id = busStopsFound.get(groupPosition).getId();
        findBusArrivalTimings(groupPosition, id);
        refreshDontExpand = false;
    }
}
