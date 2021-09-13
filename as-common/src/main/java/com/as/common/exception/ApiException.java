package com.as.common.exception;

/**
 * 接口调用异常
 *
 * @author Divid
 */
public class ApiException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    protected final String message;

    public ApiException(String message)
    {
        this.message = message;
    }

    public ApiException(String message, Throwable e)
    {
        super(message, e);
        this.message = message;
    }

    @Override
    public String getMessage()
    {
        return message;
    }
}
