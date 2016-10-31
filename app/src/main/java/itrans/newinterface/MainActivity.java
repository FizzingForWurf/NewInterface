package itrans.newinterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import itrans.newinterface.Nearby.FragmentNearby;
import itrans.newinterface.Nearby.NearbyMap;

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

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_white_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_access_alarm_blue_24dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_bookmark_blue_24dp);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_room_blue_24dp);

        final FloatingActionButton AddDestinationFab = (FloatingActionButton) findViewById(R.id.AddDestinationFab);
        AddDestinationFab.hide();
        AddDestinationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddDestination.class);
                startActivity(intent);
            }
        });

        final FloatingActionButton viewMapFab = (FloatingActionButton) findViewById(R.id.ViewMapFab);
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
                        setTitle("iTrans");
                        AddDestinationFab.hide();
                        viewMapFab.hide();
                        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_white_24dp);
                        tabLayout.getTabAt(1).setIcon(R.drawable.ic_access_alarm_blue_24dp);
                        tabLayout.getTabAt(2).setIcon(R.drawable.ic_bookmark_blue_24dp);
                        tabLayout.getTabAt(3).setIcon(R.drawable.ic_room_blue_24dp);
                        break;
                    case 1:
                        setTitle("Alarms");
                        AddDestinationFab.show();
                        viewMapFab.hide();
                        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_blue_24dp);
                        tabLayout.getTabAt(1).setIcon(R.drawable.ic_access_alarm_white_24dp);
                        tabLayout.getTabAt(2).setIcon(R.drawable.ic_bookmark_blue_24dp);
                        tabLayout.getTabAt(3).setIcon(R.drawable.ic_room_blue_24dp);
                        break;
                    case 2:
                        setTitle("Bookmarks");
                        AddDestinationFab.hide();
                        viewMapFab.hide();
                        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_blue_24dp);
                        tabLayout.getTabAt(1).setIcon(R.drawable.ic_access_alarm_blue_24dp);
                        tabLayout.getTabAt(2).setIcon(R.drawable.ic_bookmark_white_24dp);
                        tabLayout.getTabAt(3).setIcon(R.drawable.ic_room_blue_24dp);
                        break;
                    case 3:
                        setTitle("Nearby");
                        AddDestinationFab.hide();
                        viewMapFab.show();
                        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_blue_24dp);
                        tabLayout.getTabAt(1).setIcon(R.drawable.ic_access_alarm_blue_24dp);
                        tabLayout.getTabAt(2).setIcon(R.drawable.ic_bookmark_blue_24dp);
                        tabLayout.getTabAt(3).setIcon(R.drawable.ic_room_white_24dp);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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

//    /**
//     * A placeholder fragment containing a simple view.
//     */
//    public static class PlaceholderFragment extends Fragment {
//        private static final String ARG_SECTION_NUMBER = "section_number";
//
//        public PlaceholderFragment() {
//        }
//
//        /**
//         * Returns a new instance of this fragment for the given section
//         * number.
//         */
//        public static PlaceholderFragment newInstance(int sectionNumber) {
//            PlaceholderFragment fragment = new PlaceholderFragment();
//            Bundle args = new Bundle();
//            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_one, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
//            return rootView;
//        }
//    }

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
            int[] tabIcons = {R.drawable.ic_home_blue_24dp, R.drawable.ic_access_alarm_blue_24dp,
                    R.drawable.ic_bookmark_blue_24dp, R.drawable.ic_room_blue_24dp};

            Drawable image = ContextCompat.getDrawable(MainActivity.this, tabIcons[position]);
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            SpannableString sb = new SpannableString(" ");
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return null;
        }
    }
}
