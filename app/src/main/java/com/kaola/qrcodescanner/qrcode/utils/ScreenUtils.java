package com.kaola.qrcodescanner.qrcode.utils;

import android.content.Context;
import android.util.DisplayMetrics;

import com.kaola.qrcodescanner.qrcode.QrCodeApplication;

/**
 * ScreenUtils
 * <ul>
 * <strong>Convert between dp and sp</strong>
 * </ul>
 *
 * @author <a href="http://www.trinea.cn" target="_blank">Trinea</a> 2014-2-14
 */
public class ScreenUtils {

    private ScreenUtils() {
        throw new AssertionError();
    }

    /**
     * 获取屏幕宽度
     *
     * @return
     */
    public static int getScreenWidth() {
        Context context = QrCodeApplication.sAppContext;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @return
     */
    public static int getScreenHeight() {
        Context context = QrCodeApplication.sAppContext;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }
}
