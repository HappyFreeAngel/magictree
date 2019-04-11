package com.happyfreeangel.application.test;

import java.util.*;

public class Box {
	private TreeSet<Double> data = new TreeSet<Double>();
	private Double total = new Double(0.0);

	public Double getSum() {
		return total;
	}

	public void add(Double tmp) {
		data.add(tmp);
		total += tmp;
	}

	public void remove(Double tmp) {
		data.remove(tmp);
		total -= tmp;
	}

	/**
	 * 清空所有数据.
	 */
	public void clear() {
		data.clear();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
