package com.tech.playin.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.tech.playinsdk.model.entity.Advert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO 为了方便，直接起个线程下载视频
public class DownloadUtil {

    public interface DownloadListener {
        void onDownloadUpdate(Advert advert);

        void onDownloadComplete();
    }

    public static void prepareVideoLocalPath(final Context context, final List<Advert> adverts,
                                             final DownloadListener listener) {
        final CountDownLatch latch = new CountDownLatch(adverts.size());
        ExecutorService exe = Executors.newCachedThreadPool();
        for (int i = 0; i < adverts.size(); i++) {
            final Advert ad = adverts.get(i);
            exe.submit(new Runnable() {
                @Override
                public void run() {
                    downloadVideo(context, ad, listener);
                    latch.countDown();
                }
            });
        }
        exe.shutdown();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await(5000, TimeUnit.MILLISECONDS);
//                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                listener.onDownloadComplete();
            }
        }).start();
    }

    public static void downloadVideo(Context context, Advert advert, DownloadListener listener) {
        if (TextUtils.isEmpty(advert.getVideoUrl())) {
            return;
        }
        File rootFile = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File videoFile = new File(rootFile, advert.getOsType() + getNameByUrl(advert.getVideoUrl()));
        if (videoFile.exists()) {
            advert.setVideoPath(videoFile.getAbsolutePath());
        } else {
            InputStream is = null;
            FileOutputStream fs = null;
            try {
                videoFile.createNewFile();
                URL url = new URL(advert.getVideoUrl());
                URLConnection conn = url.openConnection();
                is = conn.getInputStream();
                fs = new FileOutputStream(videoFile.getAbsoluteFile());

                byte[] buffer = new byte[1204];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    fs.write(buffer, 0, length);
                }
                advert.setVideoPath(videoFile.getAbsolutePath());
                listener.onDownloadUpdate(advert);
            } catch (Exception e) {
                videoFile.delete();
                e.printStackTrace();
            } finally {
                Common.closeStream(fs, is);
            }
        }
    }

    public static String getNameByUrl(String url) {
        try {
            String[] us = url.split("/");
            return us[us.length - 1];
        } catch (Exception ex) {
            return url.hashCode() + ".mp4";
        }
    }
}
