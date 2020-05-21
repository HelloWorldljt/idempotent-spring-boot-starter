package com.xiangshang.extension.idempotent.extension;


import org.apache.commons.lang3.StringUtils;

/**
@description 幂等性上下文处理器
@author tanyuanpeng
@date 2018/12/19
**/
public class IdempotentContextHolder {


	private static final ThreadLocal<String> SERIAL_THREAD_LOCAL = new ThreadLocal<>();

	private static final ThreadLocal<String> IDEMPOTENT_KEY_THREAD_LOCAL = new ThreadLocal<>();

	public static String getSerialNumber() {
		return SERIAL_THREAD_LOCAL.get();
	}

	public static void setSerialNumber(String serialNumber) {
		String existSerialNumber = getSerialNumber();
		if (StringUtils.isNotBlank(existSerialNumber)){
			throw new IllegalIdempotentArgumentException("the thread local have exist serialNumber,nested calls are currently not supported");
		}
		SERIAL_THREAD_LOCAL.set(serialNumber);
	}

	public static void removeSerialNumber() {
		SERIAL_THREAD_LOCAL.remove();
	}

	public static String getIdempotentKey(){
		return IDEMPOTENT_KEY_THREAD_LOCAL.get();
	}

	public static void setIdempotentKey(String idempotentKey){
		String existIdempotentKey = getIdempotentKey();
		if (StringUtils.isNotBlank(existIdempotentKey)){
			throw new IllegalIdempotentArgumentException("the thread local have exist idempotentKey,nested calls are currently not supported");
		}
		IDEMPOTENT_KEY_THREAD_LOCAL.set(idempotentKey);
	}

	public static void removeIdempotentKey(){
		IDEMPOTENT_KEY_THREAD_LOCAL.remove();
	}


}
