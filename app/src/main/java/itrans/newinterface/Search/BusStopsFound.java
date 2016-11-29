package itrans.newinterface.Search;

import java.util.ArrayList;

import itrans.newinterface.Nearby.NearbyBusTimings;

public class BusStopsFound {

    private String name;
    private String road;
    private String id;
    private ArrayList<NearbyBusTimings> timings = new ArrayList<>();
    private String query;
    private boolean checked;

    public BusStopsFound() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoad() {
        return road;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<NearbyBusTimings> getTimings() {
        return timings;
    }

    public void setTimings(ArrayList<NearbyBusTimings> timings) {
        this.timings = timings;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
