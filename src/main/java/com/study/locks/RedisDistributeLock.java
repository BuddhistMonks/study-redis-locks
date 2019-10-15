package com.study.locks;

import java.util.Collections;

import redis.clients.jedis.Jedis;

public class RedisDistributeLock {

	/**
	 * 尝试获取分布式锁
	 * 
	 * @param jedis
	 *            Redis客户端
	 * @param key
	 *            锁
	 * @param value
	 *            请求标识
	 * @param expireTime
	 *            超期时间
	 * @return 是否获取成功
	 */
	public boolean tryGetDistributedLock(Jedis jedis, String key, String value, int expireTime) {

		String result = jedis.set(key, value, "NX", "PX", expireTime);

		if ("OK".equals(result)) {
			return true;
		}
		return false;

	}

	private final Long RELEASE_SUCCESS = 1L;

	// 获取锁时的睡眠等待时间片，单位毫秒
	private long SLEEP_PER = 5;

	private final String key = "lock_key";

	private final String value = "lock_value";

	public final int exprieTimeInMilliseconds = 5 * 1000;

	/**
	 * 释放分布式锁
	 * 
	 * @param jedis
	 *            Redis客户端
	 * @param lockKey
	 *            锁
	 * @param value
	 *            请求标识
	 * @return 是否释放成功
	 */
	public boolean unLock(Jedis jedis, String lockKey, String value) {

		String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
		Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(value));

		if (RELEASE_SUCCESS.equals(result)) {
			return true;
		}
		return false;
	}

	// 获取锁
	public void lock() {
		try (Jedis jedis = JedisUtil.getJedis();) {
			while (!(tryGetDistributedLock(jedis, key, value, exprieTimeInMilliseconds))) {
				try {
					Thread.sleep(SLEEP_PER);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public boolean tryLock() {
		try (Jedis jedis = JedisUtil.getJedis();) {
			return tryGetDistributedLock(jedis, key, value, exprieTimeInMilliseconds);
		}
	}

	public void unlock() {
		try (Jedis jedis = JedisUtil.getJedis();) {
			unLock(jedis, key, value);
		}
	}

}
