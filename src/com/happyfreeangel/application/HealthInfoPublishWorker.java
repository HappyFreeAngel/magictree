package com.happyfreeangel.application;

import java.io.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import net.snapbug.util.dbtool.ConnectionManager;
import net.snapbug.util.dbtool.ConnectionPool;

import cn.infoservice.common.Mail;
import cn.infoservice.common.MailCarrier;

import com.happyfreeangel.magictree.CatalogTree;
import com.happyfreeangel.magictree.CatalogTreeView;
import cn.infoservice.common.*;

public class HealthInfoPublishWorker extends Thread {
	private int updateInfoCount = 2; // 每次更新5个信息，如果目录大于updateInfoCount就从每个目录选一个更新
	private int waitInSeconds = 60 * 60;// 没任务时等待1个小时.
	private int updateMethod; // 发布信息的方法
	private CatalogTree ct;
	private CatalogTreeView ctv;
	private StringBuffer model;
	private StringBuffer dirModel;
	private int dictionaryFolderID;
	private String propertiesFileNameWithFullPath;
	private String latestInfoScriptFileName; // 最新信息的JavaScript脚本
	private String hotInfoScriptFileName; // 访问量最高的信息JavaScript脚本
	private long latestNotifyTimeInMilliseconds = 0;
	private Properties profile;

	public HealthInfoPublishWorker(String propertiesFileNameWithFullPath)
			throws Exception {
		this.propertiesFileNameWithFullPath = propertiesFileNameWithFullPath;
	}

	/**
	 * 通知用户，信息即将消耗尽，已经低于指定的数值。 newInfoCount 目前剩下的信息数量.
	 * 
	 * @param content
	 */
	public void notifyByEmail(int newInfoCount) {
		MailCarrier mc = new MailCarrier(profile.getProperty("mailHost"), 25,
				"smtp", profile.getProperty("senderEmail"),
				profile.getProperty("senderEmailPassword"), true);
		/**
		 * 还剩下多少秒就耗尽.
		 */
		double secondsLeft = newInfoCount * .10 / this.updateInfoCount
				* this.waitInSeconds;
		// 什么时候耗尽.
		Timestamp futureTime = new Timestamp(System.currentTimeMillis()
				+ (long) (secondsLeft * 1000));

		Mail mail = new Mail();
		mail.setSender(profile.getProperty("senderEmail"));
		mail.addReceiver(profile.getProperty("executorEmail"));

		mail.setSubject(profile.getProperty("website") + "网站信息添加紧急通知");
		mail.setText(" 尊敬的" + profile.getProperty("executorEmail")
				+ " \n\r    网站" + profile.getProperty("website") + "可添加的库存信息量:"
				+ newInfoCount + "已经低于指定的数值:"
				+ profile.getProperty("alertInfoCount") + " 在 " + futureTime
				+ " 时，信息库存将彻底用完. 请您务必尽快安排添加，以免网站自动更新中断.");
		mail.setContentEncoding("GBK");
		// mail.setFormat("text/html"); //text/html
		mail.setNotification(true);
		mc.send(mail);
	}

	/***
	 * 把一个文件读到内存的StringBuffer 对象里.
	 * 
	 * @param fileName
	 * @param encoding
	 * @return
	 */
	public static StringBuffer readFileToBuffer(String fileName, String encoding) {
		StringBuffer model = new StringBuffer();

		File f = new File(fileName);
		int len = (int) f.length();
		if (len <= 0) {
			System.out.println("failed");
			return null;
		}

		byte[] fileContent = null;
		try {
			FileInputStream fin = new FileInputStream(f);
			fileContent = new byte[len];

			int bytesHasRead = fin.read(fileContent);

			if (bytesHasRead != len) {
				System.out.println("读取文件,发生了错误. fileLength=" + len
						+ "  已经成功读到字节数组:" + bytesHasRead + "字节");
				return null;
			}
		} catch (IOException e) {
			System.out.println(e);
		}
		try {
			model = new StringBuffer(new String(fileContent, encoding)); // ??为什么GBK会乱码?
		} catch (Exception e) {
			System.out.println("编码:" + encoding + "不支持");
		}
		return model;
	}

