package com.tech.playin.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.tech.playinsdk.model.entity.Advert;

import java.util.ArrayList;
import java.util.List;

public class Tool {

    /**
     * 移除没有googlePaly下载地址的游戏 并且 去重
     */
    public static List<Advert> filterAdvertList(List<Advert> advertList) {
        List<Advert> srcList = advertList;
        List<Advert> destList = new ArrayList<>();
        Advert ad;
        for (int i = 0; i < srcList.size(); i++) {
            ad = srcList.get(i);
            String url = ad.getGoogleplayUrl();
            if (url != null && !url.equals("null") && url.length() > 0 && !destList.contains(ad)) {
                destList.add(ad);
            }
        }
        return destList;
    }

    /**
     * 获取当前版本号.
     * @param context
     * @return
     */
    public static String getVerName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
