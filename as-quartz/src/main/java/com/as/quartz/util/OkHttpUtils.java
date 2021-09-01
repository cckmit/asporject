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
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .connectionPool(new ConnectionPool(32, 5, TimeUnit.MINUTES))
                            .dns(new XDns(60, TimeUnit.SECONDS))
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return client;
    }
}