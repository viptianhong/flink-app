package com.th.dispatch.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

	//派单订单锁
	public static final String DISPATCH_ORDER_LOCK = "dispatch:order:lock:";
	//派单司机锁
	public static final String DISPATCH_DRIVER_LOCK = "dispatch:driver:lock:";
	//一次派单的超时锁时长（秒）
	public static final int DISPATCH_LOCK_TIMEOUT = 30;
	//司机接单到服务结束锁，订单结束需要主动删除这个锁，默认1天
	public static final String END_SERVICE_DRIVER_LOCK = "end:service:driver:lock:";
	//司机接单到服务结束锁时长
	public static final int END_SERVICE_DRIVER_LOCK_TIMEOUT = 24 * 60 * 60;

	//服务器IP地址
	private static String ADDR = "127.0.0.1";
	//端口
	private static int PORT = 6379;
	//密码
	private static String AUTH = null;
	//连接实例的最大连接数
	private static int MAX_ACTIVE = 3;
	//控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
	private static int MAX_IDLE = 2;
	//等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException
	private static int MAX_WAIT = 1000;
	//连接超时的时间　　
	private static int TIMEOUT = 1000;
	// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
	private static boolean TEST_ON_BORROW = true;

	private static JedisPool jedisPool = null;
	//数据库模式是16个数据库 0~15
	public static final int DEFAULT_DATABASE = 0;

	/**
	 * 初始化Redis连接池
	 */

	static {

		try {

			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(MAX_ACTIVE);
			config.setMaxIdle(MAX_IDLE);
			config.setMinIdle(1);
			config.setMaxWaitMillis(MAX_WAIT);
			config.setTestOnBorrow(TEST_ON_BORROW);
			config.setTestWhileIdle(true);
			jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH, DEFAULT_DATABASE);

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	/**
	 * 获取Jedis实例
	 */

	private synchronized static Jedis getJedis() {

		try {

			if (jedisPool != null) {
				Jedis resource = jedisPool.getResource();
				System.out.println("redis--服务正在运行: " + resource.ping());
				return resource;
			} else {
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/***
	 *
	 * 释放资源
	 */

	private static void returnResource(final Jedis jedis) {
		if (jedis != null) {
			//jedisPool.returnResource(jedis);
			jedis.close();
		}

	}

	public static void exec(IHandler handler) {
		Jedis jedis = RedisUtil.getJedis();
		try {
			handler.handle(jedis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			RedisUtil.returnResource(jedis);
		}
	}

	public static void main(String[] args) {
		String key = "tian";
		String value = System.currentTimeMillis() / 1000 + "";
//		Jedis jedis = RedisUtil.getJedis();
//		System.out.println(jedis.get(key));
//		jedis.set(key, value);
//		System.out.println(jedis.get(key));
//		RedisUtil.returnResource(jedis);

		RedisUtil.exec(jedis -> {
			System.out.println(jedis.get(key));
			jedis.set(key, value);
			System.out.println(jedis.get(key));
		});
	}

	public interface IHandler {
		void handle(Jedis jedis);
	}
}