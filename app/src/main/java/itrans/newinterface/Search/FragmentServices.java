package itrans.newinterface.Search;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import itrans.newinterface.R;

public class FragmentServices extends Fragment implements AdapterView.OnItemClickListener {

    private String query;

    private ArrayList<String> busServicesFound = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;
    private ListView lvSearchServices;

    private TextView tvNoResults2;
    private ProgressBar searchServicesProgress;

    public FragmentServices() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        query = getArguments().getString("servicesQuery");

        View v = inflater.inflate(R.layout.fragment_search_services, container, false);
        lvSearchServices = (ListView) v.findViewById(R.id.lvSearchServices);
        tvNoResults2 = (TextView) v.findViewById(R.id.tvNoResults2);
        searchServicesProgress = (ProgressBar) v.findViewById(R.id.searchServicesProgress);

        lvSearchServices.setOnItemClickListener(this);
        tvNoResults2.setVisibility(View.INVISIBLE);
        searchServicesProgress.setVisibility(View.VISIBLE);

        searchForBusServices();
        return v;
    }

    private void searchForBusServices() {
        Thread thread = new Thread() {
            public void run() {
                InputStream data = getResources().openRawResource(R.raw.bus_service_number);
                Scanner sc = new Scanner(data);

                while (sc.hasNextLine()) {
                    String busService = sc.nextLine();
                    if (busService.toLowerCase().startsWith(query)) {
                        busServicesFound.add(busService);
                    }
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
                                busServicesFound);
                        lvSearchServices.setAdapter(arrayAdapter);

                        if (busServicesFound.isEmpty()) {
                            tvNoResults2.setVisibility(View.VISIBLE);
                        } else {
                            tvNoResults2.setVisibility(View.INVISIBLE);
                        }
                        searchServicesProgress.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        thread.start();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String busService = busServicesFound.get(position);
        Intent intent = new Intent(getContext(), SearchServices.class);
        intent.putExtra("SEARCH BUS SERVICE", busService);
        startActivity(intent);
    }
}
