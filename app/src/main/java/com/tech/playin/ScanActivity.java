package com.tech.playin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.tech.playinsdk.util.PILog;

import org.json.JSONObject;

import cn.simonlee.xcodescanner.core.CameraScanner;
import cn.simonlee.xcodescanner.core.GraphicDecoder;
import cn.simonlee.xcodescanner.core.NewCameraScanner;
import cn.simonlee.xcodescanner.core.OldCameraScanner;
import cn.simonlee.xcodescanner.core.ZBarDecoder;
import cn.simonlee.xcodescanner.view.AdjustTextureView;

public class ScanActivity extends AppCompatActivity implements CameraScanner.CameraListener,
        TextureView.SurfaceTextureListener, GraphicDecoder.DecodeListener, View.OnClickListener {

    private static final int REQUEST_ALBUM = 101;

    private AdjustTextureView mTextureView;
    private View mScannerFrameView;

    private CameraScanner mCameraScanner;
    protected GraphicDecoder mGraphicDecoder;

    private int[] mCodeType = new int[]{ZBarDecoder.CODABAR, ZBarDecoder.CODE39, ZBarDecoder.CODE93, ZBarDecoder.CODE128, ZBarDecoder.DATABAR, ZBarDecoder.DATABAR_EXP
            , ZBarDecoder.EAN8, ZBarDecoder.EAN13, ZBarDecoder.I25, ZBarDecoder.ISBN10, ZBarDecoder.ISBN13, ZBarDecoder.PDF417, ZBarDecoder.QRCODE
            , ZBarDecoder.UPCA, ZBarDecoder.UPCE};

    private int brightnessCount = 0;
    private int scanCount = 0;
    private String scanResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mTextureView = findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);
        mScannerFrameView = findViewById(R.id.scannerframe);

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.album).setOnClickListener(this);

        Intent intent = getIntent();
        if (intent.getBooleanExtra("newAPI", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraScanner = new NewCameraScanner(this);
        } else {
            mCameraScanner = new OldCameraScanner(this);
        }
    }

    @Override
    protected void onRestart() {
        if (mTextureView.isAvailable()) {
            mCameraScanner.setPreviewTexture(mTextureView.getSurfaceTexture());
            mCameraScanner.setPreviewSize(mTextureView.getWidth(), mTextureView.getHeight());
            mCameraScanner.openCamera(this.getApplicationContext());
        }
        super.onRestart();
    }

    @Override
    protected void onPause() {
        mCameraScanner.closeCamera();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCameraScanner.setGraphicDecoder(null);
        if (mGraphicDecoder != null) {
            mGraphicDecoder.setDecodeListener(null);
            mGraphicDecoder.detach();
        }
        mCameraScanner.detach();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraScanner.setPreviewTexture(surface);
        mCameraScanner.setPreviewSize(width, height);
        mCameraScanner.openCamera(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void openCameraSuccess(int frameWidth, int frameHeight, int frameDegree) {
        mTextureView.setImageFrameMatrix(frameWidth, frameHeight, frameDegree);
        if (mGraphicDecoder == null) {
            mGraphicDecoder = new ZBarDecoder(this, mCodeType);
        }
        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
        mCameraScanner.setGraphicDecoder(mGraphicDecoder);
    }

    @Override
    public void openCameraError() {
        Toast.makeText(this, "Camera on error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void noCameraPermission() {
        Toast.makeText(this, "No camera permission", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void cameraDisconnected() {
    }

    @Override
    public void cameraBrightnessChanged(int brightness) {
        if (brightness <= 50) {
            if (brightnessCount < 0) {
                brightnessCount = 1;
            } else {
                brightnessCount++;
            }
        } else {
            if (brightnessCount > 0) {
                brightnessCount = -1;
            } else {
                brightnessCount--;
            }
        }
    }

    private void codeValidate(String barCode) {
        PILog.e("二维码: " + barCode);
        try {
            String[] barArr = barCode.split("\\.");
            // 长度校验
            if (barArr.length != 3) {
                incorrectCode();
                return;
            }
            // json校验
            new JSONObject(new String(Base64.decode(barArr[0], Base64.DEFAULT)));
            new JSONObject(new String(Base64.decode(barArr[1], Base64.DEFAULT)));
            scanCodeSuccess(barCode);
        } catch (Exception ex) {
            ex.printStackTrace();
            incorrectCode();
        }
    }

    // 二维码不正确
    private void incorrectCode() {
        Toast.makeText(this, "Barcode is incorrect", Toast.LENGTH_SHORT).show();
    }

    // 成功获取到二维码
    private void scanCodeSuccess(String barCode) {
        Intent intent = new Intent();
        intent.putExtra("barCode", barCode);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void decodeComplete(String result, int type, int quality, int requestCode) {
        if (requestCode == REQUEST_ALBUM) {
            // 扫码相册
            if (!TextUtils.isEmpty(result)) {
                codeValidate(result);
            }
        } else {
            if (result == null || TextUtils.isEmpty(result)) return;
            // 直接扫码
            if (result.equals(scanResult)) {
                if (++scanCount > 2) {
                    codeValidate(scanResult);
                }
            } else {
                scanCount = 1;
                scanResult = result;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALBUM && resultCode == Activity.RESULT_OK && data != null) {
            if (mGraphicDecoder == null) {
                mGraphicDecoder = new ZBarDecoder(this, mCodeType);
            } else {
                mGraphicDecoder.setCodeTypes(mCodeType);
            }
            mGraphicDecoder.decodeForResult(this, data.getData(), REQUEST_ALBUM);
        }
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
                break;
            default:
                break;
        }
    }
}
