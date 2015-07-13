package model;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by annupinju on 7/1/2015.
 */
public class ArtistInfo implements Parcelable {

    private String name;
    private String id;
    private String thumbnailUrl;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public ArtistInfo( String id, String name, String url){

        this.name = name;
        this.id = id;
        this.thumbnailUrl = url;
    }

    private ArtistInfo(Parcel in) {
        name = in.readString();
        id = in.readString();
        thumbnailUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeString(name);
        parcel.writeString(id);
        parcel.writeString(thumbnailUrl);
    }

    public static final Parcelable.Creator<ArtistInfo> CREATOR
            = new Parcelable.Creator<ArtistInfo>() {
        public ArtistInfo createFromParcel(Parcel in) {
            return new ArtistInfo(in);
        }

        public ArtistInfo[] newArray(int size) {
            return new ArtistInfo[size];
        }
    };

}
