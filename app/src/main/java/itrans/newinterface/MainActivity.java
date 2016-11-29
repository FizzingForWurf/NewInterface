package itrans.newinterface;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import itrans.newinterface.Alarm.AddDestination;
import itrans.newinterface.Alarm.FragmentAlarm;
import itrans.newinterface.Bookmarks.FragmentBookmarks;
import itrans.newinterface.Nearby.FragmentNearby;
import itrans.newinterface.Nearby.NearbyMap;
import itrans.newinterface.Search.SearchResults;
import itrans.newinterface.Settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 100;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FloatingActionButton viewMapFab;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

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
        prefs = getPreferences(Context.MODE_PRIVATE);
        editor = prefs.edit();

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.getTabAt(0).setText("Alarms");
        tabLayout.getTabAt(1).setText("Bookmarks");
        tabLayout.getTabAt(2).setText("Nearby");

        final FloatingActionButton AddDestinationFab = (FloatingActionButton) findViewById(R.id.AddDestinationFab);
        AddDestinationFab.show();
        AddDestinationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddDestination.class);
                startActivity(intent);
            }
        });

        viewMapFab = (FloatingActionButton) findViewById(R.id.ViewMapFab);
        viewMapFab.hide();
        viewMapFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NearbyMap.class);
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
                        AddDestinationFab.show();
                        viewMapFab.hide();
                        break;
                    case 1:
                        AddDestinationFab.hide();
                        viewMapFab.hide();
                        break;
                    case 2:
                        AddDestinationFab.hide();
                        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            viewMapFab.show();
                        }else{
                            viewMapFab.hide();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSIONS_REQUEST_FINE_LOCATION);
                            }
                        }
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mViewPager.getCurrentItem() == 2){
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                viewMapFab.hide();
            }else{
                viewMapFab.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("LOCATIONACCEPTED", true);
                    editor.apply();
                }else {
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("LOCATIONPERMISSION", false);
                    editor.apply();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case FragmentNearby.GPS_REQUEST_CODE:
                    switch (resultCode) {
                        case RESULT_OK:
                            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("LOCATION ACCEPTED", true);
                            editor.apply();
                            break;
                        case RESULT_CANCELED:
                            SharedPreferences prefs1 = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor1 = prefs1.edit();
                            editor1.putBoolean("LOCATION ACCEPTED", false);
                            editor1.apply();
                            break;
                    }
                    break;
                case FragmentAlarm.GPS_REQUEST_CODE_ALARM:
                    switch (resultCode) {
                        case RESULT_OK:
                            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("Bookmark Location ACCEPTED", true);
                            editor.apply();
                            break;
                        case RESULT_CANCELED:

                            break;
                    }
                    break;
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        ComponentName cn = new ComponentName(this, SearchResults.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));

        LinearLayout searchBar = (LinearLayout) searchView.findViewById(R.id.search_bar);
        searchBar.setLayoutTransition(new LayoutTransition());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settings  = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settings);
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
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Fragment fragment = new Fragment();
            if (position == 0){
                fragment = new FragmentAlarm();
            }else if (position == 1){
                fragment = new FragmentBookmarks();
            }else if (position == 2){
                fragment = new FragmentNearby();
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("LOCATIONPERMISSION", true);
                    editor.apply();
                }
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int[] tabIcons = {R.drawable.ic_home_blue_24dp, R.drawable.ic_access_alarm_blue_24dp,
                    R.drawable.ic_bookmark_blue_24dp, R.drawable.ic_room_blue_24dp};

            Drawable image = ContextCompat.getDrawable(MainActivity.this, tabIcons[position]);
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            SpannableString sb = new SpannableString(" ");
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}
