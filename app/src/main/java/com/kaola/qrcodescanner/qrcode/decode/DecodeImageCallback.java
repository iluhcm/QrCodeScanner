package com.kaola.qrcodescanner.qrcode.decode;

import com.google.zxing.Result;

/**
 * Created by xingli on 1/4/16.
 *
 * 图片解析二维码回调方法
 */
public interface DecodeImageCallback {

    void decodeSucceed(Result result);

    void decodeFail(int type, String reason);
}
