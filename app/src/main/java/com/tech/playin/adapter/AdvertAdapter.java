package com.tech.playin.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.tech.playin.R;
import com.tech.playin.util.RecyclerItemLisener;
import com.tech.playinsdk.model.entity.Advert;

import java.util.List;

public class AdvertAdapter extends RecyclerView.Adapter implements View.OnClickListener {

    private final Context context;
    private final List<Advert> data;
    private final RecyclerItemLisener<Advert> listener;

    public AdvertAdapter(Context context, List<Advert> data, RecyclerItemLisener<Advert> listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.adapter_advert, parent, false);
        return new GameListHodler(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        GameListHodler gameListHodler = (GameListHodler) holder;
        gameListHodler.itemView.setOnClickListener(this);
        gameListHodler.setData(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public void onClick(View v) {
        if (null != listener) {
            Advert ln = (Advert) v.getTag();
            listener.onItemClick(v, ln);
        }
    }

    private static class GameListHodler extends RecyclerView.ViewHolder {

        private ImageView image;
        private ImageView cache;
        private TextView name;
        private TextView info;

        public GameListHodler(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            cache = itemView.findViewById(R.id.cache);
            name = itemView.findViewById(R.id.name);
            info = itemView.findViewById(R.id.info);
        }

        public void setData(Advert advert) {
            itemView.setTag(advert);
            String typeName = advert.getOsType() == 1 ? "ios" : "android ";
            name.setText(advert.getAppName());
            info.setText(typeName + " - " + advert.getAdId());
            if (TextUtils.isEmpty(advert.getVideoPath())) {
                cache.setImageDrawable(null);
            } else {
                cache.setImageResource(R.drawable.finish);
            }
            Glide.with(itemView.getContext()).load(advert.getAppIcon()).centerCrop().into(image);
        }
    }
}
