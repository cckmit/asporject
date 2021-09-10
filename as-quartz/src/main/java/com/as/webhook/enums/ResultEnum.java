package com.as.webhook.enums;

import lombok.Getter;

/**
 * @author dongyang.hu
 * @date 2019/12/7 17:18
 */
@Getter
public enum ResultEnum {

    /**
     * success
     */
    SUCCESS(0, "success"),

    /**
     * failed
     */
    FAILED(-1, "failed"),

    /**
     * send email failed
     */
    LOG_ERROR(1001, "save log failed"),

    /**
     * send tg failed
     */
    TG_ERROR(1002, "send telegram failed"),

    /**
     * send email failed
     */
    MAIL_ERROR(1003, "send mail failed"),

    /**
     * email empty
     */
    MAIL_EMPTY(1004, "mailAdd can not be empty"),

    /**
     * tg empty
     */
    TG_EMPTY(1005, "tgId can not be empty"),

    /**
     * job empty
     */
    JOB_EMPTY(1006, "sql job not exist"),

    /**
     * elastic empty
     */
    ELASTIC_EMPTY(1007, "elastic job not exist"),

    /**
     * api empty
     */
    API_EMPTY(1008, "api job not exist"),

    /**
     * user not exist
     */
    USER_EMPTY(1009, "Reporter does not exist"),

    ;
    private final int code;
    private final String message;

    ResultEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return this.getCode() == SUCCESS.getCode();
    }
}
