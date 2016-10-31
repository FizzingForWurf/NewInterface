package itrans.newinterface.Nearby;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import itrans.newinterface.R;

public class NearbyExpandListAdapter extends BaseExpandableListAdapter{
    private Context context;
    private ArrayList<NearbyBusStops> nearbyBusStops;

    public NearbyExpandListAdapter(Context context, ArrayList<NearbyBusStops> nearbyBusStops) {
        this.context = context;
        this.nearbyBusStops = nearbyBusStops;
    }

    @Override
    public int getGroupCount() {
        return nearbyBusStops.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        ArrayList<NearbyBusTimings> chList = nearbyBusStops.get(groupPosition).getArrivalTimings();
        if (chList != null) {
            return chList.size();
        }else{
            return 0;
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return nearbyBusStops.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        ArrayList<NearbyBusTimings> chList = nearbyBusStops.get(groupPosition).getArrivalTimings();
        if (chList != null) {
            if (chList.size() != 0) {
                return chList.get(childPosition);
            }else{
                return null;
            }
        }else{
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
        NearbyBusStops group = (NearbyBusStops) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.custom_nearby_row, null);
        }
        TextView tv1 = (TextView) convertView.findViewById(R.id.tvBusStopName);
        TextView tv2 = (TextView) convertView.findViewById(R.id.tvBusStopRoad);
        TextView tv3 = (TextView) convertView.findViewById(R.id.tvBusStopId);
        TextView tv4 = (TextView) convertView.findViewById(R.id.tvNearbyDistance);
        tv1.setText(group.getBusStopName());
        tv2.setText(group.getBusStopRoad());
        tv3.setText(" (" + group.getBusStopID() + ")");
        tv4.setText(group.getProximity() + "m");
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                             ViewGroup parent) {
        NearbyBusTimings child = (NearbyBusTimings) getChild(groupPosition, childPosition);
//        if (isLastChild){
//            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
//            convertView = layoutInflater.inflate(R.layout.custom_nearby_footer, null);
//        }
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.custom_timings_row, null);
        }
        TextView tv1 = (TextView) convertView.findViewById(R.id.nearbyBus);
        TextView tv2 = (TextView) convertView.findViewById(R.id.nearbyBusTiming);

        if (child != null){
            tv1.setText(child.getBusService());
            if (!child.getBusTiming().equals("Not in Operation") && !child.getBusTiming().equals("No data available from LTA")) {
                String timeRemaining = findBusArrivalTiming(child.getBusTiming());
                tv2.setText(timeRemaining);
            }else{
                tv2.setText(child.getBusTiming());
            }
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
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

        Log.e("NEARBY BUS TIMING", rawTiming);
        if (rawTiming.length() >= 25) {
            String time = rawTiming.substring(11, 16);
            Log.e("NEARBY BUS TIMING", time);
            String BusTimeHour = time.substring(0, 2);
            Log.e("BUS HOUR", BusTimeHour);
            int BusHour = Integer.parseInt(BusTimeHour);
            String BusTimeMinutes = time.substring(3, 5);
            Log.e("BUS MINUTES", BusTimeMinutes);
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
                returnFormat = "Arriving";
            } else {
                returnFormat = "Arriving";
            }
        }
        return returnFormat;
    }
}
