package com.hit.jj.mapshow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.esri.arcgis.android.samples.routing.R;
import com.hit.jj.pathplaning.Buliding;

import java.util.List;

/**
 * Created by jiajie on 15/11/10.
 */
public class BuildingAdapter extends BaseAdapter  implements Filterable {
    private Context mContext;
    private List<Buliding> mlist;

    public BuildingAdapter(Context mContext, List<Buliding> mlist) {
        this.mContext = mContext;
        this.mlist = mlist;
    }
    public void setData(List<Buliding> list){
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
        Buliding bean=mlist.get(position);
        if(convertView==null){
            convertView= LayoutInflater.from(mContext).inflate(R.layout.list_item_build,parent,false);
        }
        TextView tv=ViewHolder.get(convertView,R.id.tip_build);
        tv.setText(bean.getName());
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return null;
    }
}
