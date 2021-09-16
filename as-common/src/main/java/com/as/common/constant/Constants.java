package com.as.common.constant;

/**
 * 通用常量信息
 * 
 * @author kolin
 */
public class Constants
{
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 通用异常标识
     */
    public static final String ERROR = "2";

    /**
     * 是
     */
    public static final String YES = "Y";

    /**
     * 否
     */
    public static final String NO = "N";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";
    
    /**
     * 系统用户授权缓存
     */
    public static final String SYS_AUTH_CACHE = "sys-authCache";

    /**
     * 参数管理 cache name
     */
    public static final String SYS_CONFIG_CACHE = "sys-config";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache name
     */
    public static final String SYS_DICT_CACHE = "sys-dict";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi://";

    /**
     * 1.0平台
     */
    public static final String PLATFORM_1 = "1.0";

    /**
     * 5.0平台
     */
    public static final String PLATFORM_2 = "5.0";

    /**
     * sql job push template
     */
    public static final String DESCR_TEMPLATE_JOB = "descr_template_job";

    /**
     * elastic job push template
     */
    public static final String DESCR_TEMPLATE_ELASTIC = "descr_template_elastic";

    /**
     * api job push template
     */
    public static final String DESCR_TEMPLATE_API = "descr_template_api";

    /**
     * webhook push template
     */
    public static final String DESCR_TEMPLATE_WEBHOOK = "descr_template_webhook";

    /**
     * webhook mail push template
     */
    public static final String MAIL_TEMPLATE_WEBHOOK = "mail_template_webhook";
}
