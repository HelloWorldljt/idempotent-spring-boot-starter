package com.xiangshang.extension.idempotent.extension;

import java.util.ArrayList;
import java.util.List;

/**
 * 异常处理器
 * @author tanyuanpeng
 * @Date 2019/8/12 19:29
 **/
public class ExceptionHandler {


    private final static List<Class> idempotentExceptionList = new ArrayList<>();

    static {
        idempotentExceptionList.add(BizUnKnownStatusExceptioin.class);
        idempotentExceptionList.add(IllegalIdempotentArgumentException.class);
        idempotentExceptionList.add(ProcessingIdempotentStatusException.class);
        idempotentExceptionList.add(UnsupportedIdempotentStatusException.class);
    }


    /**
     * 判断是否幂等插件异常
     * @param ex
     * @return
     */
    public static boolean isIdempotentException(Exception ex){
        for (Class clazz : idempotentExceptionList) {
            if (ex.getClass().isAssignableFrom(clazz)){
                return true;
            }
        }
        return false;
    }

}
