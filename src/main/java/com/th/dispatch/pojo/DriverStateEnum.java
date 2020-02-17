package com.th.dispatch.pojo;

public enum DriverStateEnum {
	IDLE(0, "空闲"),
	OFFLINE(1, "下线"),
	BUSY(2, "服务中");

	private int code;
	private String desc;

	DriverStateEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}
}
