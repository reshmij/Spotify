package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import model.TrackInfo;
import nanodegree.reshmi.com.spotify.R;

/**
 * Created by annupinju on 7/1/2015.
 */
public class ArtistTopTracksListAdapter extends BaseAdapter {

    private List<TrackInfo> mTrackInfoList = new ArrayList<TrackInfo>();
    private Context mContext = null;

    private static LayoutInflater inflater = null;

    public ArtistTopTracksListAdapter(Context ctx, List<TrackInfo> list) {
        mTrackInfoList.addAll(list);
        mContext = ctx;
        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addAll(List<TrackInfo> list) {
        mTrackInfoList.addAll(list);
        notifyDataSetChanged();
    }

    public void removeAll() {
        mTrackInfoList.clear();
    }

    public void clear() {
        removeAll();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTrackInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mTrackInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private class ViewHolder {
        TextView tv1;
        TextView tv2;
        ImageView img;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = new ViewHolder();
        View row = convertView;

        if (convertView == null) {

            row = inflater.inflate(R.layout.top_ten_tracks_list_item, parent, false);

            holder.tv1 = (TextView) row.findViewById(R.id.text_view_track);
            holder.tv2 = (TextView) row.findViewById(R.id.text_view_album);
            holder.img = (ImageView) row.findViewById(R.id.img_view_track_thumbnail);

            row.setTag(holder);
        } else {

            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv1.setText(mTrackInfoList.get(position).getTrackName());
        holder.tv2.setText(mTrackInfoList.get(position).getAlbumName());

        ImageView imgView = holder.img;
        String imageUrl = mTrackInfoList.get(position).getAlbumThumbnailSmallUrl();

        if ((imageUrl != null)&& (!imageUrl.isEmpty())) {
            Picasso.with(mContext).load(imageUrl).into(imgView);
        }

        return row;
    }
}
