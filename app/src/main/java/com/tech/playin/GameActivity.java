package com.tech.playin;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.tech.playin.util.CommonUtil;
import com.tech.playinsdk.PlayInView;
import com.tech.playinsdk.listener.PlayListener;
import com.tech.playinsdk.model.entity.Advert;
import com.tech.playinsdk.util.PlayLog;

public class GameActivity extends AppCompatActivity implements VideoFragment.VideoFragmentListener,
        View.OnClickListener, PlayListener {

    private final String TAG_VIDEO_FRAGMENT = "tagVideoFragment";
    private final Handler handler = new Handler();

    private RelativeLayout mPlayInfoView;
    private RelativeLayout mPlayEndView;

    private Advert advert;
    private boolean playing;
    private int count = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);
        advert = (Advert) getIntent().getSerializableExtra("advert");
        this.addVideoFragment(advert);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void addVideoFragment(Advert advert) {
        VideoFragment videoFragment = VideoFragment.newInstance(advert);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.videoContainer, videoFragment, TAG_VIDEO_FRAGMENT)
                .commit();
    }

    private void removeVideoFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_VIDEO_FRAGMENT);
        if (null != fragment) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    private void hideLoadingView() {
        findViewById(R.id.playLoadView).setVisibility(View.GONE);
    }

    private void playGame() {
        final Advert ad = advert;
        boolean audioState = true;
        VideoFragment fragment = (VideoFragment) getSupportFragmentManager().findFragmentByTag(TAG_VIDEO_FRAGMENT);
        if (null != fragment) {
            audioState = fragment.audioState();
        }
        final PlayInView playView = findViewById(R.id.playView);
        PlayLog.e("adid:" + ad.getAdId());
        playView.play(ad.getAdId(), this);
        playView.setAudioState(audioState);
        initInfoView(ad, audioState, playView);
    }

    private void initInfoView(final Advert ad, boolean audioState, final PlayInView playView) {
        mPlayInfoView = findViewById(R.id.playInfoView);
        mPlayEndView = findViewById(R.id.playEndView);
        ImageView gameIconIv = findViewById(R.id.gameIconIv);
        TextView gameNameTv = findViewById(R.id.gameNameTv);
        Glide.with(this).load(advert.getAppIcon()).centerCrop().into(gameIconIv);
        gameNameTv.setText(advert.getAppName());
        findViewById(R.id.installBtn).setOnClickListener(this);
        findViewById(R.id.playTimeTv).setOnClickListener(this);
        findViewById(R.id.closeInstallBtn).setOnClickListener(this);
        ToggleButton voiceTb = findViewById(R.id.audioTb);
        voiceTb.setChecked(audioState);
        voiceTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                playView.setAudioState(isChecked);
            }
        });

        Spinner quality = findViewById(R.id.quality);
        quality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlayInView playView = findViewById(R.id.playView);
                playView.setVideoQuality((int)(id + 1));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initFinshView() {
        int marginTop = 0;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
        } else {
            marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.maskLayout).getLayoutParams();
        params.setMargins(0, marginTop, 0 , 0);
        mPlayInfoView.setVisibility(View.GONE);
        mPlayEndView.setVisibility(View.VISIBLE);
        ImageView gameIconIv = findViewById(R.id.finishGameIconIv);
        TextView gameNameTv = findViewById(R.id.findhGameNameTv);
        Glide.with(this).load(advert.getAppIcon()).centerCrop().into(gameIconIv);
        gameNameTv.setText(advert.getAppName());
        findViewById(R.id.finishInstallBtn).setOnClickListener(this);
        findViewById(R.id.closeGameBtn).setOnClickListener(this);
    }

    private void startCountdown() {
        final TextView playTimeTv = findViewById(R.id.playTimeTv);
        playTimeTv.setText(count + "s | Exit PlayIN");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                count--;
                playTimeTv.setText(count + "s | Exit PlayIN");
                if (count > 0) {
                    handler.postDelayed(this, 1000);
                } else {
                    PlayInView playView = findViewById(R.id.playView);
                    playView.finish();
                }
            }
        }, 1000);
    }

    @Override
    public void onClick(View v) {
        int clickId = v.getId();
        Advert ad = advert;

        if (clickId == R.id.finishInstallBtn || clickId == R.id.installBtn) {
            CommonUtil.downloadApp(this, ad.getGoogleplayUrl());
        } else if (clickId == R.id.playTimeTv) {
            finish();
        } else if (clickId == R.id.closeGameBtn) {
            finish();
        } else if (clickId == R.id.closeInstallBtn) {
            findViewById(R.id.installView).setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoFinish() {
        finish();
    }

    @Override
    public void onPlayGame() {
        if (playing) {
            Toast.makeText(this, "Game start, please wait a moment", Toast.LENGTH_LONG).show();
            return;
        } else {
            playing = true;
            playGame();
            removeVideoFragment();
        }
    }

    @Override
    public void onPlayStart(int duration) {
//        removeVideoFragment();
        hideLoadingView();
        startCountdown();
    }

    @Override
    public void onPlayEnd(boolean manual) {
        PlayLog.e("onPlayFinish ");
        initFinshView();
    }

    @Override
    public void onPlayError(Exception ex) {
        PlayLog.e("onPlayError  " + ex.getMessage());
        playing = false;
        hideLoadingView();
        removeVideoFragment();
        initFinshView();
    }
}
