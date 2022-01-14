package com.as.quartz.util;

import com.as.common.utils.StringUtils;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient工具类
 *
 * @author kolin
 */
public class OkHttpUtils {

    private static OkHttpClient client = null;

    //单例模式创建OkHttpClient实例，避免重复创建造成资源浪费
    public static OkHttpClient getInstance() {
        if (StringUtils.isNull(client)) {
            synchronized (OkHttpUtils.class) {
                if (StringUtils.isNull(client)) {
                    client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)//讀取超時
                            .writeTimeout(30, TimeUnit.SECONDS)//寫入超時
                            .connectionPool(new ConnectionPool(64, 5, TimeUnit.MINUTES))//自定義連線池最大空嫌連接數和等待時間大小，否則默認最大5個空間連接'
                            .dns(new XDns(30, TimeUnit.SECONDS))
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return client;
    }
}