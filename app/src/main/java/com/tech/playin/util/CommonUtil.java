package com.tech.playin.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.tech.playinsdk.model.entity.Advert;
import com.tech.playinsdk.util.PlayLog;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommonUtil {

    /**
     * 验证二维码是否为PlayIn的.
     * @param qrCode
     * @return
     */
    public static boolean codeValidate(String qrCode) {
        try {
            String[] barArr = qrCode.split("\\.");
            // 长度校验
            if (barArr.length != 3) {
                return false;
            }
            // json校验
            new JSONObject(new String(Base64.decode(barArr[0], Base64.DEFAULT)));
            new JSONObject(new String(Base64.decode(barArr[1], Base64.DEFAULT)));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

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

    public static void closeStream(Closeable... closeables) {
        for (Closeable stream: closeables) {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void showErrorDialog(final Activity activity, String title) {
        if (activity.isFinishing()) return;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage("Exception, click confirm to return")
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                }).create();
        dialog.setCancelable(false);
        dialog.show();
    }

    public static void downloadApp(final Activity activity, String url) {
        if (TextUtils.isEmpty(url) || "null".equals(url)) {
            Toast.makeText(activity, "There is no googlePlay download url", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            activity.startActivity(intent);
            activity.finish();
        } catch (Exception ex) {
            ex.printStackTrace();
            PlayLog.e("download app error：" + ex);
        }
    }
}
