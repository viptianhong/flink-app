package com.th.dispatch.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟司机
 */
public class DriverTable {
	public static List<Driver> driverList = new ArrayList<>();

	static {
		Driver driver = new Driver();
		driver.setCityId(1);
		driver.setDriverId("BJ9072");
		driver.setDriverState(DriverStateEnum.IDLE.getCode());
		driver.setLat(39.939433);
		driver.setLng(116.459752);

		driverList.add(driver);
	}

	public static List<Driver> getNearbyIdleDriver(int cityId, double lng, double lat) {
		//todo 模拟，可以依赖mongo
		return driverList;
	}
}