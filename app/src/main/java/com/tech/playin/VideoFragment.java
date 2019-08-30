package com.tech.playin;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.tech.playin.widget.CountdownView;
import com.tech.playinsdk.PlayInSdk;
import com.tech.playinsdk.http.HttpException;
import com.tech.playinsdk.http.HttpListener;
import com.tech.playinsdk.model.entity.Advert;
import com.tech.playinsdk.util.PILog;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoFragment extends Fragment implements SurfaceHolder.Callback, IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnVideoSizeChangedListener {

    private static final String ARG_PARAM = "param_advert";

    private final Handler handler = new Handler();
    private VideoFragmentListener mListener;
    private Advert advert;
    private IjkMediaPlayer ijkMediaPlayer;

    private SurfaceView surfaceView;
    private ImageView coverView;
    private CountdownView countdownView;

    public static VideoFragment newInstance(Advert advert) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM, advert);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            advert = (Advert) getArguments().getSerializable(ARG_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        surfaceView = getView().findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
        coverView = getView().findViewById(R.id.coverView);
        countdownView = getView().findViewById(R.id.videoCountView);

        if (TextUtils.isEmpty(advert.getVideoUrl())) {
            initCoverView();
        } else {
            initIjkPlayer();
        }
        countdownNow();
        checkAvaliable();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof VideoFragmentListener) {
            mListener = (VideoFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement VideoFragmentListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.pause();
        }
        countdownView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.start();
        }
        countdownView.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        handler.removeCallbacksAndMessages(null);
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.release();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.setDisplay(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        initVoiceView();
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {
        // 防止多次调用
        if (i2 != 0) {
            return;
        }
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        int adapterHeight = dm.widthPixels * i1 / i;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
        params.width = dm.widthPixels;
        params.height = adapterHeight;
        surfaceView.setLayoutParams(params);
    }

    // 加载封面
    private void initCoverView() {
        coverView.setVisibility(View.VISIBLE);
        Glide.with(coverView.getContext()).load(advert.getAppCover()).centerCrop().into(coverView);
    }

    // 加载声音
    private void initVoiceView() {
        ToggleButton voiceTb = getView().findViewById(R.id.voice);
        voiceTb.setVisibility(View.VISIBLE);
        voiceTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (null != ijkMediaPlayer) {
                    float volume = isChecked ? 1 : 0;
                    ijkMediaPlayer.setVolume(volume, volume);
                }
            }
        });
    }

    private void initIjkPlayer() {
        surfaceView.setVisibility(View.VISIBLE);
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            ijkMediaPlayer = new IjkMediaPlayer();
            String url = TextUtils.isEmpty(advert.getVideoPath()) ? advert.getVideoUrl() : advert.getVideoPath();
            PILog.v("视频播放地址: " + url);
            ijkMediaPlayer.setDataSource(url);
            ijkMediaPlayer.setOnPreparedListener(this);
            ijkMediaPlayer.setOnVideoSizeChangedListener(this);
            ijkMediaPlayer.prepareAsync();
            ijkMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            ijkMediaPlayer = null;
        }
    }

    private void countdownNow() {
        getView().findViewById(R.id.funcLayout).setVisibility(View.VISIBLE);
        countdownView.countDown(advert.getVideoTime(), new CountdownView.CountdownListener() {
            @Override
            public void countfinish() {
                if (null != mListener) {
                    mListener.onVideoFinish();
                }
            }
        });
    }

    // 检查是否可以试玩
    private void checkAvaliable() {
        PlayInSdk.getInstance().checkAvailable(advert.getAdId(), new HttpListener<Boolean>() {
            @Override
            public void success(Boolean result) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isVisible()) {
                            initPlayNow();
                        }
                    }
                }, 5000);
            }
            @Override
            public void failure(HttpException e) {
                PILog.e("[PlayIn] checkAvaliable: internal Error: " + e);
            }
        });
    }


    private void initPlayNow() {
        Button playNowBtn = getView().findViewById(R.id.playNow);
        playNowBtn.setVisibility(View.VISIBLE);
        playNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onPlayGame();
                }
            }
        });
    }

    public interface VideoFragmentListener {
        void onVideoFinish();
        void onPlayGame();
    }
}
