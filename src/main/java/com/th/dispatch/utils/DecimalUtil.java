package com.th.dispatch.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecimalUtil {
	/**
	 * 四舍五入
	 *
	 * @param number
	 * @param rest
	 * @return
	 */
	public static Double roundingHalfUp(Double number, Integer rest) {
		if (number == null) {
			return 0.0;
		}
		BigDecimal b = new BigDecimal(number);
		double nb = b.setScale(rest, RoundingMode.HALF_UP).doubleValue();
		return nb;
	}

	/**
	 * a<b -1;a>b 1;a=b 0
	 *
	 * @param a
	 * @param b
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static int compareDouble(Double a, Double b) throws IllegalArgumentException {
		if (a != null && b != null) {
			BigDecimal data1 = new BigDecimal(a);
			BigDecimal data2 = new BigDecimal(b);
			return data1.compareTo(data2);
		} else {
			throw new IllegalArgumentException("两个比较对象必须都!=null");
		}
	}
}