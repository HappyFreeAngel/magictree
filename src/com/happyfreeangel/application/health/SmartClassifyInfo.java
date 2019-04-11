package com.happyfreeangel.application.health;

import com.happyfreeangel.application.HealthHTMLMaker;

public class SmartClassifyInfo {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		// test();
		String propertiesFileNameWithFullPath = null;

		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				System.out.println("args[" + i + "]=" + args[i]);
			}
			propertiesFileNameWithFullPath = args[0].trim();
		}
		if (propertiesFileNameWithFullPath == null) {
			System.out.println("propertiesFileNameWithFullPath is null.");
			return;
		}
		System.out.println("propertiesFileNameWithFullPath="
				+ propertiesFileNameWithFullPath);

		HealthHTMLMaker maker = new HealthHTMLMaker();
		maker.smartClassifyInfo(propertiesFileNameWithFullPath);
	}

}
