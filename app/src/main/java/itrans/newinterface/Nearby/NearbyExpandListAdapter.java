package itrans.newinterface.Nearby;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import itrans.newinterface.R;

public class NearbyExpandListAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {
    private LayoutInflater inflater;

    private ArrayList<NearbyBusStops> nearbyBusStops;

    public NearbyExpandListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setData(ArrayList<NearbyBusStops> items) {
        this.nearbyBusStops = items;
    }

    @Override
    public NearbyBusTimings getChild(int groupPosition, int childPosition) {
        if (nearbyBusStops.get(groupPosition).getArrivalTimings().size() > childPosition) {
            if (nearbyBusStops.get(groupPosition).getArrivalTimings().get(childPosition) != null) {
                return nearbyBusStops.get(groupPosition).getArrivalTimings().get(childPosition);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        NearbyBusTimings item = getChild(groupPosition, childPosition);
        if (isLastChild) {
            convertView = inflater.inflate(R.layout.custom_nearby_footer, null);
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
        return nearbyBusStops.get(groupPosition).getArrivalTimings().size() + 1;
    }

    @Override
    public NearbyBusStops getGroup(int groupPosition) {
        return nearbyBusStops.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return nearbyBusStops.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        NearbyBusStops group = getGroup(groupPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.custom_nearby_row, parent, false);
        }

        TextView tv1 = (TextView) convertView.findViewById(R.id.tvBusStopName);
        TextView tv2 = (TextView) convertView.findViewById(R.id.tvBusStopRoad);
        TextView tv3 = (TextView) convertView.findViewById(R.id.tvBusStopId);
        TextView tv4 = (TextView) convertView.findViewById(R.id.tvNearbyDistance);
        ProgressBar findingTimingProgress = (ProgressBar) convertView.findViewById(R.id.findingTimingProgress);

        tv1.setText(group.getBusStopName());
        tv2.setText(group.getBusStopRoad());
        tv3.setText(" (" + group.getBusStopID() + ")");
        if (group.getProximity() == -100) {
            findingTimingProgress.setVisibility(View.VISIBLE);
            tv4.setVisibility(View.INVISIBLE);
        } else {
            findingTimingProgress.setVisibility(View.INVISIBLE);
            tv4.setText(group.getProximity() + "m");
            tv4.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
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
