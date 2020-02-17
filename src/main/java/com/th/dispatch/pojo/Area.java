package com.th.dispatch.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Area {
	private String index;
	private int cityId;
	private double centerLng;
	private double centerLat;
	private double radius;
	private String name;
	private List<PointNode> boundary;
}