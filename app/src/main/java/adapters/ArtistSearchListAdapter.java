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

import model.ArtistInfo;
import nanodegree.reshmi.com.spotify.R;

/**
 * Created by annupinju on 7/1/2015.
 */
public class ArtistSearchListAdapter extends BaseAdapter {

    private List<ArtistInfo> mArtistInfoList = new ArrayList<ArtistInfo>();
    private Context mContext = null;

    private static LayoutInflater inflater = null;

    public ArtistSearchListAdapter(Context ctx, List<ArtistInfo> list) {

        mArtistInfoList.addAll(list);
        mContext = ctx;
        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addAll(List<ArtistInfo> list) {
        mArtistInfoList.addAll(list);
        notifyDataSetChanged();
    }

    public void removeAll() {
        mArtistInfoList.clear();
    }

    public void clear() {
        removeAll();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mArtistInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mArtistInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private class ViewHolder {
        TextView tv;
        ImageView img;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = new ViewHolder();
        View row = convertView;

        if (convertView == null) {

            row = inflater.inflate(R.layout.artist_search_result_list_item, parent, false);

            holder.tv = (TextView) row.findViewById(R.id.text_view_artist_search_result);
            holder.img = (ImageView) row.findViewById(R.id.img_view_artist_search_result);

            row.setTag(holder);
        } else {

            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv.setText(mArtistInfoList.get(position).getName());

        ImageView imgView = holder.img;
        String imageUrl = mArtistInfoList.get(position).getThumbnailUrl();

        if (imageUrl != null) {
            Picasso.with(mContext).load(imageUrl).into(imgView);
        }

        return row;
    }
}
