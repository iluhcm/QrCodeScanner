package com.kaola.qrcodescanner.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.kaola.qrcodescanner.R;
import com.kaola.qrcodescanner.qrcode.camera.CameraManager;
import com.kaola.qrcodescanner.qrcode.decode.CaptureActivityHandler;
import com.kaola.qrcodescanner.qrcode.decode.DecodeManager;
import com.kaola.qrcodescanner.qrcode.decode.InactivityTimer;
import com.kaola.qrcodescanner.qrcode.view.QrCodeFinderView;

import java.io.IOException;

/**
 * Created by xingli on 12/26/15.
 * <p/>
 * 二维码扫描类。
 */
public class QrCodeActivity extends Activity implements Callback, OnClickListener {

    public static final String INTENT_OUT_STRING_SCAN_RESULT = "scan_result";
    private static final String INTENT_IN_INT_SUPPORT_TYPE = "support_type";
    private static final int REQUEST_PERMISSIONS = 1;
    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
    private InactivityTimer mInactivityTimer;
    private QrCodeFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private View mLlFlashLight;
    private boolean mNeedFlashLightOpen = true;
    private ImageView mIvFlashLight;
    private TextView mTvFlashLightText;
    private DecodeManager mDecodeManager = new DecodeManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        initView();
        initData();
    }

    private void initView() {
        final TextView tvPic = (TextView) findViewById(R.id.qr_code_header_black_pic);
        mIvFlashLight = (ImageView) findViewById(R.id.qr_code_iv_flash_light);
        mTvFlashLightText = (TextView) findViewById(R.id.qr_code_tv_flash_light);
        mQrCodeFinderView = (QrCodeFinderView) findViewById(R.id.qr_code_view_finder);
        mSurfaceView = (SurfaceView) findViewById(R.id.qr_code_preview_view);
        mLlFlashLight = findViewById(R.id.qr_code_ll_flash_light);
        mHasSurface = false;
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                mIvFlashLight.setOnClickListener(QrCodeActivity.this);
                tvPic.setOnClickListener(QrCodeActivity.this);
            }
        }, 1000);
    }

    private void initData() {
        CameraManager.init();
        mInactivityTimer = new InactivityTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            if (mHasSurface) {
                initCamera(surfaceHolder);
            } else {
                surfaceHolder.addCallback(this);
                surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            try {
                mCaptureActivityHandler.quitSynchronously();
                mCaptureActivityHandler = null;
                mHasSurface = false;
                mSurfaceView.getHolder().removeCallback(this);
                CameraManager.get().closeDriver();
            } catch (Exception e) {
                // 关闭摄像头失败的情况下,最好退出该Activity,否则下次初始化的时候会显示摄像头已占用.
                finish();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        findViewById(R.id.qr_code_view_background).setVisibility(View.VISIBLE);
        mQrCodeFinderView.setVisibility(View.GONE);
        mDecodeManager.showPermissionDeniedDialog(this);
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     */
    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            String resultString = result.getText();
            handleResult(resultString);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (!CameraManager.get().openDriver(surfaceHolder)) {
                showPermissionDeniedDialog();
                return;
            }
        } catch (IOException e) {
            // 基本不会出现相机不存在的情况
            Toast.makeText(this, getString(R.string.qr_code_camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            showPermissionDeniedDialog();
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);
        mLlFlashLight.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        turnFlashLightOff();
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this);
        }
    }

    private void restartPreview() {
        if (null != mCaptureActivityHandler) {
            try {
                mCaptureActivityHandler.restartPreviewAndDecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.qr_code_iv_flash_light:
                if (mNeedFlashLightOpen) {
                    turnFlashlightOn();
                } else {
                    turnFlashLightOff();
                }
                break;
        }
    }

    private void turnFlashlightOn() {
        try {
            CameraManager.get().setFlashLight(true);
            mNeedFlashLightOpen = false;
            mTvFlashLightText.setText(getString(R.string.qr_code_close_flash_light));
            mIvFlashLight.setBackgroundResource(R.drawable.flashlight_turn_off);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnFlashLightOff() {
        try {
            CameraManager.get().setFlashLight(false);
            mNeedFlashLightOpen = true;
            mTvFlashLightText.setText(getString(R.string.qr_code_open_flash_light));
            mIvFlashLight.setBackgroundResource(R.drawable.flashlight_turn_on);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0) {
            int cameraPermission = grantResults[0];
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
                if (mHasSurface) {
                    initCamera(surfaceHolder);
                } else {
                    surfaceHolder.addCallback(this);
                    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                        REQUEST_PERMISSIONS);
            }
        }
    }

    private void handleResult(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            mDecodeManager.showResultDialog(this, resultString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    restartPreview();
                }
            });
        }
    }
}