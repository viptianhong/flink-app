package com.th.dispatch.match;

import com.th.dispatch.pojo.Driver;
import com.th.dispatch.pojo.OrderMsg;
import com.th.dispatch.utils.RedisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MatchStrategy {

	public static void match(List<OrderMsg> orders, List<Driver> drivers) {
		if (orders == null || orders.size() == 0) {
			return;
		}
		if (drivers == null || drivers.size() == 0) {
			return;
		}

		int[][] dataMatrix = null;
		//初始化权重
		int ovsdFlag = orders.size() > drivers.size() ? 1 : 0;
		if (ovsdFlag == 1) {
			dataMatrix = new int[orders.size()][orders.size()];
			for (int i = 0; i < orders.size(); i++) {
				for (int j = 0; j < orders.size(); j++) {
					if (j > (drivers.size() - 1)) {
						dataMatrix[i][j] = 0;
					} else {
						int weight = getWeight(orders.get(i), drivers.get(j));
						dataMatrix[i][j] = weight;
					}
				}
			}
		} else {
			dataMatrix = new int[drivers.size()][drivers.size()];
			for (int i = 0; i < drivers.size(); i++) {
				for (int j = 0; j < drivers.size(); j++) {
					if (i > (orders.size() - 1)) {
						dataMatrix[i][j] = 0;
					} else {
						int weight = getWeight(orders.get(i), drivers.get(j));
						dataMatrix[i][j] = weight;
					}
				}
			}
		}
		//开始匹配
		HungarianAlgorithm ha = new HungarianAlgorithm(dataMatrix);
		int[][] assignment = ha.findOptimalAssignment();

		for (int i = 0; i < assignment.length; i++) {
			int orderIndex = assignment[i][0];
			int driverIndex = assignment[i][1];
			if (orderIndex <= (orders.size() - 1)) {
				OrderMsg orderMsg = orders.get(orderIndex);
				if (driverIndex <= (drivers.size() - 1)) {
					Driver driver = drivers.get(driverIndex);
					orderMsg.setDriverId(driver.getDriverId());
				}

			}

		}

		//匹配完之后锁定司机
		orders.stream()
				.forEach(orderMsg -> {
					if (orderMsg.getDriverId() != null && orderMsg.getDriverId().trim().length() > 0) {
						RedisUtil.exec(jedis -> {
							jedis.setex(RedisUtil.DISPATCH_ORDER_LOCK + orderMsg.getOrderId(), RedisUtil.DISPATCH_LOCK_TIMEOUT, orderMsg.getOrderId() + orderMsg.getDriverId());
							//锁定司机
							Long flag = jedis.setnx(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId(), orderMsg.getOrderId() + orderMsg.getDriverId());
							if (flag > 0l) {
								jedis.setex(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId(), RedisUtil.DISPATCH_LOCK_TIMEOUT, orderMsg.getOrderId() + orderMsg.getDriverId());
							}
						});
						//todo 给司机发push
					}
				});
	}

	//派单策略因子
	private static int getWeight(OrderMsg orderMsg, Driver driver) {
		//todo 可以按业务去做，下面的二维数组是测试用的数据，可以用路径规划接口得到的导航时间
		int[][] dataMatrix = {
				{70, 40, 20, 55},
				{65, 60, 45, 90},
				{30, 45, 50, 75},
				{25, 30, 55, 40}
		};
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (orderMsg.getOrderId().substring(5).equals(i + "")) {
					if (driver.getDriverId().substring(6).equals(j + "")) {
						return dataMatrix[i][j];
					}
				}
			}
		}
		return 0;
	}

	//测试用
	public static void main(String[] args) {
		List<OrderMsg> orders = new ArrayList<>();
		List<Driver> drivers = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			//调下面这个值，模拟订单少于司机
			if (i < 3) {
				OrderMsg orderMsg = new OrderMsg();
				orderMsg.setOrderId("order" + i);
				orders.add(orderMsg);
			}
			//调下面这个值，模拟司机少于订单
			if (i < 4) {
				Driver driver = new Driver();
				driver.setDriverId("driver" + i);
				drivers.add(driver);
			}
		}
		System.out.println("匹配前：" + orders);
		match(orders, drivers);
		System.out.println("匹配后：" + orders);
	}
}