package com.th.dispatch.pojo;

import com.th.dispatch.utils.DecimalUtil;
import com.th.dispatch.utils.GrahamScanUtil;
import java.util.*;

/**
 * 城市分割的区域
 */
public class AreaTable {
	public static Map<Integer, Map<String, Area>> areaMap = new HashMap<Integer, Map<String, Area>>();

	static {
		Area area = new Area();
		area.setIndex("123456");
		area.setCenterLat(39.939443);
		area.setCenterLng(116.459732);
		area.setCityId(1);
		area.setName("北京三里屯");
		area.setRadius(3.0);
		List<PointNode> sanlitunAreaBoundary = new ArrayList<>();
		sanlitunAreaBoundary.add(new PointNode(116.457288, 39.94149));
		sanlitunAreaBoundary.add(new PointNode(116.457144, 39.938281));
		sanlitunAreaBoundary.add(new PointNode(116.460235, 39.937286));
		sanlitunAreaBoundary.add(new PointNode(116.463037, 39.938115));
		sanlitunAreaBoundary.add(new PointNode(116.463253, 39.939775));
		sanlitunAreaBoundary.add(new PointNode(116.46045, 39.94232));
		area.setBoundary(sanlitunAreaBoundary);
		Map<String, Area> map = new HashMap<>();
		map.put(area.getIndex(), area);
		areaMap.put(1, map);
	}

	public static Area getArea(int cityId, double lng, double lat) {
		Map<String, Area> map = areaMap.get(cityId);
		if (map != null) {
			Collection<Area> areas =  map.values();
			if (areas != null && areas.size() > 0) {
				lng = DecimalUtil.roundingHalfUp(lng, 6);
				lat = DecimalUtil.roundingHalfUp(lat, 6);
				for (Area area : areas) {
					if (GrahamScanUtil.isInConvexHull(area.getBoundary(), new PointNode(lng, lat))) {
						return area;
					}
				}
			}
		}
		return null;
	}

	public static void main(String[] args) {
		List<PointNode> boundary = GrahamScanUtil.getConvexHull(areaMap.get(1).get("123456").getBoundary());
//		System.out.println(boundary);
		Area area = getArea(1, 116.459732, 39.939443);
		System.out.println(area);
	}
}