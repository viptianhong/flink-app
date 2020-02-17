package com.th.dispatch.pojo;

import lombok.Data;

@Data
public class Driver {
	private String driverId;
	private int driverState;
	private int cityId;
	private double lng;
	private double lat;
}
