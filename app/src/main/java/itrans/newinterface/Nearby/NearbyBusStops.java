package itrans.newinterface.Nearby;

import java.util.ArrayList;

public class NearbyBusStops {
    private String busStopName;
    private String busStopRoad;
    private String busStopID;
    private Double busStopLat;
    private Double busStopLng;
    private int proximity;
    private boolean checked;

    private ArrayList<NearbyBusTimings> arrivalTimings;

    public String getBusStopName() {
        return busStopName;
    }

    public void setBusStopName(String busStopName) {
        this.busStopName = busStopName;
    }

    public String getBusStopRoad() {
        return busStopRoad;
    }

    public void setBusStopRoad(String busStopRoad) {
        this.busStopRoad = busStopRoad;
    }

    public Double getBusStopLat() {
        return busStopLat;
    }

    public void setBusStopLat(Double busStopLat) {
        this.busStopLat = busStopLat;
    }

    public Double getBusStopLng() {
        return busStopLng;
    }

    public void setBusStopLng(Double busStopLng) {
        this.busStopLng = busStopLng;
    }

    public String getBusStopID() {
        return busStopID;
    }

    public void setBusStopID(String busStopID) {
        this.busStopID = busStopID;
    }

    public ArrayList<NearbyBusTimings> getArrivalTimings() {
        return arrivalTimings;
    }

    public void setArrivalTimings(ArrayList<NearbyBusTimings> arrivalTimings) {
        this.arrivalTimings = arrivalTimings;
    }

    public int getProximity() {
        return proximity;
    }

    public void setProximity(int proximity) {
        this.proximity = proximity;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
