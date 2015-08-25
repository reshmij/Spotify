package model;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by annupinju on 7/1/2015.
 */
public class TrackInfo implements Parcelable {
    private String trackName;
    private String albumName;
    private String albumThumbnailSmallUrl;
    private String getAlbumThumbnailLrgUrl;
    private String trackUrl;
    private String externalUrl;

    public TrackInfo(String trackName, String albumName, String albumThumbnailSmallUrl, String getAlbumThumbnailLrgUrl, String trackUrl, String externalUrl) {
        this.trackName = trackName;
        this.albumName = albumName;
        this.albumThumbnailSmallUrl = albumThumbnailSmallUrl;
        this.getAlbumThumbnailLrgUrl = getAlbumThumbnailLrgUrl;
        this.trackUrl = trackUrl;
        this.externalUrl = externalUrl;
    }

    private TrackInfo(Parcel in) {
        this.trackName = in.readString();
        this.albumName = in.readString();
        this.albumThumbnailSmallUrl = in.readString();
        this.getAlbumThumbnailLrgUrl = in.readString();
        this.trackUrl = in.readString();
        this.externalUrl = in.readString();
    }

    public String getTrackUrl() {
        return trackUrl;
    }

    public String getGetAlbumThumbnailLrgUrl() {
        return getAlbumThumbnailLrgUrl;
    }

    public String getAlbumThumbnailSmallUrl() {
        return albumThumbnailSmallUrl;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getTrackName() {
        return trackName;
    }


    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeString(this.trackName);
        parcel.writeString(this.albumName);
        parcel.writeString(this.albumThumbnailSmallUrl);
        parcel.writeString(this.getAlbumThumbnailLrgUrl);
        parcel.writeString(this.trackUrl);
        parcel.writeString(this.externalUrl);
    }

    public static final Parcelable.Creator<TrackInfo> CREATOR
            = new Parcelable.Creator<TrackInfo>() {
        public TrackInfo createFromParcel(Parcel in) {
            return new TrackInfo(in);
        }

        public TrackInfo[] newArray(int size) {
            return new TrackInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {

        //Override the equals method.
        TrackInfo in = (TrackInfo) o;
        if (in.getTrackName().equals(this.trackName) &&
                in.getAlbumName().equals(this.albumName) &&
                in.getTrackUrl().equals(this.trackUrl) &&
                in.getExternalUrl().equals(this.externalUrl)) {
            return true;
        } else {
            return false;
        }

    }
}