	public void load() throws Exception {
		profile = new Properties();
		try {
			profile.load(new FileInputStream(propertiesFileNameWithFullPath));
		} catch (IOException e) {
			System.out.println(e);
		}
		int catalogTreeID = Integer.parseInt(profile
				.getProperty("catalogTreeID"));
		ConnectionManager.init(profile.getProperty("dbConf"));
		ConnectionPool pool = ConnectionManager.getConnectionPool(profile
				.getProperty("poolName"));

		if (pool == null) {
			System.out.println("database connection failed.");
		}
		String catalogTreeViewTableName = profile
				.getProperty("catalogTreeViewTableName");

		CatalogTreeView catalogTreeView = null;
		CatalogTree currentCatalogTree = null;

		catalogTreeView = CatalogTreeView.loadFromDatabase(pool, catalogTreeID,
				catalogTreeViewTableName);
		if (catalogTreeView == null) {
			System.out.println("error! catalogTreeView 加载失败.");
			return;
		}

		if (catalogTreeView != null) {
			this.ct = CatalogTree.loadFromDatabase(catalogTreeView); // root
			currentCatalogTree = ct; // root
			this.ctv = catalogTreeView;
		}
		if (ct == null) {
			System.out.println("catalogTree 加载失败.");
			return;
		}

		this.waitInSeconds = Integer.parseInt(profile
				.getProperty("waitInSeconds"));
		this.updateInfoCount = Integer.parseInt(profile
				.getProperty("updateInfoCount"));
		this.dictionaryFolderID = Integer.parseInt(profile
				.getProperty("dictionaryFolderID"));
		this.model = readFileToBuffer(profile.getProperty("modelInfoFile"),
				profile.getProperty("modelInfoFileEncoding"));
		this.dirModel = readFileToBuffer(profile.getProperty("modelDirFile"),
				profile.getProperty("modelDirFileEncoding"));
		this.hotInfoScriptFileName = profile
				.getProperty("hotInfoScriptFileName");
		this.latestInfoScriptFileName = profile
				.getProperty("latestInfoScriptFileName");
	}

