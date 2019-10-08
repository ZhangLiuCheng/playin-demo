package com.tech.playin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.tech.playinsdk.PlayInView;
import com.tech.playinsdk.http.HttpException;
import com.tech.playinsdk.listener.PlayListener;
import com.tech.playinsdk.model.entity.Advert;
import com.tech.playinsdk.util.PlayLog;

public class GameActivity extends AppCompatActivity implements VideoFragment.VideoFragmentListener, PlayListener {

    private final String TAG_VIDEO_FRAGMENT = "tagVideoFragment";
    private final Handler handler = new Handler();

    private Advert advert;
    private boolean playing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        Advert ad = advert;
        PlayInView playView = findViewById(R.id.playView);
        playView.play(ad.getAdId(), 30, this);

    }

    @Override
    public void onVideoFinish() {
//        if (!playing) {
//            finish();
//        }
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
    public void onPlayClose() {
        finish();
    }

    @Override
    public void onPlayError(Exception ex) {
        PlayLog.e("onPlayError  " + ex.getMessage());
        playing = false;
        removeVideoFragment();
        if (ex instanceof HttpException) {
            showErrorDialog(ex.getMessage());
        }
    }

    @Override
    public void onPlayDownload(String url) {
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
}
