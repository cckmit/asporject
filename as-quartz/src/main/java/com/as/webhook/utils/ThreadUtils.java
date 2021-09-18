package com.as.webhook.utils;

import com.as.common.utils.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ExecutorService 工具类
 *
 * @author kolin
 */
public class ThreadUtils {

    private static ExecutorService executorService = null;

    //单例模式创建ExecutorService 实例，避免重复创建造成资源浪费
    public static ExecutorService getInstance() {
        if (StringUtils.isNull(executorService)) {
            synchronized (ThreadUtils.class) {
                if (StringUtils.isNull(executorService)) {
                    executorService = Executors.newCachedThreadPool();
                }
            }
        }
        return executorService;
    }
}