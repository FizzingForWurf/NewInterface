package itrans.newinterface.Alarm;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;

import itrans.newinterface.R;

public class CustomAlarmAdapter extends ArrayAdapter<Alarm> {

    private Context context;
    private ArrayList<Alarm> alarmList;
    private AdapterInterface switchListener;

    public interface AdapterInterface {
        public void switchActivate(Float distance);

        public void disableSwitch(boolean isNearbyAlready, int position);

        public void checkPermissionAndLocation();
    }

    public CustomAlarmAdapter(Context context, ArrayList<Alarm> list, AdapterInterface switchListener) {
        super(context, R.layout.custom_alarm_row, list);
        this.context = context;
        this.alarmList = list;
        this.switchListener = switchListener;
    }

    static class ViewHolder {
        protected TextView tvTitle;
        protected TextView tvDestination;
        protected TextView tvDistance;
        protected Switch alarmSwitch;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final boolean[] isOneActive = {false};
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflator = LayoutInflater.from(context);
            convertView = inflator.inflate(R.layout.custom_alarm_row, null);
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tvAlarmTitle);
            viewHolder.tvDestination = (TextView) convertView.findViewById(R.id.tvAlarmDestination);
            viewHolder.tvDistance = (TextView) convertView.findViewById(R.id.tvAlarmDistance);
            viewHolder.alarmSwitch = (Switch) convertView.findViewById(R.id.alarmSwitch);

            viewHolder.alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = (Integer) buttonView.getTag();  // Here we get the position that we have set for the checkbox using setTag.
                    Log.e("TEST", String.valueOf(position));
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            for (int i = 0; i < alarmList.size(); i++) {
                                isOneActive[0] = alarmList.get(position).isChecked();
                                if (isOneActive[0]) {
                                    break;
                                }
                            }

                            alarmList.get(position).setChecked(buttonView.isChecked()); // Set the value of switch to maintain its state.

                            if (isChecked) {
                                SharedPreferences prefs = context.getSharedPreferences("ONRESUME RESTORE", Context.MODE_PRIVATE);
                                if (!prefs.getBoolean("ONRESUME RESTORE", false)) {
                                    if (!isOneActive[0]) {
                                        SharedPreferences prefs1 = context.getSharedPreferences("ALARM RESTORE POSITION", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs1.edit();
                                        editor.putInt("ALARM RESTORE POSITION", position);
                                        editor.apply();

                                        Location location = alarmList.get(0).getCurrentLocation();
                                        AlarmDBAdapter dbAdapter = new AlarmDBAdapter(context);
                                        dbAdapter.open();
                                        String destination = dbAdapter.getLatLng(position + 1);
                                        Float range = Float.parseFloat(dbAdapter.getRadius(position + 1));
                                        dbAdapter.close();

                                        String[] latLng = destination.split(",");
                                        double latitude = Double.parseDouble(latLng[0]);
                                        double longitude = Double.parseDouble(latLng[1]);
                                        Location selectedLocation = new Location("SELECTED LOCATION");
                                        selectedLocation.setLatitude(latitude);
                                        selectedLocation.setLongitude(longitude);

                                        if (location != null) {
                                            Float distance = selectedLocation.distanceTo(location);
                                            if (distance > range) {
                                                switchListener.switchActivate(distance);
                                            } else {
                                                Toast.makeText(getContext(), "You are already near your destination!", Toast.LENGTH_SHORT).show();
                                                buttonView.setChecked(false);
                                                alarmList.get(position).setChecked(false);
                                                switchListener.disableSwitch(true, position);
                                            }
                                        }

                                    } else {
                                        Toast.makeText(context, "You can only set one alarm.", Toast.LENGTH_SHORT).show();
                                        buttonView.setChecked(false);
                                        alarmList.get(position).setChecked(false);
                                    }
                                } else {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("ONRESUME RESTORE", false);
                                    editor.apply();
                                }
                            } else {
                                isOneActive[0] = false;
                                switchListener.disableSwitch(false, position);
                            }
                        } else {
                            buttonView.setChecked(false);
                            alarmList.get(position).setChecked(false);
                            switchListener.checkPermissionAndLocation();
                        }
                    } else {
                        buttonView.setChecked(false);
                        alarmList.get(position).setChecked(false);
                        switchListener.checkPermissionAndLocation();
                    }
                }
            });

            convertView.setTag(viewHolder);
            convertView.setTag(R.id.tvAlarmTitle, viewHolder.tvTitle);
            convertView.setTag(R.id.tvAlarmDestination, viewHolder.tvDestination);
            convertView.setTag(R.id.tvAlarmDistance, viewHolder.tvDistance);
            convertView.setTag(R.id.alarmSwitch, viewHolder.alarmSwitch);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.alarmSwitch.setTag(position); // This line is important.

        viewHolder.tvTitle.setText(alarmList.get(position).getTitle());
        viewHolder.tvDestination.setText(alarmList.get(position).getDestination());
        viewHolder.alarmSwitch.setChecked(alarmList.get(position).isChecked());

        if (alarmList.get(position).getDistance() == -1) {
            viewHolder.tvDistance.setText("Distance left: ");
        } else {
            viewHolder.tvDistance.setText("Distance left: " + round(alarmList.get(position).getDistance(), 2) + "m");
        }

        return convertView;
    }

    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }
}
