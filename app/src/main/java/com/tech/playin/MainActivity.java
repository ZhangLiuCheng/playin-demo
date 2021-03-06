package com.tech.playin;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.tech.playin.adapter.AdvertAdapter;
import com.tech.playin.util.Constants;
import com.tech.playin.util.DownloadUtil;
import com.tech.playin.util.RecyclerItemLisener;
import com.tech.playin.util.RecyclerLinearDivider;
import com.tech.playin.util.PreferencesUtil;
import com.tech.playin.util.Common;
import com.tech.playinsdk.PlayInSdk;
import com.tech.playinsdk.http.HttpException;
import com.tech.playinsdk.http.HttpListener;
import com.tech.playinsdk.listener.InitListener;
import com.tech.playinsdk.model.entity.Advert;

import java.util.List;

import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity implements RecyclerItemLisener<Advert>,
        DownloadUtil.DownloadListener {

    private String sdkKey;

    private Dialog loadingDialog;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingDialog = getLoadingDialog();

        initData();
        initListener();
    }

    private void initListener() {
        findViewById(R.id.reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
            }
        });
        findViewById(R.id.qrCode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrCode();
            }
        });
    }

    private void initData() {
        TextView verNameTv = findViewById(R.id.verName);
        verNameTv.setText("v " + Common.getVerName(this));
        sdkKey = PreferencesUtil.getSdkKey(this);

        // 固定渠道写死sdkkey
        if (!TextUtils.isEmpty(Constants.SDK_KEY)) {
            sdkKey = Constants.SDK_KEY;
        }
        if (TextUtils.isEmpty(sdkKey)) {
            Toast.makeText(this, "Please set key first", Toast.LENGTH_SHORT).show();
            return;
        }
        loadingDialog.show();
        PlayInSdk.getInstance().configWithKey(sdkKey, new InitListener() {
            @Override
            public void success() {
                getGameList();
            }

            @Override
            public void failure(Exception ex) {
                dismissLoadingDialog();
                showErrorDialog("Initialization failure");
                PreferencesUtil.setSdkKey(MainActivity.this, null);
            }
        });
    }

    private void initRecyclerView(List<Advert> adverts) {
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(OrientationHelper. VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new AdvertAdapter(this, adverts, this));
        recyclerView.addItemDecoration(new RecyclerLinearDivider(LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    // 获取游戏列表
    private void getGameList() {
        PlayInSdk.getInstance().userAppKeys(new HttpListener<List<Advert>>() {
            @Override
            public void success(List<Advert> result) {
                initRecyclerView(result);
                DownloadUtil.prepareVideoLocalPath(MainActivity.this, result, MainActivity.this);
            }

            @Override
            public void failure(HttpException ex) {
                dismissLoadingDialog();
                showErrorDialog("Failed to get list");
            }
        });
    }

    private void qrCode() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.CAMERA)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) {
                        if (granted) {
                            toScan();
                        } else {
                            Toast.makeText(MainActivity.this,"Need camera permission to scan code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onItemClick(View view, Advert item) {
        toGame(item);
    }

    private void toScan() {
        new IntentIntegrator(this)
                .setCaptureActivity(ScanActivity.class)
                .setPrompt("")
                .initiateScan();
    }

    private void toGame(Advert advert) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("advert", advert);
        startActivity(intent);
    }

    private void showErrorDialog(String messsage) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(messsage + ", Click ok to retry")
                .setNegativeButton("close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initData();
                    }
                }).create();
        dialog.setCancelable(false);
        if (isFinishing()) return;
        dialog.show();
    }

    private Dialog getLoadingDialog() {
        Dialog pd = new Dialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        pd.setContentView(view);
        pd.setCancelable(false);
        return pd;
    }

    private void dismissLoadingDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != loadingDialog) loadingDialog.dismiss();
            }
        });
    }

    private void updateRecyclerView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDownloadUpdate(Advert advert) {
        updateRecyclerView();
    }

    @Override
    public void onDownloadComplete() {
        dismissLoadingDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String barCodeResult = data.getStringExtra("BarCodeResult");
            if (!TextUtils.isEmpty(barCodeResult)) {
                // 相册选择二维码
                updateSdkKey(barCodeResult);
            } else {
                // 摄像头扫码
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if(result != null && result.getContents() != null) {
                    updateSdkKey(result.getContents());
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateSdkKey(String qrCode) {
        if (Common.codeValidate(qrCode)) {
            sdkKey = qrCode;
            PreferencesUtil.setSdkKey(MainActivity.this, qrCode);
            initData();
        } else {
            Toast.makeText(this, "invalid qr code", Toast.LENGTH_SHORT).show();
        }
    }
}
