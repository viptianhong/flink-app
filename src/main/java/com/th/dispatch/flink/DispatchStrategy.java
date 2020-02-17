package com.th.dispatch.flink;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.th.dispatch.match.MatchStrategy;
import com.th.dispatch.utils.RedisUtil;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08;
import org.apache.flink.util.Collector;
import com.th.dispatch.pojo.*;

import java.util.*;

/**
 * @author tianchenxi
 * @date 2020-02-15
 * @qq 1138650832
 * 派单用例：
 * <p>
 * 0 初始
 * 1 派单中
 * 2 司机接单
 * <p>
 * 0 初始
 * 1 派单中
 * 3 司机无响应，超时
 * <p>
 * 0 初始
 * 4 附近无空闲司机，超时
 * <p>
 * 每个订单最长timeWindowWidth分钟的派单窗口
 * 每timeWindowSlide秒滑动一次，做一次匹配派单
 * <p>
 * {"phone":"15652374919","orderId":"1001","dispatchState":0,"cityId":1,"lng":116.459739,"lat":39.939443,"createTime":1581905632043}
 * {"phone":"15652374919","orderId":"1001","driverId":"BJ9072","dispatchState":1,"cityId":1,"lng":116.459739,"lat":39.939443,"createTime":1581905632043}
 * {"phone":"15652374919","orderId":"1001","driverId":"BJ9072","dispatchState":2,"cityId":1,"lng":116.459739,"lat":39.939443,"createTime":1581905632043}
 * {"phone":"15652374919","orderId":"1001","driverId":"BJ9072","dispatchState":3,"cityId":1,"lng":116.459739,"lat":39.939443,"createTime":1581905632043}
 */
