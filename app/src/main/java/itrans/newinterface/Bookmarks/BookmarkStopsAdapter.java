package itrans.newinterface.Bookmarks;

import android.content.Context;
import android.graphics.Color;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import itrans.newinterface.Nearby.AnimatedExpandableListView;
import itrans.newinterface.Nearby.NearbyBusTimings;
import itrans.newinterface.R;
import itrans.newinterface.Search.BusStopsFound;

public class BookmarkStopsAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private LayoutInflater inflater;
    private Context context;
    private BookmarkStopsAdapterInterface refreshListener;

    private ArrayList<BusStopsFound> bookmarkStopsList;

    public BookmarkStopsAdapter(Context context, BookmarkStopsAdapterInterface refreshListener) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.refreshListener = refreshListener;
    }

    public void setBookmarkStopsData(ArrayList<BusStopsFound> list) {
        bookmarkStopsList = list;
    }

    public interface BookmarkStopsAdapterInterface {
        public void refreshBookmarkStopsTimings(int groupPosition);
    }

    @Override
    public View getRealChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        NearbyBusTimings item = getChild(groupPosition, childPosition);
        if (isLastChild) {
            convertView = inflater.inflate(R.layout.custom_nearby_footer, null);
            CheckBox nearbySaveStop = (CheckBox) convertView.findViewById(R.id.nearbySaveStop);
            final ImageButton nearbyRefreshTimings = (ImageButton) convertView.findViewById(R.id.nearbyRefreshTimings);
            final AnimatedVectorDrawableCompat animatedVector = AnimatedVectorDrawableCompat.create(context,
                    R.drawable.animated_refresh);

            nearbySaveStop.setVisibility(View.INVISIBLE);
            nearbyRefreshTimings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nearbyRefreshTimings.setImageDrawable(animatedVector);
                    if (animatedVector != null) {
                        animatedVector.start();
                    }
                    //refreshListener.refreshBookmarkStopsTimings(groupPosition);
                }
            });
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
        return bookmarkStopsList.get(groupPosition).getTimings().size() + 1;
    }

    @Override
    public int getGroupCount() {
        return bookmarkStopsList.size();
    }

    @Override
    public BusStopsFound getGroup(int groupPosition) {
        return bookmarkStopsList.get(groupPosition);
    }

    @Override
    public NearbyBusTimings getChild(int groupPosition, int childPosition) {
        if (bookmarkStopsList.get(groupPosition).getTimings().size() > childPosition) {
            if (bookmarkStopsList.get(groupPosition).getTimings().get(childPosition) != null) {
                return bookmarkStopsList.get(groupPosition).getTimings().get(childPosition);
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
        BusStopsFound group = getGroup(groupPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.custom_search_stops_row, parent, false);
        }

        TextView tv1 = (TextView) convertView.findViewById(R.id.tvSearchName);
        TextView tv2 = (TextView) convertView.findViewById(R.id.tvSearchRoad);
        TextView tv3 = (TextView) convertView.findViewById(R.id.tvSearchId);

        tv1.setText(group.getName());
        tv2.setText(group.getRoad());
        tv3.setText(group.getId());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private String findBusArrivalTiming(String rawTiming) {
        String returnFormat = "No data available from LTA";
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
