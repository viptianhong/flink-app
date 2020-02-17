package com.th.dispatch.utils;

import com.th.dispatch.pojo.PointNode;

import java.util.*;

import static com.th.dispatch.utils.DecimalUtil.compareDouble;

public final class GrahamScanUtil {

	private enum Turn {
		CLOCKWISE, COUNTER_CLOCKWISE, COLLINEAR
	}

	private static boolean areAllCollinear(List<PointNode> points) {

		if (points.size() < 2) {
			return true;
		}

		final PointNode a = points.get(0);
		final PointNode b = points.get(1);

		for (int i = 2; i < points.size(); i++) {

			PointNode c = points.get(i);

			if (getTurn(a, b, c) != Turn.COLLINEAR) {
				return false;
			}
		}

		return true;
	}

	public static Boolean isInConvexHull(List<PointNode> points, PointNode pointNode) {
		//克隆points
		List<PointNode> clonePoints = new ArrayList<>();

		if (points != null && points.size() >= 3 && pointNode != null) {
			for (int i = 0; i < points.size(); i++) {
				if (pointNode.equals(points.get(i))) {
					return true;
				}
				clonePoints.add(points.get(i));
			}
			clonePoints.add(pointNode);
			List<PointNode> afterPoints = getConvexHull(clonePoints);
			for (int i = 0; i < afterPoints.size(); i++) {
				if (pointNode.equals(afterPoints.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 获取凸多边形
	 *
	 * @param points
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static List<PointNode> getConvexHull(List<PointNode> points) throws IllegalArgumentException {

		if (points == null || points.size() < 3) {
			throw new IllegalArgumentException("can only create a convex hull of 3 or more unique points");
		}

		List<PointNode> sorted = new ArrayList<PointNode>(getSortedPointSet(points));

		if (areAllCollinear(sorted)) {
			throw new IllegalArgumentException("cannot create a convex hull from collinear points");
		}

		Stack<PointNode> stack = new Stack<PointNode>();
		stack.push(sorted.get(0));
		stack.push(sorted.get(1));

		for (int i = 2; i < sorted.size(); i++) {

			PointNode head = sorted.get(i);
			PointNode middle = stack.pop();
			PointNode tail = stack.peek();

			Turn turn = getTurn(tail, middle, head);

			switch (turn) {
				case COUNTER_CLOCKWISE:
					stack.push(middle);
					stack.push(head);
					break;
				case CLOCKWISE:
					i--;
					break;
				case COLLINEAR:
					stack.push(head);
					break;
			}
		}

		// close the hull
		stack.push(sorted.get(0));

		return new ArrayList<PointNode>(stack);
	}

	private static PointNode getLowestPoint(List<PointNode> points) {

		PointNode lowest = points.get(0);

		for (int i = 1; i < points.size(); i++) {

			PointNode temp = points.get(i);
			if (compareDouble(temp.latitude, lowest.latitude) < 0 || (compareDouble(temp.latitude, lowest.latitude) == 0 && compareDouble(temp.longitude, lowest.longitude) < 0)) {
				lowest = temp;
			}
		}

		return lowest;
	}

	private static Set<PointNode> getSortedPointSet(List<PointNode> points) {

		final PointNode lowest = getLowestPoint(points);

		TreeSet<PointNode> set = new TreeSet<PointNode>(new Comparator<PointNode>() {
			@Override
			public int compare(PointNode a, PointNode b) {

				if (a == b || a.equals(b)) {
					return 0;
				}

				double thetaA = Math.atan2(a.latitude - lowest.latitude, a.longitude - lowest.longitude);
				double thetaB = Math.atan2(b.latitude - lowest.latitude, b.longitude - lowest.longitude);

				if (compareDouble(thetaA, thetaB) < 0) {
					return -1;
				} else if (compareDouble(thetaA, thetaB) > 0) {
					return 1;
				} else {
					double distanceA = Math.sqrt(((lowest.longitude - a.longitude) * (lowest.longitude - a.longitude)) +
							((lowest.latitude - a.latitude) * (lowest.latitude - a.latitude)));
					double distanceB = Math.sqrt(((lowest.longitude - b.longitude) * (lowest.longitude - b.longitude)) +
							((lowest.latitude - b.latitude) * (lowest.latitude - b.latitude)));

					if (compareDouble(distanceA, distanceB) < 0) {
						return -1;
					} else {
						return 1;
					}
				}
			}
		});

		set.addAll(points);

		return set;
	}

	private static Turn getTurn(PointNode a, PointNode b, PointNode c) throws IllegalArgumentException {

		Double crossProduct = ((b.longitude - a.longitude) * (c.latitude - a.latitude)) -
				((b.latitude - a.latitude) * (c.longitude - a.longitude));
		if (compareDouble(crossProduct, 0.0) > 0) {
			return Turn.COUNTER_CLOCKWISE;
		} else if (compareDouble(crossProduct, 0.0) < 0) {
			return Turn.CLOCKWISE;
		} else {
			return Turn.COLLINEAR;
		}
	}


}