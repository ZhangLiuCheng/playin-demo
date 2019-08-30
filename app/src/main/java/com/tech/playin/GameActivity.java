package com.tech.playin;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.tech.playinsdk.PlayInView;
import com.tech.playinsdk.listener.PlayListener;
import com.tech.playinsdk.model.entity.Advert;
import com.tech.playinsdk.util.PILog;

public class GameActivity extends AppCompatActivity implements VideoFragment.VideoFragmentListener {

    private final String TAG_VIDEO_FRAGMENT = "tagVideoFragment";
    private final Handler handler = new Handler();

    private Advert advert;
    private boolean playing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        getSupportFragmentManager().beginTransaction()
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
        Advert ad = advert;
        PlayInView playView = findViewById(R.id.playView);
        playView.play(ad.getAdId(), ad.getAppName(), ad.getAppIcon(),
                ad.getAppCover(), ad.getGoogleplayUrl(), 120, 2, new PlayListener() {
            @Override
            public void onPlaystart() {
                PILog.v("试玩成功");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        removeVideoFragment();
                    }
                });
            }

            @Override
            public void onPlayClose() {
                finish();
            }

            @Override
            public void onPlayError(Exception ex) {
                PILog.e("onPlayError " + ex);
                playing = false;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        removeVideoFragment();
                    }
                });
            }
        });
    }

    @Override
    public void onVideoFinish() {
        if (!playing) {
            finish();
        }
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
}
