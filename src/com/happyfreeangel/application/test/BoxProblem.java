package com.happyfreeangel.application.test;

import java.util.*;

public class BoxProblem {

	private static TreeSet<Box> boxes = new TreeSet<Box>();// 专门来放已经组合好的数据，每个集合代表一个箱子.

	/**
	 * 产生n个【0，1】之间的数，随机产生，
	 * 
	 * @param n
	 * @return
	 */
	public static TreeSet getData(int n) {
		if (n < 1) {
			System.out.println("非法输入.");
			return null;
		}
		double[] data = new double[n];

		System.out.println();
		int totalInt = 0;
		double total = 0.0;
		for (int i = 0; i < n; i++) {
			data[i] = ((int) (100 * Math.random())) / 100.0;
			totalInt += (int) (data[i] * 100);
		}

		total = totalInt * 1.0 / 100;
		// 排序，从小到大排序

		System.out.println("从小到大排序后，结果如下: 共有" + data.length + "个数，总和是:"
				+ totalInt + "  total=" + total);
		double tmp = 0.0;
		for (int i = 0; i < data.length; i++) {
			for (int j = i + 1; j < data.length; j++) {
				if (data[j] < data[i]) {
					tmp = data[i];
					data[i] = data[j];
					data[j] = tmp;
				}
			}
			System.out.print(" " + data[i]);
		}

		TreeSet<Double> set = new TreeSet<Double>(); // 这是按照从小到大排序的排序集合，如果插入新的数，则集合还是从小到大排序.
		for (int i = 0; i < data.length; i++) {
			set.add(data[i]);
		}
		return set;
	}

	/***
	 * 对输入的货物进行最佳的组合，确保每个集合里总和<=1 且集合数量最小，算法最优秀.
	 */
	public void assemble(TreeSet data) {
		Box newBox = new Box();
		Double tmp = (Double) data.last();// 最大的一个数.
		newBox.add(tmp);
		data.remove(tmp);// 删除解决了一个问题.

		if (data.size() == 0) {
			boxes.add(newBox); // ??
			return;
		}

	}

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			int n = (int) (20 * Math.random());
			if (n < 1) {
				n = 1;
			}
			TreeSet<Double> data = getData(n);

		}
	}
}
