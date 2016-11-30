package com.example.tengyu.mediaplayer.listviewactivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.tengyu.mediaplayer.R;

import java.util.List;

/**
 * Created by tengyu on 2016/6/24.
 */
public class MusicAdapter extends BaseAdapter {

    private List<MusicData> listDatas;
    private LayoutInflater inflater;

    public MusicAdapter(Context context, List<MusicData> list) {
        this.listDatas = list;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listDatas.size();
    }

    @Override
    public Object getItem(int i) {
        return listDatas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        MusicData data = listDatas.get(i);
        ViewHolder holder = null;

        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.activity_item, null);

            holder.title = (TextView) view.findViewById(R.id.tvName);
            holder.subtitle = (TextView) view.findViewById(R.id.tvSinger);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.title.setText(data.getTitle()+"");
        holder.subtitle.setText(data.getArtist()+"");
        

        return view;
    }

    private static class ViewHolder {
        TextView title;
        TextView subtitle;
    }

}
