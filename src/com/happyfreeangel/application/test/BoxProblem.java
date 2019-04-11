package com.happyfreeangel.application.test;

import java.util.*;

public class BoxProblem {

	private static TreeSet<Box> boxes = new TreeSet<Box>();// ר�������Ѿ���Ϻõ����ݣ�ÿ�����ϴ���һ������.

	/**
	 * ����n����0��1��֮����������������
	 * 
	 * @param n
	 * @return
	 */
	public static TreeSet getData(int n) {
		if (n < 1) {
			System.out.println("�Ƿ�����.");
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
		// ���򣬴�С��������

		System.out.println("��С��������󣬽������: ����" + data.length + "�������ܺ���:"
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

		TreeSet<Double> set = new TreeSet<Double>(); // ���ǰ��մ�С������������򼯺ϣ���������µ������򼯺ϻ��Ǵ�С��������.
		for (int i = 0; i < data.length; i++) {
			set.add(data[i]);
		}
		return set;
	}

	/***
	 * ������Ļ��������ѵ���ϣ�ȷ��ÿ���������ܺ�<=1 �Ҽ���������С���㷨������.
	 */
	public void assemble(TreeSet data) {
		Box newBox = new Box();
		Double tmp = (Double) data.last();// ����һ����.
		newBox.add(tmp);
		data.remove(tmp);// ɾ�������һ������.

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
