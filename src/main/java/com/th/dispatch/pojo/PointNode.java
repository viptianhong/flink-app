package com.th.dispatch.pojo;

import com.th.dispatch.utils.DecimalUtil;

public class PointNode {
	public Double longitude;
	public Double latitude;

	public PointNode(Double longitude, Double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PointNode pointNode = (PointNode) o;
		if (DecimalUtil.compareDouble(longitude, pointNode.longitude) != 0) return false;
		if (DecimalUtil.compareDouble(latitude, pointNode.latitude) != 0) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = longitude.hashCode();
		result = 31 * result + latitude.hashCode();
		return result;
	}

	public String toString() {
		return "{ longitude : " + longitude + ", latitude : " + latitude + " }";
	}
}