public class DispatchStrategy {
	public static void main(String[] args) {

		final StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
//		streamExecutionEnvironment.enableCheckpointing(5000);

		final long timeWindowWidth = 5 * 60l;
		final long timeWindowSlide = 5l;
		final long dispatchTimeout = 5 * 60 * 1000l;
		final String bootstrapServers = "127.0.0.1:9092";
		final String zookeeperConnect = "127.0.0.1:2181";
		final String topic = "th_test";
		final String consumerGroup = "th_test";
		final String UN_KNOWN_AREA = "unknown area";

		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", bootstrapServers);
		properties.setProperty("zookeeper.connect", zookeeperConnect);
		properties.setProperty("group.id", consumerGroup);
		List topicList = new ArrayList<String>();
		topicList.add(topic);
		final FlinkKafkaConsumer08<String> flinkKafkaConsumer08 = new FlinkKafkaConsumer08<String>(topicList, new SimpleStringSchema(), properties);
		flinkKafkaConsumer08.setStartFromLatest();

		SingleOutputStreamOperator<String> singleOutputStreamOperator1 = streamExecutionEnvironment
				.addSource(flinkKafkaConsumer08)
				.filter((FilterFunction<String>) s -> {
					try {
						JSONObject orderJson = JSON.parseObject(s);
						if (orderJson != null) {
							//可以按各种条件过滤
							return true;
						}
					} catch (Exception e) {

					}
					return false;
				});

		SingleOutputStreamOperator<OrderMsg> singleOutputStreamOperator2 = singleOutputStreamOperator1
//				.setParallelism(1)
				.map((MapFunction<String, OrderMsg>) s -> {
					OrderMsg om = JSON.parseObject(s, OrderMsg.class);
					if (om.getDispatchState() == DispatchStateEnum.INITIAL.getCode()) {
						Area area = AreaTable.getArea(om.getCityId(), om.getLng(), om.getLat());
						om.setAreaIndex(area != null ? area.getIndex() : UN_KNOWN_AREA);
					}
					return om;
				});


		singleOutputStreamOperator2.keyBy((KeySelector<OrderMsg, Integer>) orderMsg -> orderMsg.getCityId())
				.timeWindow(Time.seconds(timeWindowWidth), Time.seconds(timeWindowSlide))
				.aggregate(new AggregateFunction<OrderMsg, Map<String, OrderMsg>, List<OrderMsg>>() {

					@Override
					public Map<String, OrderMsg> createAccumulator() {
						return new HashMap<>();
					}

					@Override
					public Map<String, OrderMsg> add(OrderMsg orderMsg, Map<String, OrderMsg> orderMsgMap) {
						if (!UN_KNOWN_AREA.equals(orderMsg.getAreaIndex())) {
							if (orderMsgMap.containsKey(orderMsg.getOrderId())) {
								if (orderMsg.getDispatchState() == DispatchStateEnum.ACCEPT.getCode()) {
									//司机接单
									//一直锁定司机，直到服务结束，结束的时候需要删除这个司机锁
									RedisUtil.exec(jedis -> {
										//锁定司机
										Long flag = jedis.setnx(RedisUtil.END_SERVICE_DRIVER_LOCK + orderMsg.getDriverId(), orderMsg.getOrderId() + orderMsg.getDriverId());
										if (flag > 0l) {
											jedis.setex(RedisUtil.END_SERVICE_DRIVER_LOCK + orderMsg.getDriverId(), RedisUtil.END_SERVICE_DRIVER_LOCK_TIMEOUT, orderMsg.getOrderId() + orderMsg.getDriverId());
										}
									});
									orderMsgMap.remove(orderMsg.getOrderId());
								} else if (orderMsg.getDispatchState() == DispatchStateEnum.CANCEL_BY_USER.getCode()) {
									//用户取消
									//释放订单派单锁和司机派单锁
									RedisUtil.exec(jedis -> {
										jedis.del(RedisUtil.DISPATCH_ORDER_LOCK + orderMsg.getOrderId());
										if (orderMsg.getDriverId() != null && orderMsg.getDriverId().trim().length() > 0) {
											jedis.del(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId());
										}
									});
									orderMsgMap.remove(orderMsg.getOrderId());
								} else if (orderMsg.getDispatchState() == DispatchStateEnum.CANCEL_BY_DRIVER.getCode()) {
									//司机取消
									//释放订单派单锁和司机派单锁
									RedisUtil.exec(jedis -> {
										jedis.del(RedisUtil.DISPATCH_ORDER_LOCK + orderMsg.getOrderId());
										jedis.del(RedisUtil.DISPATCH_DRIVER_LOCK + orderMsg.getDriverId());
									});
									orderMsgMap.remove(orderMsg.getOrderId());
								}
							} else {
								if (orderMsg.getDispatchState() == DispatchStateEnum.INITIAL.getCode()) {
									//初始
									orderMsgMap.put(orderMsg.getOrderId(), orderMsg);
								}
							}
						}
						return orderMsgMap;
					}

					@Override
					public List<OrderMsg> getResult(Map<String, OrderMsg> orderMsgMap) {
						return new ArrayList<>(orderMsgMap.values());
					}

					@Override
					public Map<String, OrderMsg> merge(Map<String, OrderMsg> stringOrderMsgMap, Map<String, OrderMsg> acc1) {
						return null;
					}
				})
				.flatMap(new FlatMapFunction<List<OrderMsg>, Tuple3<Integer, String, List<OrderMsg>>>() {
					@Override
					public void flatMap(List<OrderMsg> orderMsgs, Collector<Tuple3<Integer, String, List<OrderMsg>>> collector) throws Exception {
						final Map<String, List<OrderMsg>> reduceMap = new HashMap<>();
						orderMsgs
								.stream()
								.forEach(orderMsg -> {
									if (reduceMap.containsKey(orderMsg.getAreaIndex())) {
										List<OrderMsg> existList = reduceMap.get(orderMsg.getAreaIndex());
										existList.add(orderMsg);
										reduceMap.put(orderMsg.getAreaIndex(), existList);
									} else {
										List<OrderMsg> orderMsgList = new ArrayList<>();
										orderMsgList.add(orderMsg);
										reduceMap.put(orderMsg.getAreaIndex(), orderMsgList);
									}
								});
						reduceMap.keySet().stream().forEach(s -> {
							collector.collect(new Tuple3<>(reduceMap.get(s).get(0).getCityId(), s, reduceMap.get(s)));
						});
					}
				})
				.map(new MapFunction<Tuple3<Integer, String, List<OrderMsg>>, List<OrderMsg>>() {

					@Override
					public List<OrderMsg> map(Tuple3<Integer, String, List<OrderMsg>> orders) throws Exception {
						//已经正在派的订单
						List<OrderMsg> dispatchingList = new ArrayList();

						List<Driver> idleList = new ArrayList<>();
						//orders.f0是城市id，orders.f1是区域索引，根据区域索引从db或配置中心查出对应的区域配置，找到该区域的空闲司机，集合n
						Area area = AreaTable.areaMap.get(orders.f0).get(orders.f1);
						if (area != null) {
							idleList = DriverTable.getNearbyIdleDriver(area.getCityId(), area.getCenterLng(), area.getCenterLat());
						}
						//orders.f2是该区域的未派或正在派的订单，先过滤掉正在派的订单
						List<OrderMsg> toDispatchOrderList = new ArrayList<>();
						orders.f2
								.stream()
								.forEach(orderMsg -> {
									long now = System.currentTimeMillis();
									//过滤掉超时订单
									if (orderMsg.getCreateTime() + dispatchTimeout > now) {
										if (orderMsg.getDispatchState() == DispatchStateEnum.INITIAL.getCode()) {
											RedisUtil.exec(jedis -> {
												boolean flag = jedis.exists(RedisUtil.DISPATCH_ORDER_LOCK + orderMsg.getOrderId());
												if (!flag) {
													toDispatchOrderList.add(orderMsg);
												} else {
													dispatchingList.add(orderMsg);
												}
											});
										}
									}
								});
						//匹配算法，m : n
						//通过批量算路，得到了k个订单和司机的匹配关系，k<=min{m,n}，总时间最短或总时长最短等策略，把匹配好的k个司机锁定30秒
						MatchStrategy.match(toDispatchOrderList, idleList);

						dispatchingList.addAll(toDispatchOrderList);
						//输出所有正在派的订单
						return dispatchingList;
					}
				}).print();

		try {
			streamExecutionEnvironment.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}