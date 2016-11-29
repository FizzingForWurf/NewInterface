package itrans.newinterface.Search;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import itrans.newinterface.Bookmarks.BusStopBookmarks;
import itrans.newinterface.Nearby.AnimatedExpandableListView;
import itrans.newinterface.Nearby.NearbyBusTimings;
import itrans.newinterface.R;

public class SearchBusStopsAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private LayoutInflater inflater;
    private Context context;
    private SearchStopsAdapterInterface searchRefreshListener;

    private ArrayList<BusStopsFound> busStopsFounds;

    public SearchBusStopsAdapter(Context context, SearchStopsAdapterInterface searchRefreshListener) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.searchRefreshListener = searchRefreshListener;
    }

    public void setSearchBusStopsResults(ArrayList<BusStopsFound> items) {
        this.busStopsFounds = items;
    }

    public interface SearchStopsAdapterInterface {
        public void refreshSearchStopsTimings(int groupPosition);
    }

    @Override
    public View getRealChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
        NearbyBusTimings item = getChild(groupPosition, childPosition);
        if (isLastChild) {
            convertView = inflater.inflate(R.layout.custom_nearby_footer, null);
            final CheckBox nearbySaveStop = (CheckBox) convertView.findViewById(R.id.nearbySaveStop);
            final ImageButton nearbyRefreshTimings = (ImageButton) convertView.findViewById(R.id.nearbyRefreshTimings);
            final AnimatedVectorDrawableCompat animatedVector = AnimatedVectorDrawableCompat.create(context,
                    R.drawable.animated_refresh);
            nearbyRefreshTimings.setImageDrawable(animatedVector);

            nearbySaveStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BusStopBookmarks db = new BusStopBookmarks(context);
                    db.open();
                    if (db.getNumberOfRows() < 2) { //this means that bookmarks database isn't setup yet
                        //add items to database for setup
                        for (int i = 0; i < 2; i++) {
                            switch (i) {
                                case 0:
                                    db.setUpBookmarksLayout("BUSSERVICE", "EMPTY");
                                    break;
                                case 1:
                                    db.setUpBookmarksLayout("BUSSTOP", "EMPTY");
                                    break;
                            }
                        }
                    }

                    final String busStopString = db.getData(2);
                    String id = busStopsFounds.get(groupPosition).getId();
                    if (id.startsWith("0")) {
                        id = id.substring(1, 5);
                    }

                    if (nearbySaveStop.isChecked()) {
                        Toast.makeText(context, busStopsFounds.get(groupPosition).getName() + " added to bookmarks.", Toast.LENGTH_SHORT).show();
                        if (busStopString.equals("EMPTY")) { //no bus stops have been bookmarked yet so must
                            // add bus stop need to update the database with bus stop id
                            db.updateBusStop(id);
                        } else {  //database might have bookmark stored check if database already has bookmark
                            String compress = busStopString + ", " + id;
                            db.updateBusStop(compress);
                        }
                    } else {
                        final String finalId = id;
                        new AlertDialog.Builder(context)
                                .setTitle("Confirm delete")
                                .setMessage("Are you sure you want to remove '" + busStopsFounds.get(groupPosition).getName() + "' from your bookmarks?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                        BusStopBookmarks dbAdapter = new BusStopBookmarks(context);
                                        dbAdapter.open();
                                        if (!busStopString.equals("EMPTY")) {
                                            ArrayList<String> busStops = new ArrayList<String>();
                                            String[] split = busStopString.split(", ");
                                            for (String a : split) {
                                                busStops.add(a);
                                                if (finalId.equals(a)) {
                                                    busStops.remove(a);
                                                }
                                            }
                                            String array = busStops.toString();
                                            String newBusStopData = array.substring(array.indexOf("[") + 1, array.indexOf("]"));
                                            dbAdapter.updateBusStop(newBusStopData);
                                        }
                                        dbAdapter.close();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        nearbySaveStop.setChecked(true);
                                    }
                                })
                                .show();
                    }
                    db.close();
                }
            });

            nearbySaveStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = (Integer) buttonView.getTag();
                    busStopsFounds.get(position).setChecked(buttonView.isChecked());
                }
            });

            nearbyRefreshTimings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nearbyRefreshTimings.setImageDrawable(animatedVector);
                    if (animatedVector != null) {
                        animatedVector.start();
                    }
                    //searchRefreshListener.refreshSearchStopsTimings(groupPosition);
                }
            });
            nearbySaveStop.setTag(groupPosition);

            nearbySaveStop.setChecked(busStopsFounds.get(groupPosition).isChecked());
        } else {
            convertView = inflater.inflate(R.layout.custom_timings_row, parent, false);
            TextView tvBusNumber = (TextView) convertView.findViewById(R.id.nearbyBus);
            TextView tvBusTiming = (TextView) convertView.findViewById(R.id.nearbyBusTiming);
            View nearbyTimingLoad = convertView.findViewById(R.id.nearbyTimingLoad);

            if (item != null) {
                if (nearbyTimingLoad != null) {
                    switch (item.getBusLoad()) {
                        case "Seats Available":
                            nearbyTimingLoad.setVisibility(View.VISIBLE);
                            nearbyTimingLoad.setBackgroundColor(Color.parseColor("#4CAF50"));
                            break;
                        case "Standing Available":
                            nearbyTimingLoad.setVisibility(View.VISIBLE);
                            nearbyTimingLoad.setBackgroundColor(Color.parseColor("#FFC107"));
                            break;
                        case "Limited Standing":
                            nearbyTimingLoad.setVisibility(View.VISIBLE);
                            nearbyTimingLoad.setBackgroundColor(Color.parseColor("#F44336"));
                            break;
                        case "":
                            nearbyTimingLoad.setVisibility(View.INVISIBLE);
                            break;
                        default:
                            nearbyTimingLoad.setVisibility(View.INVISIBLE);
                            break;
                    }
                }
            }

            if (tvBusNumber != null && tvBusTiming != null && item != null) {
                tvBusNumber.setText(item.getBusService());
                if (!item.getBusTiming().equals("Not in Operation") && !item.getBusTiming().equals("No data available from LTA")) {
                    String timeRemaining = findBusArrivalTiming(item.getBusTiming());
                    tvBusTiming.setText(timeRemaining);
                } else {
                    tvBusTiming.setText(item.getBusTiming());
                }
            }
        }
        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return busStopsFounds.get(groupPosition).getTimings().size() + 1;
    }

    @Override
    public int getGroupCount() {
        return busStopsFounds.size();
    }

    @Override
    public BusStopsFound getGroup(int groupPosition) {
        return busStopsFounds.get(groupPosition);
    }

    @Override
    public NearbyBusTimings getChild(int groupPosition, int childPosition) {
        if (busStopsFounds.get(groupPosition).getTimings().size() > childPosition) {
            if (busStopsFounds.get(groupPosition).getTimings().get(childPosition) != null) {
                return busStopsFounds.get(groupPosition).getTimings().get(childPosition);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String query = busStopsFounds.get(0).getQuery();
        Log.e("TEST", query);

        BusStopsFound group = getGroup(groupPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.custom_search_stops_row, parent, false);
        }

        TextView tv1 = (TextView) convertView.findViewById(R.id.tvSearchName);
        TextView tv2 = (TextView) convertView.findViewById(R.id.tvSearchRoad);
        TextView tv3 = (TextView) convertView.findViewById(R.id.tvSearchId);

        String name = group.getName();
        String road = group.getRoad();
        String id = group.getId();

        if (name.toLowerCase().contains(query)) {
            tv1.setText(highlight(query, name));
        } else {
            tv1.setText(name);
        }

        if (road.toLowerCase().contains(query)) {
            tv2.setText(highlight(query, road));
        } else {
            tv2.setText(road);
        }

        if (id.toLowerCase().contains(query)) {
            tv3.setText(highlight(query, id));
        } else {
            tv3.setText(id);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private CharSequence highlight(String query, String originalText) {
        String normalizedText = Normalizer.normalize(originalText, Normalizer.Form.NFD)
                .replaceAll("\\p{InCOMBINING_DIACRITICAL_MARKS}+", "").toLowerCase();

        int start = normalizedText.indexOf(query);
        if (start < 0) {
            return originalText;
        } else {
            Spannable highlighted = new SpannableString(originalText);
            while (start >= 0) {
                int spanStart = Math.min(start, originalText.length());
                int spanEnd = Math.min(start + query.length(), originalText.length());

                highlighted.setSpan(new ForegroundColorSpan(Color.parseColor("#FFB300")), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = normalizedText.indexOf(query, spanEnd);
            }
            return highlighted;
        }
    }

    private String findBusArrivalTiming(String rawTiming) {
        String returnFormat = "No data available";
        DateFormat date = new SimpleDateFormat("HH:mm");

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00"));
        Date currentLocalTime = cal.getTime();
        String localTime = date.format(currentLocalTime);

        String LocalTimeHour = localTime.substring(0, 2);
        int localHour = Integer.parseInt(LocalTimeHour);
        String LocalTimeMinutes = localTime.substring(3, 5);
        int localMin = Integer.parseInt(LocalTimeMinutes);

        if (rawTiming.length() >= 25) {
            String time = rawTiming.substring(11, 16);
            String BusTimeHour = time.substring(0, 2);
            int BusHour = Integer.parseInt(BusTimeHour);
            String BusTimeMinutes = time.substring(3, 5);
            int BusMin = Integer.parseInt(BusTimeMinutes);

            if (localHour == BusHour && localMin < BusMin) {
                int timeDifference = BusMin - localMin;
                returnFormat = String.valueOf(timeDifference) + " min";
            } else if (localHour < BusHour) {
                int timeDifference = (BusMin + (BusHour - localHour) * 60) - localMin;
                returnFormat = String.valueOf(timeDifference) + " min";
            } else if (localHour > BusHour) {
                int timeDifference = (BusMin + ((24 - localHour) + (BusHour)) * 60) - localMin;
                returnFormat = String.valueOf(timeDifference) + " min";
            } else if (localHour == BusHour && localMin == BusMin) {
                returnFormat = "Arr";
            } else {
                returnFormat = "Arr";
            }
        }
        return returnFormat;
    }
}
