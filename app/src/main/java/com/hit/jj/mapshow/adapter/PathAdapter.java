package com.hit.jj.mapshow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.arcgis.android.samples.routing.R;
import com.hit.jj.pathplaning.Path;

import java.util.List;

/**
 * Created by jiajie on 15/11/25.
 */
public class PathAdapter extends BaseAdapter {
    private List<Path> mlist;
    private Context mContext;

    public PathAdapter(List<Path> mlist, Context mContext) {
        this.mlist = mlist;
        this.mContext = mContext;
    }

    public void setData(List<Path> list){
        this.mlist=list;
        super.notifyDataSetChanged();
    }
    public void clear(){
        this.mlist.clear();
        super.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        if (mlist==null){
            return 0;
        }
        return mlist.size();
    }

    @Override
    public Object getItem(int position) {
        if (mlist==null)
            return null;
        else
            return mlist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Path bean=mlist.get(position);
        if(convertView==null){
            convertView= LayoutInflater.from(mContext).inflate(R.layout.list_item,parent,false);
        }
        ImageView myImage = (ImageView) convertView.findViewById(R.id.imageView1);
        switch (bean.getNextDirection()){
            case 0:
                myImage.setImageResource(R.drawable.nav_straight);
                break;
            case 1:
                myImage.setImageResource(R.drawable.nav_left);
                break;
            case 2:
                myImage.setImageResource(R.drawable.nav_right);
                break;
            default:
                break;
        }
        TextView myTitle = (TextView) convertView.findViewById(R.id.segment);
        myTitle.setText(bean.getId());
        return convertView;
    }
}
