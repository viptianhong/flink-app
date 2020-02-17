package com.th.dispatch.pojo;

/**
 * 派单状态枚举，不考虑接单后的销单
 */
public enum DispatchStateEnum {
	INITIAL(0, "初始"),
	ACCEPT(1, "司机接单"),
	CANCEL_BY_USER(2, "接单前用户取消"),
	CANCEL_BY_DRIVER(3, "接单前司机取消");

	private int code;
	private String desc;

	DispatchStateEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}
}