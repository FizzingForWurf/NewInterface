package itrans.newinterface.Nearby;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

public class NearbySuggestions implements SearchSuggestion{
    private String mPlaceName;
    private String mSecondaryPlaceName;
    private String mPlaceID;

    public NearbySuggestions(){

    }

    public NearbySuggestions(String suggestion) {
        this.mPlaceName = suggestion.toLowerCase();
    }

    public NearbySuggestions(Parcel source) {
        this.mPlaceName = source.readString();
    }

    public String getmPlaceName() {
        return mPlaceName;
    }

    public void setmPlaceName(String mPlaceName) {
        this.mPlaceName = mPlaceName;
    }

    public String getmSecondaryPlaceName() {
        return mSecondaryPlaceName;
    }

    public void setmSecondaryPlaceName(String mSecondaryPlaceName) {
        this.mSecondaryPlaceName = mSecondaryPlaceName;
    }

    public String getmPlaceID() {
        return mPlaceID;
    }

    public void setmPlaceID(String mPlaceID) {
        this.mPlaceID = mPlaceID;
    }

    @Override
    public String getBody() {
        return mPlaceName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Creator<NearbySuggestions> CREATOR = new Creator<NearbySuggestions>() {
        @Override
        public NearbySuggestions createFromParcel(Parcel in) {
            return new NearbySuggestions(in);
        }

        @Override
        public NearbySuggestions[] newArray(int size) {
            return new NearbySuggestions[size];
        }
    };
}
