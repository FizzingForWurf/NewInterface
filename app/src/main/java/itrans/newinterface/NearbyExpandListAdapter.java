package itrans.newinterface;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

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
            Log.e("EXPANDABLE LIST COUNT", String.valueOf(chList.size()));
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
                Log.e("EXPANDABLE LIST CHILD", String.valueOf(chList.size()));
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
            tv2.setText(child.getBusTiming());
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
