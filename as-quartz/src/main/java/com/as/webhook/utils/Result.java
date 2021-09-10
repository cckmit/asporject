package com.as.webhook.utils;

import com.as.webhook.enums.ResultEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {

    private int code;
    private String message;

    public static Result success() {
        return Result.builder().code(ResultEnum.SUCCESS.getCode()).message(ResultEnum.SUCCESS.getMessage()).build();
    }


    public static Result error() {
        return Result.builder().code(ResultEnum.FAILED.getCode()).message(ResultEnum.FAILED.getMessage()).build();
    }


    public static Result result(ResultEnum messageEnum) {
        return Result.builder().code(messageEnum.getCode()).message(messageEnum.getMessage()).build();
    }

    public static Result result(ResultEnum messageEnum, String msg) {
        return Result.builder().code(messageEnum.getCode()).message(msg).build();
    }
}
