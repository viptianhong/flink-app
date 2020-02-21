package com.th.dispatch.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderMsg implements Serializable {

	private static final long serialVersionUID = -6957361951748382519L;

	//用户电话
	private String phone;
	//订单id
	private String orderId;
	//派单状态
	private int dispatchState;
	//城市id
	private int cityId;
	//下单经度
	private double lng;
	//下单纬度
	private double lat;
	//自定义区域索引
	private String areaIndex;
	//下单时间，13位毫秒时间戳
	private long createTime;
	//司机工号
	private String driverId;

//	//测试用
//	@Override
//	public String toString() {
//		return orderId + "->" + driverId;
//	}
}