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
	private int updateInfoCount = 2; // ÿ�θ���5����Ϣ�����Ŀ¼����updateInfoCount�ʹ�ÿ��Ŀ¼ѡһ������
	private int waitInSeconds = 60 * 60;// û����ʱ�ȴ�1��Сʱ.
	private int updateMethod; // ������Ϣ�ķ���
	private CatalogTree ct;
	private CatalogTreeView ctv;
	private StringBuffer model;
	private StringBuffer dirModel;
	private int dictionaryFolderID;
	private String propertiesFileNameWithFullPath;
	private String latestInfoScriptFileName; // ������Ϣ��JavaScript�ű�
	private String hotInfoScriptFileName; // ��������ߵ���ϢJavaScript�ű�
	private long latestNotifyTimeInMilliseconds = 0;
	private Properties profile;

	public HealthInfoPublishWorker(String propertiesFileNameWithFullPath)
			throws Exception {
		this.propertiesFileNameWithFullPath = propertiesFileNameWithFullPath;
	}

	/**
	 * ֪ͨ�û�����Ϣ�������ľ����Ѿ�����ָ������ֵ�� newInfoCount Ŀǰʣ�µ���Ϣ����.
	 * 
	 * @param content
	 */
	public void notifyByEmail(int newInfoCount) {
		MailCarrier mc = new MailCarrier(profile.getProperty("mailHost"), 25,
				"smtp", profile.getProperty("senderEmail"),
				profile.getProperty("senderEmailPassword"), true);
		/**
		 * ��ʣ�¶�����ͺľ�.
		 */
		double secondsLeft = newInfoCount * .10 / this.updateInfoCount
				* this.waitInSeconds;
		// ʲôʱ��ľ�.
		Timestamp futureTime = new Timestamp(System.currentTimeMillis()
				+ (long) (secondsLeft * 1000));

		Mail mail = new Mail();
		mail.setSender(profile.getProperty("senderEmail"));
		mail.addReceiver(profile.getProperty("executorEmail"));

		mail.setSubject(profile.getProperty("website") + "��վ��Ϣ��ӽ���֪ͨ");
		mail.setText(" �𾴵�" + profile.getProperty("executorEmail")
				+ " \n\r    ��վ" + profile.getProperty("website") + "����ӵĿ����Ϣ��:"
				+ newInfoCount + "�Ѿ�����ָ������ֵ:"
				+ profile.getProperty("alertInfoCount") + " �� " + futureTime
				+ " ʱ����Ϣ��潫��������. ������ؾ��찲����ӣ�������վ�Զ������ж�.");
		mail.setContentEncoding("GBK");
		// mail.setFormat("text/html"); //text/html
		mail.setNotification(true);
		mc.send(mail);
	}

	/***
	 * ��һ���ļ������ڴ��StringBuffer ������.
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
				System.out.println("��ȡ�ļ�,�����˴���. fileLength=" + len
						+ "  �Ѿ��ɹ������ֽ�����:" + bytesHasRead + "�ֽ�");
				return null;
			}
		} catch (IOException e) {
			System.out.println(e);
		}
		try {
			model = new StringBuffer(new String(fileContent, encoding)); // ??ΪʲôGBK������?
		} catch (Exception e) {
			System.out.println("����:" + encoding + "��֧��");
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
			System.out.println("error! catalogTreeView ����ʧ��.");
			return;
		}

		if (catalogTreeView != null) {
			this.ct = CatalogTree.loadFromDatabase(catalogTreeView); // root
			currentCatalogTree = ct; // root
			this.ctv = catalogTreeView;
		}
		if (ct == null) {
			System.out.println("catalogTree ����ʧ��.");
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
		// 2.��ȡԭ���ļ����ݵ�һ��String����
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
				System.out.println("�ļ���ȡ����.");
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
	 * ��ȡδ��������Ϣ����.
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
	 * �ҳ�ģ�����û��Զ������Ϣ
	 * 
	 * @param model
	 * @return
	 */
	public static String[][] findModelInfo(StringBuffer model) {
		// <!--���俪ʼ--titleKeyword=�ǽ���--listSize=7--maxwidth=16-->
		// <!--�������--titleKeyword=�ǽ���--listSize=7--maxwidth=16-->

		String startMark = "<!--���俪ʼ--titleKeyword=";
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
					System.out.println("�滻ģ��ʱ,ģ���ʽ�����д���.");
				}
			}
		}

		if (successCount != tmpV.size() && successCount > 0)// �д���.
		{
			modelInfo = null;
		}
		return modelInfo;
	}

	/***
	 * ������ҳ��Ϣ,�����û��Լ������ģ��
	 */
	public void updateHomePage() {
		// ������ҳ��������ӵ����Ͽ�����������֪��.
		String homepageFile = ctv.getCatalogRootInSystemDir()
				+ java.io.File.separator + "index.html";
		StringBuffer modelContent = new StringBuffer(
				readTextFileContent(homepageFile));
		String[][] modelInfo = findModelInfo(modelContent);

		if (modelInfo == null || modelInfo.length < 1) {
			System.out
					.println("��ҳ����ʧ��.if(modelInfo==null || modelInfo.length<1)");
			return;
		}

		Info[] infos = null;
		for (int i = 0; i < modelInfo.length; i++) {
			String keyword = modelInfo[i][0];
			int listSize = Integer.parseInt(modelInfo[i][1]);
			int maxwidth = Integer.parseInt(modelInfo[i][2]);

			// <!--���俪ʼ--titleKeyword=�ǽ���--listSize=7--maxwidth=16-->
			// <!--�������--titleKeyword=�ǽ���--listSize=7--maxwidth=16-->
			String startMark = "<!--���俪ʼ--titleKeyword=" + keyword
					+ "--listSize=" + listSize + "--maxwidth=" + maxwidth
					+ "-->";
			String endMark = "<!--�������--titleKeyword=" + keyword
					+ "--listSize=" + listSize + "--maxwidth=" + maxwidth
					+ "-->";
			infos = HealthHTMLMaker.getLatestInfoFromWebSiteByKeyword(keyword,
					ctv, listSize);

			// for debug
			// if("latestInfo".equals(keyword))
			{

				if (modelContent.indexOf(startMark) < 0) {
					System.out.println("modelContent �Ҳ��� ���:" + startMark);
				}

				if (modelContent.indexOf(endMark) < 0) {
					System.out.println("modelContent �Ҳ��� ���:" + endMark);
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
			// href="http://www.541fuwu.com/health/jibingdaquan/yajiankang/100_143_53.html">�����ǽ�������̽��</a>
			for (int j = 0; j < infos.length; j++) {
				String tmpTitle = infos[j].getTitle();
				if (tmpTitle.getBytes().length > maxwidth) {
					tmpTitle = truncate(tmpTitle, maxwidth * 2); // һ������2���ֽ�.
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
						+ endMark + " ֮��������Ѿ�����.\n itemContent=" + itemContent);
			}
		}

		// д���ļ���.
		try {
			FileOutputStream fout = new FileOutputStream(homepageFile);
			fout.write(modelContent.toString().getBytes("GBK"));
			fout.close();
			System.out.println("��ҳhttp://www.541fuwu.com �Ѿ����³ɹ�."
					+ homepageFile);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void generateJavaScript() {
		// 1.���·�����5����Ϣ��������Ϣ��
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
				System.out.println(this.latestInfoScriptFileName + " �ű��ɹ�����.");
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		/*
		 * <ul> <li><a href=
		 * "http://www.541fuwu.com/health/jibingdaquan/zhongliukefenlei/100_148_1010.html"
		 * >Ԥ���������ճ�����</a> <li><a href=
		 * "http://www.541fuwu.com/health/jibingdaquan/yajiankang/100_143_60.html"
		 * >ʲô�ǡ����ǽ�����״̬��</a> </ul>
		 */

		// 2.������������Ϣ��������Ϣ��

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
				System.out.println(this.hotInfoScriptFileName + " �ű��ɹ�����.");
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
		// �ҵ�ָ������Ϣ�����ɾ�̬HTML��ͬʱ�޸�״̬.
	}

	public void work() {
		publish();
		generateJavaScript();
		updateHomePage();
		check();
	}

	/**
	 * ��������Ƿ��㹻�������������Ϸ��ʼ�֪ͨ.
	 */
	public void check() {
		// 1.�ж����һ��֪ͨ�������Ƿ�֪ͨ��������Ѿ�֪ͨ�����Ͳ���֪ͨ��
		long notifyTimeSpanInSecond = Integer.parseInt(this.profile
				.getProperty("notifyTimeSpanInSecond"));
		if (System.currentTimeMillis() - this.latestNotifyTimeInMilliseconds < notifyTimeSpanInSecond * 1000) {
			return;
		}

		// 2.��û��֪ͨ�������ʣ�����Ϣ���Ƿ����ָ������ֵ.
		int newInfoCount = this.getNewInfoCount(); // �����ѯʱ�ȵ�һ���Ϻķ�ʱ���,����ÿ������ֻ��һ��,���͸���.
		int alertInfoCount = 0;

		try {
			alertInfoCount = Integer.parseInt(this.profile
					.getProperty("alertInfoCount"));
		} catch (Exception e) {
			System.out.println(e);
		}

		// ����ָ����ʱ���֪ͨ����������֪ͨ��ͣ.
		if (newInfoCount <= alertInfoCount) {
			this.notifyByEmail(newInfoCount);
			this.latestNotifyTimeInMilliseconds = System.currentTimeMillis();
		}
	}

	/**
	 * �ض��ַ��������ǲ���Ѻ��ֽس�2��.
	 * 
	 * @param str
	 *            �����Ҫ������ַ���.
	 * @param limitBytes
	 *            ����ֽ���.
	 * @return
	 */
	public static String truncate(String str, int limitBytes) {
		if (str == null) {
			return null;
		}

		// û�����,��ֱ�ӷ���ԭ����ֵ.
		if (str.getBytes().length <= limitBytes) {
			return str;
		}

		// str ������ָ�����ֽ���,Ҫ�ض�.

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
			waitInSeconds = 600; // 10����.
		}
		while (true) {
			System.out.println("����ʱ��:"
					+ new Timestamp(System.currentTimeMillis()));
			work();
			try {
				System.out.println("��Ϣ"
						+ waitInSeconds
						+ "�룬�����Ƿ���δ��������Ϣ���Է���.�´μ��ʱ��:"
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
	 *            �ַ���
	 * @param startMark
	 * @param endMark
	 * @param newValue
	 * @param retainMark
	 *            ������־
	 * @return count ��ʾ�滻�ɹ��Ĵ��� �����ʽGBK,���֧�ָ��ֱ���??
	 */

	public static StringBuffer findAndReplace(StringBuffer buf,
			String startMark, String endMark, String newValue,
			boolean retainMark) {

		if (buf == null || startMark == null || endMark == null
				|| newValue == null) {
			/*
			 * for debug if(buf==null) { System.out.println(" buf����Ϊ��ֵ."); }
			 * if(newValue==null) { System.out.println("newValue����Ϊ��ֵ."); }
			 * if(startMark==null) { System.out.println("startMark����Ϊ��ֵ."); }
			 * if(endMark==null) { System.out.println("endMark����Ϊ��ֵ."); }
			 */
			return buf; // �������������,ԭ������,
		}

		int from = 0, to = 0;
		// �����ж��.
		while (true) {
			from = buf.indexOf(startMark, from);
			if (from >= 0) {
				to = buf.indexOf(endMark, from + startMark.length());
			} else {
				break;
			}

			if (from >= 0 && to > from) {
				if (retainMark) // ������־
				{
					buf = new StringBuffer(buf.substring(0,
							from + startMark.length())
							+ newValue + buf.substring(to));
				} else // ɾ����־
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
