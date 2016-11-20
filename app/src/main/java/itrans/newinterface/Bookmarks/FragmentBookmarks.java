package itrans.newinterface.Bookmarks;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import itrans.newinterface.R;

public class FragmentBookmarks extends Fragment {

    private LinearLayout bookmarkTypeSelect;

    private boolean fragmentOnCreated = false;

    public FragmentBookmarks() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentBookmarks.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentBookmarks newInstance(String param1, String param2) {
        FragmentBookmarks fragment = new FragmentBookmarks();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_bookmarks, container, false);
        bookmarkTypeSelect = (LinearLayout) v.findViewById(R.id.bookmarkSelect);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getView() != null && !fragmentOnCreated) { //beside fragment
            Log.e("BOOKMARKS", "BESIDE VISIBLE " + String.valueOf(bookmarkTypeSelect.getAlpha()));
            ObjectAnimator translate = ObjectAnimator.ofFloat(bookmarkTypeSelect, View.TRANSLATION_Y, 0);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(bookmarkTypeSelect, View.ALPHA, 1);
            translate.setDuration(300);
            alpha.setDuration(300);

            AnimatorSet set = new AnimatorSet();
            set.play(alpha).with(translate);

            set.start();
        }

        if (isVisibleToUser) {
            fragmentOnCreated = true;
        }
        if ((isVisibleToUser && isResumed())){
            fragmentOnCreated = true;
        }
        if (!isVisibleToUser) {
            //fragment no longer visible (after being created)
            Log.e("BOOKMARKS", "FIRST STAGE");
            if (bookmarkTypeSelect != null) {
                fragmentOnCreated = false;
                bookmarkTypeSelect.setTranslationY(bookmarkTypeSelect.getHeight());
                bookmarkTypeSelect.setAlpha(0);
                Log.e("BOOKMARKS", "SECOND STAGE " + String.valueOf(bookmarkTypeSelect.getAlpha()));
            }
        }
    }
}
