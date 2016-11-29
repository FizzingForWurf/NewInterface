package itrans.newinterface.Bookmarks;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import itrans.newinterface.BusNumberDBAdapter;
import itrans.newinterface.Internet.VolleySingleton;
import itrans.newinterface.Nearby.AnimatedExpandableListView;
import itrans.newinterface.Nearby.NearbyBusTimings;
import itrans.newinterface.R;
import itrans.newinterface.Search.BusStopsFound;

public class FragmentBookmarks extends Fragment implements View.OnClickListener, BookmarkStopsAdapter.BookmarkStopsAdapterInterface,
        ExpandableListView.OnGroupClickListener {

    private LinearLayout bookmarkTypeSelect;
    private LinearLayout bookmarkServiceSelector;
    private LinearLayout bookmarkStopSelector;

    private TextView tvBookmarksEmpty;
    private ListView lvBusServiceBookmarks;

    private AnimatedExpandableListView lvBusStopBookmarks;
    private BookmarkStopsAdapter stopsAdapter;
    private ArrayList<BusStopsFound> bookmarksStopsList = new ArrayList<>();
    private ArrayList<NearbyBusTimings> arrivalTimings = new ArrayList<>();

    private boolean fragmentOnCreated = false;
    private boolean refreshDontExpand = false;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    public FragmentBookmarks() {
        // Required empty public constructor
    }

    public static FragmentBookmarks newInstance() {
        FragmentBookmarks fragment = new FragmentBookmarks();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_bookmarks, container, false);
        bookmarkTypeSelect = (LinearLayout) v.findViewById(R.id.bookmarkSelect);
        bookmarkServiceSelector = (LinearLayout) v.findViewById(R.id.bookmarkServiceSelector);
        bookmarkStopSelector = (LinearLayout) v.findViewById(R.id.bookmarkStopSelector);

        bookmarkStopSelector.setOnClickListener(this);
        bookmarkServiceSelector.setOnClickListener(this);

        lvBusStopBookmarks = (AnimatedExpandableListView) v.findViewById(R.id.lvBusStopBookmarks);
        lvBusStopBookmarks.setOnGroupClickListener(this);
        lvBusServiceBookmarks = (ListView) v.findViewById(R.id.lvBusServiceBookmarks);
        registerForContextMenu(lvBusStopBookmarks);
        registerForContextMenu(lvBusServiceBookmarks);
        tvBookmarksEmpty = (TextView) v.findViewById(R.id.tvBookmarksEmpty);

        tvBookmarksEmpty.setText("No bookmarks found.");
        tvBookmarksEmpty.setVisibility(View.INVISIBLE);
        lvBusServiceBookmarks.setVisibility(View.INVISIBLE);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        populateBookmarks();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getView() != null && !fragmentOnCreated) { //beside fragment
            ObjectAnimator translate = ObjectAnimator.ofFloat(bookmarkTypeSelect, View.TRANSLATION_Y, 0);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(bookmarkTypeSelect, View.ALPHA, 1);
            translate.setDuration(300);
            alpha.setDuration(300);

            AnimatorSet set = new AnimatorSet();
            set.play(alpha).with(translate);

            set.start();
        }

        if (isVisibleToUser) {
            fragmentOnCreated = true;
            populateBookmarks();
        }
        if ((isVisibleToUser && isResumed())) {
            fragmentOnCreated = true;
            populateBookmarks();
        }
        if (!isVisibleToUser) {
            //fragment no longer visible (after being created)
            if (bookmarkTypeSelect != null) {
                fragmentOnCreated = false;
                bookmarkTypeSelect.setTranslationY(bookmarkTypeSelect.getHeight());
                bookmarkTypeSelect.setAlpha(0);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bookmarkServiceSelector:

                break;
            case R.id.bookmarkStopSelector:

                break;
        }
    }

    private void populateBookmarks() {
        tvBookmarksEmpty.setVisibility(View.INVISIBLE);
        bookmarksStopsList.clear();
        BusStopBookmarks db = new BusStopBookmarks(getContext());
        db.open();
        if (db.getNumberOfRows() >= 2) {
            String busStopsString = db.getData(2);
            if (!busStopsString.equals("EMPTY")) {
                String[] split = busStopsString.split(", ");
                BusNumberDBAdapter dbAdapter = new BusNumberDBAdapter(getContext());
                dbAdapter.open();
                for (String a : split) {
                    String stopId = a;
                    if (a.startsWith("0")) {
                        a = a.substring(1, 5);
                    }
                    String stopName = dbAdapter.getBusStopName(a);
                    String stopRoad = dbAdapter.getBusStopRoad(a);
                    BusStopsFound busStopsFound = new BusStopsFound();

                    busStopsFound.setName(stopName);
                    busStopsFound.setRoad(stopRoad);
                    busStopsFound.setId(stopId);
                    busStopsFound.setTimings(new ArrayList<NearbyBusTimings>());

                    bookmarksStopsList.add(busStopsFound);
                }
                dbAdapter.close();

                stopsAdapter = new BookmarkStopsAdapter(getContext(), FragmentBookmarks.this);
                stopsAdapter.setBookmarkStopsData(bookmarksStopsList);
                lvBusStopBookmarks.setAdapter(stopsAdapter);
            } else {
                tvBookmarksEmpty.setVisibility(View.VISIBLE);
            }
        }
        db.close();
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id) {
        if (!parent.isGroupExpanded(groupPosition)) {
            //Expanded group
            String busStopId = bookmarksStopsList.get(groupPosition).getId();
            findBusArrivalTimings(groupPosition, busStopId);
        } else {
            //Collapsed group
            lvBusStopBookmarks.collapseGroupWithAnimation(groupPosition);
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
                            bookmarksStopsList.get(groupPosition).setTimings(arrivalTimings);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                        stopsAdapter.notifyDataSetChanged();
                        if (!refreshDontExpand) {
                            lvBusStopBookmarks.expandGroupWithAnimation(groupPosition);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        lvBusStopBookmarks.collapseGroupWithAnimation(groupPosition);

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
    public void refreshBookmarkStopsTimings(int groupPosition) {
        refreshDontExpand = true;

        String id = bookmarksStopsList.get(groupPosition).getId();
        findBusArrivalTimings(groupPosition, id);
        refreshDontExpand = false;
        stopsAdapter.notifyDataSetChanged();
    }
}