	public static String readTextFileContent(String fileNameWithFullPath) {
		// 2.读取原来文件内容到一个String对象
		String originText = null;
		java.io.File f = new java.io.File(fileNameWithFullPath);
		java.io.InputStream input = null;
		try {
			input = new java.io.FileInputStream(f);
		} catch (Exception e) {
			System.out.println(e);
		}
		int bytesHasRead = 0;
		int off = 0;
		byte[] byteBuffer = new byte[(int) f.length() * 2];

		while (true) {
			try {
				bytesHasRead = input.read(byteBuffer, off, (int) f.length());
			} catch (Exception e) {
				System.out.println("文件读取错误.");
				break;
			}

			if (bytesHasRead >= 0) {
				off += bytesHasRead;
			} else {
				break;
			}
		}
		try {
			input.close();
		} catch (IOException e) {
			System.out.println(e);
		}

		if (off == f.length()) {
			try {
				originText = new String(byteBuffer, 0, off, "GBK");
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		return originText;
	}

	/**
	 * 获取未发布的信息数量.
	 * 
	 * @return
	 */
	public int getNewInfoCount() {
		int newInfoCount = 0;
		String querySQL = " select count(distinct infoID)  from "
				+ ctv.getCatalogTreeTableName() + " where infoPublishStatus=0 ";
		Statement stmt = null;

		try {
			stmt = ctv.getPool().createStatement();
			ResultSet rs = stmt.executeQuery(querySQL);
			if (rs != null && rs.next()) {
				newInfoCount = rs.getInt(1);
			}
			if (rs != null) {
				rs.close();
			}

			if (stmt != null) {
				stmt.close();
			}
		} catch (java.sql.SQLException e) {
			System.out.println(e);
		}

		return newInfoCount;
	}

	/**
	 * 找出模版中用户自定义的信息
	 * 
	 * @param model
	 * @return
	 */
	public static String[][] findModelInfo(StringBuffer model) {
		// <!--段落开始--titleKeyword=亚健康--listSize=7--maxwidth=16-->
		// <!--段落结束--titleKeyword=亚健康--listSize=7--maxwidth=16-->

		String startMark = "<!--段落开始--titleKeyword=";
		String endMark = "-->";
		String listSizeStr = "--listSize=";
		String maxwidthStr = "--maxwidth=";
		int count = 0, from = 0, to = 0;

		Vector<String> tmpV = new Vector<String>();
		while (true) {
			from = model.indexOf(startMark, from);
			to = model.indexOf("-->", from + startMark.length());

			String line = null;
			if (from > 0 && from < to) {
				line = model.substring(from + startMark.length(), to);
			} else {
				break;
			}

			from = to + 3;

			if (line.indexOf(listSizeStr) >= 0) {
				line = line.replace(listSizeStr, " ");
			} else {
				continue;
			}
			if (line.indexOf(maxwidthStr) >= 0) {
				line = line.replace(maxwidthStr, " ");
			} else {
				continue;
			}
			tmpV.add(line);
		}

		String[][] modelInfo = null;
		int successCount = 0;
		if (tmpV.size() > 0) {
			modelInfo = new String[tmpV.size()][3];

			for (int i = 0; i < modelInfo.length; i++) {
				String str = tmpV.get(i);
				String[] tmp = str.split(" ");
				if (tmp.length == 3) {
					modelInfo[i][0] = tmp[0];
					modelInfo[i][1] = tmp[1];
					modelInfo[i][2] = tmp[2];
					successCount++;
				} else {
					System.out.println("替换模版时,模版格式解析有错误.");
				}
			}
		}

		if (successCount != tmpV.size() && successCount > 0)// 有错误.
		{
			modelInfo = null;
		}
		return modelInfo;
	}

	/***
	 * 更新主页信息,根据用户自己定义的模版
	 */
	public void updateHomePage() {
		// 更新首页，让新添加的资料可以马上让人知道.
		String homepageFile = ctv.getCatalogRootInSystemDir()
				+ java.io.File.separator + "index.html";
		StringBuffer modelContent = new StringBuffer(
				readTextFileContent(homepageFile));
		String[][] modelInfo = findModelInfo(modelContent);

		if (modelInfo == null || modelInfo.length < 1) {
			System.out
					.println("主页更新失败.if(modelInfo==null || modelInfo.length<1)");
			return;
		}

		Info[] infos = null;
		for (int i = 0; i < modelInfo.length; i++) {
			String keyword = modelInfo[i][0];
			int listSize = Integer.parseInt(modelInfo[i][1]);
			int maxwidth = Integer.parseInt(modelInfo[i][2]);

			// <!--段落开始--titleKeyword=亚健康--listSize=7--maxwidth=16-->
			// <!--段落结束--titleKeyword=亚健康--listSize=7--maxwidth=16-->
			String startMark = "<!--段落开始--titleKeyword=" + keyword
					+ "--listSize=" + listSize + "--maxwidth=" + maxwidth
					+ "-->";
			String endMark = "<!--段落结束--titleKeyword=" + keyword
					+ "--listSize=" + listSize + "--maxwidth=" + maxwidth
					+ "-->";
			infos = HealthHTMLMaker.getLatestInfoFromWebSiteByKeyword(keyword,
					ctv, listSize);

			// for debug
			// if("latestInfo".equals(keyword))
			{

				if (modelContent.indexOf(startMark) < 0) {
					System.out.println("modelContent 找不到 标记:" + startMark);
				}

				if (modelContent.indexOf(endMark) < 0) {
					System.out.println("modelContent 找不到 标记:" + endMark);
				}

				if (infos != null) {
					System.out
							.println("update home page  latestInfo  infos.size="
									+ infos.length);
				} else {
					System.out
							.println(" update home page  latestInfo infos ==null ");
				}
			}
			if (infos == null || infos.length == 0) {
				continue;
			}
			String itemContent = "";
			// <li><a
			// href="http://www.541fuwu.com/health/jibingdaquan/yajiankang/100_143_53.html">心理亚健康问题探讨</a>
			for (int j = 0; j < infos.length; j++) {
				String tmpTitle = infos[j].getTitle();
				if (tmpTitle.getBytes().length > maxwidth) {
					tmpTitle = truncate(tmpTitle, maxwidth * 2); // 一个汉字2个字节.
				}
				itemContent += "<li><a href=" + ctv.getURI()
						+ infos[j].getVirtualWebPath() + ">" + tmpTitle
						+ "</a>\n\r";
			}

			boolean retainMark = true;
			String tmpOld = modelContent.toString();
			modelContent = findAndReplace(modelContent, startMark, endMark,
					itemContent, retainMark);

			if (!modelContent.toString().equals(tmpOld)) {
				// System.out.println("modelContent = findAndReplace(modelContent, startMark,endMark,itemContent,retainMark); failed.");
				System.out.println("startMark=" + startMark + "  endMark="
						+ endMark + " 之间的内容已经更新.\n itemContent=" + itemContent);
			}
		}

		// 写到文件里.
		try {
			FileOutputStream fout = new FileOutputStream(homepageFile);
			fout.write(modelContent.toString().getBytes("GBK"));
			fout.close();
			System.out.println("主页http://www.541fuwu.com 已经更新成功."
					+ homepageFile);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void generateJavaScript() {
		// 1.最新发布的5条信息【最新信息】
		{
			int returnCount = 10;
			Info[] infos = HealthHTMLMaker.getLatestInfoFromWebSite(ctv,
					returnCount);
			String scriptStr = "document.write(\"<ul>\");";
			if (infos != null) {
				for (int i = 0; i < infos.length; i++) {
					scriptStr += "document.write(\"            <li><a href="
							+ ctv.getURI() + infos[i].getVirtualWebPath() + ">"
							+ truncate(infos[i].getTitle(), 18 * 2)
							+ "</a> \");\n\r";
				}
			}
			scriptStr += "document.write(\"</ul>\");\n\r";

			try {
				FileOutputStream fout = new FileOutputStream(
						this.latestInfoScriptFileName);
				fout.write(scriptStr.getBytes("GBK"));
				fout.close();
				System.out.println(this.latestInfoScriptFileName + " 脚本成功生成.");
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		/*
		 * <ul> <li><a href=
		 * "http://www.541fuwu.com/health/jibingdaquan/zhongliukefenlei/100_148_1010.html"
		 * >预防肿瘤从日常做起</a> <li><a href=
		 * "http://www.541fuwu.com/health/jibingdaquan/yajiankang/100_143_60.html"
		 * >什么是“超亚健康”状态？</a> </ul>
		 */

		// 2.访问量最大的信息【热门信息】

		{
			int returnCount = 10;
			Info[] infos = HealthHTMLMaker.getFavoriteInfoFromWebSite(ctv,
					returnCount);
			String scriptStr = "document.write(\"<ul>\");";
			if (infos != null) {
				for (int i = 0; i < infos.length; i++) {
					scriptStr += "document.write(\"            <li><a href="
							+ ctv.getURI() + infos[i].getVirtualWebPath() + ">"
							+ truncate(infos[i].getTitle(), 18 * 2)
							+ "</a> \");";
				}
			}
			scriptStr += "document.write(\"</ul>\");";

			try {
				FileOutputStream fout = new FileOutputStream(
						this.hotInfoScriptFileName);
				fout.write(scriptStr.getBytes("GBK"));
				fout.close();
				System.out.println(this.hotInfoScriptFileName + " 脚本成功生成.");
			} catch (Exception e) {
				System.out.println(e);
			}

		}
	}

	public void publish() {
		boolean updateInfo = true, updateDir = true;
		HealthHTMLMaker hm = new HealthHTMLMaker();
		hm.safePublishNewInfoToHTMLForUserView(ct, ctv, model, dirModel,
				updateInfo, updateDir, updateInfoCount);
		// 找到指定的信息，生成静态HTML，同时修改状态.
	}

	public void work() {
		publish();
		generateJavaScript();
		updateHomePage();
		check();
	}

	/**
	 * 检查库存量是否足够，如果不足就马上发邮件通知.
	 */
	public void check() {
		// 1.判断最近一个通知周期内是否通知过，如果已经通知过，就不再通知。
		long notifyTimeSpanInSecond = Integer.parseInt(this.profile
				.getProperty("notifyTimeSpanInSecond"));
		if (System.currentTimeMillis() - this.latestNotifyTimeInMilliseconds < notifyTimeSpanInSecond * 1000) {
			return;
		}

		// 2.还没有通知过，检查剩余的信息量是否低于指定的数值.
		int newInfoCount = this.getNewInfoCount(); // 这个查询时比第一步较耗费时间的,这样每个周期只查一次,降低负载.
		int alertInfoCount = 0;

		try {
			alertInfoCount = Integer.parseInt(this.profile
					.getProperty("alertInfoCount"));
		} catch (Exception e) {
			System.out.println(e);
		}

		// 超出指定的时间才通知，不是整体通知不停.
		if (newInfoCount <= alertInfoCount) {
			this.notifyByEmail(newInfoCount);
			this.latestNotifyTimeInMilliseconds = System.currentTimeMillis();
		}
	}

	/**
	 * 截段字符串，但是不会把汉字截成2半.
	 * 
	 * @param str
	 *            输入的要处理的字符串.
	 * @param limitBytes
	 *            最多字节数.
	 * @return
	 */
	public static String truncate(String str, int limitBytes) {
		if (str == null) {
			return null;
		}

		// 没有溢出,就直接返回原来的值.
		if (str.getBytes().length <= limitBytes) {
			return str;
		}

		// str 超过了指定的字节数,要截断.

		int half = (int) (limitBytes / 2.0);

		int pos = half;

		String tmpStr = null;

		if (str.length() >= pos && pos > 0) {
			tmpStr = str.substring(0, pos);
		} else {
			// System.out.println("error"); ???
			return str;
		}
		while (tmpStr.getBytes().length + 1 < limitBytes) {
			pos++;
			tmpStr = str.substring(0, pos);
		}
		return str.substring(0, pos);
	}

	public void run() {
		try {
			load();
		} catch (Exception e) {
			System.out.println(e);
		}
		if (waitInSeconds <= 60) {
			waitInSeconds = 600; // 10分钟.
		}
		while (true) {
			System.out.println("现在时间:"
					+ new Timestamp(System.currentTimeMillis()));
			work();
			try {
				System.out.println("休息"
						+ waitInSeconds
						+ "秒，看看是否有未发布的信息可以发布.下次检查时间:"
						+ new Timestamp(System.currentTimeMillis()
								+ waitInSeconds * 1000));
				Thread.sleep(1000 * waitInSeconds);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	/**
	 * @param buf
	 *            字符串
	 * @param startMark
	 * @param endMark
	 * @param newValue
	 * @param retainMark
	 *            保留标志
	 * @return count 表示替换成功的次数 编码格式GBK,如何支持各种编码??
	 */

	public static StringBuffer findAndReplace(StringBuffer buf,
			String startMark, String endMark, String newValue,
			boolean retainMark) {

		if (buf == null || startMark == null || endMark == null
				|| newValue == null) {
			/*
			 * for debug if(buf==null) { System.out.println(" buf不能为空值."); }
			 * if(newValue==null) { System.out.println("newValue不能为空值."); }
			 * if(startMark==null) { System.out.println("startMark不能为空值."); }
			 * if(endMark==null) { System.out.println("endMark不能为空值."); }
			 */
			return buf; // 不满足操作条件,原样返回,
		}

		int from = 0, to = 0;
		// 可能有多对.
		while (true) {
			from = buf.indexOf(startMark, from);
			if (from >= 0) {
				to = buf.indexOf(endMark, from + startMark.length());
			} else {
				break;
			}

			if (from >= 0 && to > from) {
				if (retainMark) // 保留标志
				{
					buf = new StringBuffer(buf.substring(0,
							from + startMark.length())
							+ newValue + buf.substring(to));
				} else // 删除标志
				{
					buf = new StringBuffer(buf.substring(0, from) + newValue
							+ buf.substring(to + endMark.length()));
				}
				from = to + endMark.length();
			} else {
				break;
			}
		}
		return buf;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// String fstr="D:\\work\\www\\www.541fuwu.com\\index.html";
		//
		//
		// StringBuffer buf = readFileToBuffer(fstr,"GBK");
		// findModelInfo(buf);
		//
		// if(1==1)
		// {
		// return;
		// }

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
		HealthInfoPublishWorker worker = null;
		try {
			worker = new HealthInfoPublishWorker(propertiesFileNameWithFullPath);
			worker.start();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
