package com.as.webhook.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.as.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestUtil {

    public static final Logger log = LoggerFactory.getLogger(HttpRequestUtil.class);

    /**
     * 通用请求格式转换,兼容form和json两种格式
     *
     * @param httpServletRequest
     * @return
     * @Author Divid
     * @Date 2020/03/06
     */
    public static Object commonHttpRequestParamConvert(HttpServletRequest httpServletRequest, Object classModel) {
        Map<String, String> params = new HashMap<>();
        Object tempClass = classModel;
        try {
            Map<String, String[]> requestParams = httpServletRequest.getParameterMap();
            if (requestParams != null && !requestParams.isEmpty()) {
                //form格式
                requestParams.forEach((key, value) -> params.put(key, value[0]));
            } else {
                //json格式
                StringBuilder paramSb = new StringBuilder();
                String str = "";
                BufferedReader br = httpServletRequest.getReader();
                while ((str = br.readLine()) != null) {
                    paramSb.append(str);
                }
                if (paramSb.length() > 0) {
                    JSONObject paramJsonObject = JSON.parseObject(paramSb.toString());
                    if (paramJsonObject != null && !paramJsonObject.isEmpty()) {
                        paramJsonObject.forEach((key, value) -> params.put(key, String.valueOf(value)));
                    }
                }
            }

            //将params匹配到对应model中
            Class<?> clz = tempClass.getClass();
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (params.containsKey(field.getName())) { //如果map集合存在与属性名相同的键
                    //判断数据类型
                    if ("java.lang.int".equals(field.getType().getCanonicalName())
                            || "java.lang.Integer".equals(field.getType().getCanonicalName())) {
                        field.set(tempClass, Integer.valueOf(params.get(field.getName())));
                    } else if ("java.lang.String".equals(field.getType().getCanonicalName())) {
                        field.set(tempClass, params.get(field.getName()));
                    } else if ("java.lang.long".equals(field.getType().getCanonicalName())
                            || "java.lang.Long".equals(field.getType().getCanonicalName())) {
                        field.set(tempClass, Long.valueOf(params.get(field.getName())));
                    } else if ("java.lang.double".equals(field.getType().getCanonicalName())
                            || "java.lang.Double".equals(field.getType().getCanonicalName())) {
                        field.set(tempClass, Double.valueOf(params.get(field.getName())));
                    } else if ("java.lang.boolean".equals(field.getType().getCanonicalName())
                            || "java.lang.Boolean".equals(field.getType().getCanonicalName())) {
                        field.set(tempClass, Boolean.valueOf(params.get(field.getName())));
                    }
//                    field.set(tempClass, params.get(field.getName())); //把属性值赋予给目标类对应属性
                }
            }

        } catch (Exception e) {
            log.error(httpServletRequest.getRemoteAddr() + "请求接口错误,数据为:" + params.toString());
            throw new ApiException("参数格式错误");
        }
        return tempClass;
    }
}
