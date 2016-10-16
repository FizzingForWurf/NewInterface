package itrans.newinterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("iTrans");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        final FloatingActionButton AddDestinationFab = (FloatingActionButton) findViewById(R.id.AddDestinationFab);
        AddDestinationFab.hide();
        AddDestinationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddDestination.class);
                startActivity(intent);
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        setTitle("iTrans");
                        AddDestinationFab.hide();
                        Drawable drawable = MainActivity.this.getResources().getDrawable(R.drawable.ic_home_white_24dp);
                        drawable.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_IN));
                        break;
                    case 1:
                        setTitle("Alarms");
                        AddDestinationFab.show();
                        break;
                    case 2:
                        setTitle("Bookmarks");
                        AddDestinationFab.hide();
                        break;
                    case 3:
                        setTitle("Nearby");
                        AddDestinationFab.hide();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        BusStopContainerDBAdapter db = new BusStopContainerDBAdapter(getApplicationContext());
        db.open();
        for (int i = 1; i < 13; i++) {
            String test = db.getInitialBusStops(i);
            Log.e("SORTING TEST", test);
        }
        db.close();

        //startSortingBusStops();
    }

    private void startSortingBusStops(){
        ArrayList<Double> longitudinalArray = new ArrayList<>();
        longitudinalArray.add(103.597870);
        ArrayList<Double> latitudinalArray = new ArrayList<>();
        latitudinalArray.add(1.476153);
        final double latitudeIncrement = 4/110.574; //in kilometers, 0.036175
        final double longitudeIncrement = 4/(111.320 * Math.cos(Math.toRadians(1.476153))); //in kilometers, 0.035944376
        double lastDistance1 = 0;
        double lastDistance2 = 0;

        HashMap<Integer, LatLngBounds> busStopsContainers = new HashMap<>();
        //Boundaries of Singapore
        //LatLng Northwest = new LatLng(1.476153, 103.597870);
        //LatLng Southeast = new LatLng(1.216673, 104.102554);

        Location topLeft = new Location("topleft");
        topLeft.setLatitude(1.476153);
        topLeft.setLongitude(103.597870);

        Location topRight = new Location("topright");
        topRight.setLatitude(1.476153);
        topRight.setLongitude(104.102554);

        Location bottomLeft = new Location("bottomleft");
        bottomLeft.setLatitude(1.216673);
        bottomLeft.setLongitude(103.597870);

        Location bottomRight = new Location("bottomright");
        bottomRight.setLatitude(1.216673);
        bottomRight.setLongitude(104.102554);

        double longitudinalDistance = topLeft.distanceTo(topRight);
        double latitudinalDistance = topLeft.distanceTo(bottomLeft);
        Log.e("DISTANCES LATLNG", String.valueOf(latitudinalDistance) + ", " + String.valueOf(longitudinalDistance));

        //while loop for longitude
        double variable1 = 103.597870;
        while (lastDistance1 <= longitudinalDistance){
            variable1 += longitudeIncrement;
            longitudinalArray.add(variable1);
            Location newTemporaryPoint = new Location("TemporaryPoint");
            newTemporaryPoint.setLatitude(1.476153);
            newTemporaryPoint.setLongitude(variable1);
            lastDistance1 = topLeft.distanceTo(newTemporaryPoint);
            Log.e("SORTING LONGITUDE", String.valueOf(variable1));
            Log.e("SORTING DISTANCE1", String.valueOf(lastDistance1));
        }

        //while loop for latitude
        double variable2 = 1.476153;
        while (lastDistance2 <= latitudinalDistance){
            variable2 -= latitudeIncrement;
            latitudinalArray.add(variable2);
            Location newPoint = new Location("newPoint");
            newPoint.setLatitude(variable2);
            newPoint.setLongitude(103.597870);
            lastDistance2 = topLeft.distanceTo(newPoint);
            Log.e("SORTING LATITUDE", String.valueOf(variable2));
            Log.e("SORTING DISTANCE2", String.valueOf(lastDistance2));
        }

        int number = 1;
        Log.e("SORTING SIZES", String.valueOf(latitudinalArray.size()) + ", " + String.valueOf(longitudinalArray.size()));
        for (int i = 0; i < latitudinalArray.size() - 2; i++){
            for (int a = 0; a < longitudinalArray.size() - 2; a++){
                number++;
                LatLng Southwest = new LatLng(latitudinalArray.get(i + 1), longitudinalArray.get(a));
                LatLng Northeast = new LatLng(latitudinalArray.get(i), longitudinalArray.get(a + 1));
                LatLngBounds container = new LatLngBounds(Southwest, Northeast);
                busStopsContainers.put(number, container);
                Log.e("SORTING HASHMAP", Southwest.toString() + ", " + Northeast.toString());
            }
        }
        Log.e("HASHMAP SIZE", String.valueOf(busStopsContainers.size()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.action_feedback){
            return true;
        }else if (id == R.id.action_about){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("About us");
            builder.setCancelable(true);
            builder.setMessage("We are iTans, a group of Secondary 3s from Hwa Chong Institution, " +
                    "and we present an app that makes your everyday commuting much easier.");

            builder.setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_one, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Fragment fragment = new Fragment();
            if (position == 0){
                fragment = new FragmentHome();
            }else if (position == 1){
                fragment = new FragmentAlarm();
            }else if (position == 2){
                fragment = new FragmentBookmarks();
            }else if (position == 3){
                fragment = new FragmentNearby();
            }
            return fragment;//PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int[] tabIcons = {R.drawable.ic_home_white_24dp, R.drawable.ic_access_alarm_white_24dp,
                    R.drawable.ic_bookmark_white_24dp, R.drawable.ic_room_white_24dp};

            Drawable image = ContextCompat.getDrawable(MainActivity.this, tabIcons[position]);
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            SpannableString sb = new SpannableString(" ");
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }
    }
}
