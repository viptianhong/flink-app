package com.th.dispatch.match;

import com.th.dispatch.pojo.Driver;
import com.th.dispatch.pojo.OrderMsg;
import com.th.dispatch.utils.RedisUtil;

import java.util.List;

public class MatchStrategy {

	public static void match(List<OrderMsg> orders, List<Driver> drivers) {
		if (orders != null && orders.size() > 0 && drivers != null && drivers.size() > 0) {
			//todo 两个集合的全局匹配
			orders.stream()
					.forEach(orderMsg -> {
						RedisUtil.exec(jedis -> {
							jedis.setex(RedisUtil.DISPATCH_ORDER_LOCK + orderMsg.getOrderId(), RedisUtil.DISPATCH_LOCK_TIMEOUT, orderMsg.getOrderId() + orderMsg.getDriverId());
							//锁定司机
							Long flag = jedis.setnx(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId(), orderMsg.getOrderId() + orderMsg.getDriverId());
							if (flag > 0l) {
								jedis.setex(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId(), RedisUtil.DISPATCH_LOCK_TIMEOUT, orderMsg.getOrderId() + orderMsg.getDriverId());
							}
						});
						//todo 给司机发push
					});
		}
	}
}