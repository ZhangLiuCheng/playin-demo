package com.tech.playin.util;

import android.view.View;

public interface RecyclerItemLisener<T> {

    void onItemClick(View view, T item);

}
