package com.tech.playin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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

    private void playGame() {
        final Advert ad = advert;
        boolean audioState = true;
        VideoFragment fragment = (VideoFragment) getSupportFragmentManager().findFragmentByTag(TAG_VIDEO_FRAGMENT);
        if (null != fragment) {
            audioState = fragment.audioState();
        }
        final PlayInView playView = findViewById(R.id.playView);
        playView.play(ad.getAdId(), 100, this);
        playView.setAudioOpen(audioState);
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
        ToggleButton voiceTb = findViewById(com.tech.playinsdk.R.id.audioTb);
        voiceTb.setChecked(audioState);
        voiceTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                playView.setAudioOpen(isChecked);
            }
        });
    }

    private void initFinshView() {
        mPlayInfoView.setVisibility(View.GONE);
        mPlayEndView.setVisibility(View.VISIBLE);
        ImageView gameIconIv = findViewById(R.id.finishGameIconIv);
        TextView gameNameTv = findViewById(R.id.findhGameNameTv);
        Glide.with(this).load(advert.getAppIcon()).centerCrop().into(gameIconIv);
        gameNameTv.setText(advert.getAppName());
        findViewById(R.id.finishInstallBtn).setOnClickListener(this);
        findViewById(R.id.closeGameBtn).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        int clickId = v.getId();
        Advert ad = advert;

        if (clickId == R.id.finishInstallBtn || clickId == R.id.installBtn) {
            downloadApp(ad.getGoogleplayUrl());
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
        }
    }

    @Override
    public void onPlaystart() {
        removeVideoFragment();
    }

    @Override
    public void onPlayFinish() {
        PlayLog.e("onPlayFinish ");
        initFinshView();
    }

    @Override
    public void onPlayClose() {
        finish();
    }

    @Override
    public void onPlayError(Exception ex) {
        PlayLog.e("onPlayError  " + ex.getMessage());
        playing = false;
        removeVideoFragment();
//        if (ex instanceof HttpException) {
//            showErrorDialog(ex.getMessage());
//        }
        initFinshView();
    }

    @Override
    public void onPlayTime(int i) {
        TextView playTimeTv = findViewById(R.id.playTimeTv);
        playTimeTv.setText(i + "s | Exit PlayIN");
        if (i <= 0) {
            initFinshView();
        }
    }

    @Override
    public void onPlayInstall(String url) {
        downloadApp(url);
    }

    @Override
    public void onPlayForceTime() {

    }

    private void showErrorDialog(String title) {
        if (isFinishing()) return;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Exception, click confirm to return")
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void downloadApp(String url) {
        if (TextUtils.isEmpty(url) || "null".equals(url)) {
            Toast.makeText(this, "There is no googlePlay download url", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            finish();
        } catch (Exception ex) {
            ex.printStackTrace();
            PlayLog.e("download app error：" + ex);
        }
    }
}
