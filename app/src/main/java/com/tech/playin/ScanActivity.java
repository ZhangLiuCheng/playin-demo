package com.tech.playin;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.tech.playin.util.CodeScanningUtil;
import com.tech.playinsdk.util.PILog;


public class ScanActivity extends CaptureActivity implements View.OnClickListener {

    private static final int REQUEST_ALBUM = 101;
    private ImageScanningTask scanningTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.album).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.album:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_ALBUM);

//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("image/*");
//                startActivityForResult(intent, REQUEST_ALBUM);
                break;
            default:
                break;
        }
    }

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_scan);
        return (DecoratedBarcodeView) findViewById(R.id.zxingBarcodeScanner);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && REQUEST_ALBUM == requestCode && null != data) {
            Uri uri = data.getData();
            scanningTask = new ImageScanningTask( uri);
            scanningTask.execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != scanningTask) {
            scanningTask.cancel(true);
        }
    }

    public class ImageScanningTask extends AsyncTask<Uri, Void, Result> {
        private Uri uri;

        public ImageScanningTask(Uri uri) {
            this.uri = uri;
        }

        @Override
        protected Result doInBackground(Uri... params) {
            return CodeScanningUtil.scanImage(ScanActivity.this, uri);
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (!isCancelled()) {
                PILog.e("onPostExecute   "  + result);
                if (null != result) {
                    String qrCode = result.getText();
                    Intent intent = new Intent();
                    intent.putExtra("BarCodeResult", qrCode);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(ScanActivity.this, "invalid qr code", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
