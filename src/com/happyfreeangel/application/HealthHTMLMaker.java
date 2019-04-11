package com.happyfreeangel.application;

/**
 * <p>Title: ħ��</p>
 * <p>Description: ħ��</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author not attributable
 * @version 0.1
 */
import java.util.*;
import java.sql.*;
import java.io.*;

import happy.web.*;

import java.util.regex.*;

import com.happyfreeangel.magictree.*;

import net.snapbug.util.dbtool.*;

class VirtualWebInfo implements Comparable<VirtualWebInfo>{
	private int   infoId;
	private String title;
	private String virtualURL;
	private Timestamp insertTime;
	
	public VirtualWebInfo(){}
	
	public VirtualWebInfo(int infoId,String title,String virtualURL,Timestamp insertTime){
		this.infoId=infoId;
		this.title=title;
		this.virtualURL=virtualURL;
		this.insertTime=insertTime;
	}
	
	public int compareTo(VirtualWebInfo other){
		if(other==null){
			return 1;
		}
		if(other.insertTime==null){
			return 1;
		}
		if(this.insertTime==null){
			return -1;
		}
		return (int)(this.insertTime.getTime()-other.insertTime.getTime());
	}
	
	public int getInfoId(){
		return infoId;
	}
	
	public void setInfoId(int infoId){
		this.infoId=infoId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getVirtualURL() {
		return virtualURL;
	}

	public void setVirtualURL(String virtualURL) {
		this.virtualURL = virtualURL;
	}

	public Timestamp getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Timestamp insertTime) {
		this.insertTime = insertTime;
	}
}

public class HealthHTMLMaker implements HTMLMaker {
	private static String lineSeparator = System.getProperty("line.separator");

	private static int maxKeywordExplainCount = 1;// ��ʾÿƪ���¹ؼ��������͵Ĵ���.
	private static Hashtable<String, String> dictionary = new Hashtable<String, String>(); // IT�ֵ�,ÿ��1Сʱ����һ��.
	private static int dictionaryFolderID = 379; // �ֵ�����ID�š��ֵ��Ǳ������е�һ����Ŀ¼���û�����ָ����
	private static Timestamp lastestUpdateTime = new Timestamp(
			System.currentTimeMillis());
	private static boolean debug = false; // �ж��Ƿ��ǵ���ģʽ,��Ҫ���������Ϣ,�жϴ���������.
	private static String[] keywords = null;

	public static String charToUnicode(char c) {
		String unicodeStr = "&#" + (int) (c) + ";";

		return unicodeStr;
	}

	public static boolean guess(double rate) {
		double p = rate * 100;

		double current = Math.random() * 100;

		if (current < p) {
			return true;
		}
		return false;
	}

	/***
	 * ��������ַ���ת��ΪUnicode
	 * 
	 * @param str
	 * @return
	 */
	public static String StringtoUnicodeByRate(String content, double rate) {
		if (content == null) {
			System.out.println("error. content can not be null.");
			return null;
		}
		StringBuffer buf = new StringBuffer();
		String temp = null;
		for (int i = 0; i < content.length(); i++) {
			if (guess(rate)) {
				temp = charToUnicode(content.charAt(i));
			} else {
				temp = content.charAt(i) + "";
			}
			buf.append(temp);

		}
		return buf.toString();
	}

	/**
	 * ��һ����Ϣ�ŵ�ָ����Ŀ¼��,һ����Ϣ����ͬʱ�ŵ���ͬ��Ŀ¼��.
	 * 
	 * @param ctv
	 * @param ct
	 * @param info
	 * @param keywords
	 */
	public static void classifyInfo(CatalogTreeView ctv, CatalogTree ct,
			Info info, String keywords) {
		// ���û����ʾ�ؼ���,���Ŀ¼������Ϊ��Ϣ�ؼ���,���ռ���δ�������Ϣ��û�йؼ��ʵ�.
		if (keywords == null) {
			keywords = ct.getName();
		}
		int infoID = 0;
		String maxIDSQL = " select max(infoID) from Info where infoID<1000000";

		try {
			Statement stmt11 = ctv.getPool().createStatement();
			ResultSet rs11 = stmt11.executeQuery(maxIDSQL);
			if (rs11.next()) {
				infoID = rs11.getInt(1);
			}
			rs11.close();
		} catch (SQLException e) {
			System.out.println(e);
		}
		if (infoID < 0) {
			infoID = 0;
		}
		infoID = infoID + 1; // �Ѿ����ڵķ�������ϢID+1

		String updateSQL = "update Info set infoID=?, infoType = ?,keywords = ?,latestUpdateTime = ?,latestModifiedMemberID = ? where infoID = ? "; // and
		int robotID = 1000;// ������ID
		int memberID = robotID;
		try {

			PreparedStatement pstmt = ctv.getPool().prepareStatement(updateSQL);
			pstmt.setInt(1, infoID);
			pstmt.setLong(2, ct.getID()); // ??? info.getInfoTye()???
			pstmt.setString(3, keywords);
			Timestamp latestUpdateTime = new Timestamp(
					System.currentTimeMillis());
			pstmt.setTimestamp(4, latestUpdateTime);

			int latestModifiedMemberID = memberID;
			pstmt.setString(5, latestModifiedMemberID + "");
			pstmt.setInt(6, info.getInfoID());
			int affectedRecordCount = pstmt.executeUpdate();
			pstmt.close();

			if (affectedRecordCount == 1) {
				System.out.println("<div align=center>��¼�Ѿ��ɹ�����.ʱ��:"
						+ new java.sql.Timestamp(System.currentTimeMillis())
						+ "</div>");

				String insertInfoCatalogSQL = "insert into "
						+ ctv.getCatalogTreeTableName()
						+ "(catalogTreeID,folderID,infoID) values(?,?,?);";

				try {
					PreparedStatement pstmt2 = ctv.getPool().prepareStatement(
							insertInfoCatalogSQL);
					pstmt2.setLong(1, ctv.getCatalogTreeID());
					pstmt2.setLong(2, ct.getID());
					pstmt2.setLong(3, infoID);
					int affectedRecordCount2 = pstmt2.executeUpdate();
					if (affectedRecordCount2 != 1) {
						System.out.println("failed. insertInfoCatalogSQL="
								+ insertInfoCatalogSQL + "  infoID=" + infoID
								+ " currentCatalogTreeID="
								+ ctv.getCatalogTreeID() + " infoType="
								+ ct.getID());
					}
				} catch (SQLException ex) {
					System.out.println(ex);
				}
			} else {
				System.out.println("<br>����ʧ��! updateSQL=" + updateSQL);

			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	/**
	 * ʵ����Ϣ�Զ����� �������������������,
	 */
	public static void smartClassifyInfo(CatalogTreeView ctv, CatalogTree ct,
			Info info) {
		if (info == null) {
			return;
		}

		if (ct.getSons().size() > 0) {
			Iterator<CatalogTree> itor = ct.getSons().iterator();
			while (itor.hasNext()) {
				CatalogTree currentCatalogTree = (CatalogTree) itor.next();
				smartClassifyInfo(ctv, currentCatalogTree, info);
			}
		}

		// ͨ�������ж��Ƿ����,����Ŀ¼�ؼ��ֿ����Ǻ��ʣ����򲻺��ʡ�
		CatalogTree dic = ct.getRoot().findByID(dictionaryFolderID); //

		if (info.getTitle().indexOf(ct.getName()) >= 0) {
			if (ct.isOffspring(dic)) // ��Ϣ�������Ŀ¼�����ǲ��������ʵ�Ŀ¼
			{
				if (info.getTitle().equals(ct.getName())) {
					classifyInfo(ctv, ct, info, null);
					System.out.println("infoID=" + info.getInfoID() + " title="
							+ info.getTitle() + " �ŵ�Ŀ¼:"
							+ ct.getFullCatalogPath() + " Ŀ¼ID:" + ct.getID());
				}
			} else {
				classifyInfo(ctv, ct, info, null);
				System.out.println("infoID=" + info.getInfoID() + " title="
						+ info.getTitle() + " �ŵ�Ŀ¼:" + ct.getFullCatalogPath()
						+ " Ŀ¼ID:" + ct.getID());
			}
		}
		return;
	}

	/**
	 * ʵ����Ϣ�Զ����� �������������������, ����������Ϣ���������.
	 */
	public static void smartClassifyInfo(CatalogTreeView ctv, CatalogTree ct,
			Set<Info> infos) {
		if (infos == null) {
			return;
		}

		if (ct.getSons().size() > 0) {
			Iterator<CatalogTree> itor = ct.getSons().iterator();
			while (itor.hasNext()) {
				CatalogTree currentCatalogTree = (CatalogTree) itor.next();
				smartClassifyInfo(ctv, currentCatalogTree, infos);
			}
		}

		Iterator<Info> itor = infos.iterator();
		Set<Info> visitedSet = new HashSet<Info>();
		// �ֵ�Ŀ¼
		CatalogTree dic = ct.getRoot().findByID(dictionaryFolderID); //
		while (itor.hasNext()) {
			Info info = (Info) itor.next();
			// ͨ�������ж��Ƿ����,����Ŀ¼�ؼ��ֿ����Ǻ��ʣ����򲻺��ʡ�
			if (info.getTitle().indexOf(ct.getName()) >= 0) {
				if (ct.isOffspring(dic)) // ��Ϣ�������Ŀ¼�����ǲ��������ʵ�Ŀ¼
				{
					if (info.getTitle().equals(ct.getName())) {
						classifyInfo(ctv, ct, info, null);
						System.out.println("infoID=" + info.getInfoID()
								+ " title=" + info.getTitle() + " �ŵ�Ŀ¼:"
								+ ct.getFullCatalogPath() + " Ŀ¼ID:"
								+ ct.getID());
					}
				} else {
					classifyInfo(ctv, ct, info, null);
					System.out.println("infoID=" + info.getInfoID() + " title="
							+ info.getTitle() + " �ŵ�Ŀ¼:"
							+ ct.getFullCatalogPath() + " Ŀ¼ID:" + ct.getID());
				}
				visitedSet.add(info);
			}
		}
		if (visitedSet.size() > 0) {
			infos.removeAll(visitedSet);
		}
		return;
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
			fin.close();

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

	/**
	 * 
	 * @param propertiesFileNameWithFullPath
	 * @throws Exception
	 */
	public void updateInfo(String propertiesFileNameWithFullPath)
			throws Exception {
		Properties profile = new Properties();
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
		CatalogTree root = null, currentCatalogTree = null;

		catalogTreeView = CatalogTreeView.loadFromDatabase(pool, catalogTreeID,
				catalogTreeViewTableName);
		if (catalogTreeView == null) {
			System.out.println("error! catalogTreeView ����ʧ��.");
			return;
		}

		if (catalogTreeView != null) {
			root = CatalogTree.loadFromDatabase(catalogTreeView);
			currentCatalogTree = root;
		}
		if (root == null) {
			System.out.println("catalogTree ����ʧ��.");
			return;
		}

		// 2009-07-01 shanghai happy addd
		// �ֵ�����Ŀ¼ID
		dictionaryFolderID = Integer.parseInt(profile
				.getProperty("dictionaryFolderID"));

		debug = Boolean.parseBoolean(profile.getProperty("debug")); // true or
																	// false

		StringBuffer model = readFileToBuffer(
				profile.getProperty("modelInfoFile"),
				profile.getProperty("modelInfoFileEncoding"));
		StringBuffer dirModel = readFileToBuffer(
				profile.getProperty("modelDirFile"),
				profile.getProperty("modelDirFileEncoding"));

		String updateDirectory = profile.getProperty("updateDirectory");
		String[] whichDir = null;
		if (updateDirectory != null) {
			whichDir = updateDirectory.split(",");
		}

		if (whichDir == null || whichDir.length < 1) {
			System.out.println("û��ʲô��Ҫ���µ�.");
			return;
		}

		// һ��������ָ����Ŀ¼.
		for (int i = 0; i < whichDir.length; i++) {
			if (currentCatalogTree != null) {
				currentCatalogTree = root.getRoot().findByID(
						Integer.parseInt(whichDir[i]));
				String errorReport = safeUpdateTotalInfoToHTMLForUserView(
						currentCatalogTree,
						catalogTreeView,
						model,
						dirModel,
						Boolean.parseBoolean(profile.getProperty("updateInfo")),
						Boolean.parseBoolean(profile.getProperty("updateDir")));
				System.out.println("<div align=center>" + errorReport
						+ "</div>");
			}
		}
	}

	/**
	 * 
	 * @param propertiesFileNameWithFullPath
	 * @throws Exception
	 */
	public void smartClassifyInfo(String propertiesFileNameWithFullPath)
			throws Exception {
		Properties profile = new Properties();
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
		CatalogTree root = null;

		catalogTreeView = CatalogTreeView.loadFromDatabase(pool, catalogTreeID,
				catalogTreeViewTableName);
		if (catalogTreeView == null) {
			System.out.println("error! catalogTreeView ����ʧ��.");
			return;
		}

		if (catalogTreeView != null) {
			root = CatalogTree.loadFromDatabase(catalogTreeView);
		}
		if (root == null) {
			System.out.println("catalogTree ����ʧ��.");
			return;
		}

		// ���ε��ʵ�,�ʵ�����˹�����,��Ϊ��Ӱ�쵽ȫ��.
		dictionaryFolderID = Integer.parseInt(profile
				.getProperty("dictionaryFolderID"));
		debug = Boolean.parseBoolean(profile.getProperty("debug")); // true or
																	// false
		CatalogTree dic = root.findByID(dictionaryFolderID);
		CatalogTree father = (CatalogTree) dic.getFather();
		father.removeSon(dic);

		// Ҫ���������Ϣ
		int classifyInfoCount = 1;

		classifyInfoCount = Integer.parseInt(profile
				.getProperty("classifyInfoCount"));
		Info[] infos = getUnClassifyInfo(catalogTreeView, classifyInfoCount);// getLatestInfoFromWebSite(catalogTreeView,
																				// classifyInfoCount);

		System.out.println("classifyInfoCount=" + classifyInfoCount);
		if (infos != null) {
			System.out.println("��ȡ��InfoCount=" + infos.length);
		}
		if (infos != null) {
			for (int i = 0; i < infos.length; i++) {
				System.out.println(infos[i].getInfoID() + "<---->"
						+ infos[i].getTitle());
				smartClassifyInfo(catalogTreeView, root, infos[i]);
			}
		}

	}

	public HealthHTMLMaker() {
	}

	public static boolean isEnglishKeyword(String keyword) {
		if (keyword == null) {
			return false;
		}
		String regEx = "^[a-zA-Z]"; // ��ʾa��f
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(keyword);
		if (m != null) {
			return m.find();
		}
		return false;
	}

	/**
	 * �ж�������ַ��Ƿ��Ǻ���.
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isHanZi(char c) {
		return (c + "").matches("[\\u4e00-\\u9fa5]+");
	}

	public static void test() {

		Hashtable<String, String> dic = new Hashtable<String, String>();
		dic.put("��վ���",
				"http://www.541service.com/design/dictionary/100_28_150.html");
		dic.put("��������",
				"http://www.541service.com/design/dictionary/100_28_176.html");
		dic.put("��־���",
				"http://www.541service.com/design/dictionary/100_28_131.html");
		dic.put("��������",
				"http://www.541service.com/design/dictionary/100_28_176.html");
		dic.put("VI���",
				"http://www.541service.com/design/dictionary/100_28_129.html");
		dic.put("Dreamweaver",
				"http://www.541service.com/design/dictionary/100_28_111.html");
		dic.put("�û�����",
				"http://www.541service.com/design/dictionary/100_28_150.html");

		String content = "���ڴ����������˵����վ��ƾ�����ҳ������ͨ���������棬\n\r �ܶ�ѧУ������վ��ƣ������õ����Dreamweaver�����ǿ���֪��VI��ƣ��û�������һ�ִ����۵����û�ʹ��һ����Ʒ������\n\r�Ĺ����н���������������ܡ�\n\r��Ϊ���Ǵ����۵ģ��ʹ���һ���Ĳ�ȷ�����ء��������Ҳ������ÿ���û�����ʵ�������޷�ͨ������;������ȫģ������ֵġ�\n\r���Ƕ���һ���綨��ȷ���û�Ⱥ�����������û�����Ĺ������ܹ�����������Ƶ�ʵ������ʶ����";

		System.out.println(makeDictionaryLink(content, dic));
	}

	public static void test2() {
		String content = "<br><P>�����ٴ�����������Լ����������Ի�ŧ���ж��׼������֢(�ж�ճ��������Ӳ��)����Ӳ��֢�Լ������Զ����εȡ���΢���������������Ҫ�������ֶΡ���ʮ�������ҹ�����ѧ�ķ�չ����΢��Ƽ�������ߣ������Զ������������ˮƽ���˳���Ľ�����"
				+ "<br><P>����1�������ؽ������ṹ�빦�ܳ�Ϊ��������������Ƶ�����"
				+ "<br><P>��������ѭ֤ҽѧ��׼����ݲ�ͬ����Ͳ��䷶Χѡ����ѵ����Ʋ������𽥳�Ϊ������ٴ�ҽʦ�Ĺ�ʶ������ԭ��Ϊ������ȥ�����䣬����޶ȵر������޸����ؽ�����������̬���ָ�������ж��Ĵ������ܡ�"
				+ "<br><P>����1.1���Ի�ŧ���ж���"
				+ "<br><P>������ʮ������һ�������仯�ǣ���ͳ���ж���ͻ��������Ϊ���ָ����������ʽ��ȡ��������ʽ������ͬʱʩ�й��һ�������ؽ�����С��ͻǻ������޶ȵر����ж��ṹ��Ϊ���Ի�ŧ���ж���������Ƶ������������ڲ���������ͬ�ڽ����������ؽ����ٴ����顣�����ж�ճĤ���ز����没��(ճĤ�������ʺ���������״��Ƥ��)�����ŷֽ׶������������ڹ��ҳ��Σ����������������֯(ͨ���γɹ��ҹǱ���¶)�����˹轺Ĥ(O.1��1.O mm)��ֹճ��������ճĤ�������ָ����Һ���ǻ��6��1 2���º����̽�鲢�����������ؽ����˷������ʹĹܹ����д��ߵ�Ч���Ϻá������������ڵıպ�ʽ������ΪĳЩ���͵�֬���ж��׵�����ѡ��֮һ��֣������ʡ������ȱ��������ʽ����������Ч�������ڽϸߵĵ�֬��������7.7%�� 24.O% �������ϸ�������Ӧ֤�� ���Ϲ��Ҳ��䡢���������ز���֢����Ӳ������ͻ����ѡ��ʩ�У� ��ȷ��Զ��Ч�������ڶ������Ե�֬������������ж���ͻ�׶��ԣ����ѡ����ʽ����һ��������ｨ��������һ�ָĽ�����ʽ���������������ŵ�IBM �������ؼ������ǳ�ֿ����Ϲ�������ͻ���г��������ڣ�������λ���š�����������������ѣ�һ�ڻ���ڹ��ҳ��Ρ����ص��ǳ��ȥ���������ԭ�й���ǻ�ݻ����ؽ��ж�����������IBM�����ĸɶ��ʼ�����������������������ʽ��Ϊ�ٴ��ṩ����һ�����ѡ��IBM��ʽ�ۺ��˿���ʽ������պ�ʽ�������ŵ㣬������Ϊ��֬������������ж���ͻ��������Ƶ�������ʽ���������ж����ǡ� �����ٴ�֢״�����͡���Ĥ����Ϊ�������ж���<a href=\"http://www.541fuwu.com/health/dictionary/medicine/infectious_disease/social_disease/index.html\" class=dictionaryKeyword>�Բ�</a>�䡣��ȫ��������һ��3O6������Ƭ�Ĺ۲�������Ϊ�˲���Ϊ�ʹĹܹ��ܲ��������ϰ鷢���ҵͶȸ�Ⱦ���£�������8.1%�����в�ͬ�̶ȵ�ճĤ���Ըı䣬�����ٴ�ҽʦ�����꾡������ѧ��Ӱ��ѧ��飬�Է��������� ��"
				+ "<br><P>����1.2��Ӳ��֢���ٴ��Զ�Ӳ��֢����Ч������������Ϊ������ʮ������ʽѡ�񡢼�����΢�������˹����Ӧ�÷���������Խ��������Ƶ�Ч��Ҳ����ߡ��������ڻ���ȶ�Ӳ��֢�������ر�������Ǻ�������������ʽ�����1��1 2�꣬������Ч��94.7%��Զ��86.8%������ʽ�ŵ㣺��㡢΢������Ч�ȶ�����ʩ���ٴ����������������Ͻṹ���˶����еװ����������ٶ����鲡�����бȽϣ���������������봫ͳ��ʽ����Ч���������족������װ忪���ͻ���ʽ�˹���ǵ�Ӧ������һʱ�ڵ���Ҫ�ص㡣������������CO2������ǵװ忪����ֲ�����ʽ�˹���ǵľ��顣44���������3���¡�1�꣬�����������31.2dB���˷��ŵ�Ϊ�ǽӴ����׿��ƺ�΢���ԡ��������ڵװ�����������͡���帡���� ������Er-YAG����Ҳ���ڶ�Ӳ��֢������Ӳ��֢�������������������������Ч���������Ǳ�����Co2�����Er-YAG��������ʹ�ò������Ա�֤�ϸ�Ĵ�͸��ȣ���С�������˺ͻ�еϮ�š�"
				+ "<br><P>����1.3�����Զ����Ρ����Ÿ߷ֱ�CTӰ��͵���������Ӧ�ã��Ը��������������ǰ�����ˮƽ������ߣ��������������������½���ͬ�������ؽ�������������ڶ��ҽԺ��չ��������������53��ǰ�Ͼ�·��ֱ��ʽ��������1���⣬52����˳��̽����ҡ��˷�������Ӳ���ͻ����ʣ����1��9�꣬�����ָ������⣬��ͬ�Ρ�����һ�ȱ��������������Ƶ�Ч��������ߡ�����������89��(99��)������ͶӰ��̽���ж�����ɹ�������·�а�ȫ��ʡʱ���������յ��ص㡣"
				+ "<br><P align=right>��ʵϰ�༭�������㣩";

		System.out.println(StringtoUnicodeByRate(content, 0.03));

	}

	public static void main(String[] args) throws Exception {
		test2();
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
		maker.updateInfo(propertiesFileNameWithFullPath);
	}

	/**
	 * 
	 * @param line
	 *            �ַ���
	 * @param keyword
	 *            �ؼ���
	 * @param startMark
	 *            ��ʼ��־
	 * @param endMark
	 *            ������־
	 * @return keywordPos �ж� �Ƿ��� startMark+keyword+endMark ���ֽṹ
	 */
	private static int isKeywordInMark(String line, String keyword,
			String startMark, String endMark) {
		if (line == null || keyword == null || startMark == null
				|| endMark == null) {
			return -1;
		}

		// 1.�����Ͳ�����
		int keywordPos = line.indexOf(keyword);
		if (keywordPos < 0) {
			return -1;
		}

		int startPos = line.indexOf(startMark, 0);
		int endPos = line.indexOf(endMark, keywordPos);

		// 2.�Ѵ������,�������ظ�����
		if (startPos < keywordPos && keywordPos < endPos) {
			return 0;
		}

		// 3.�а���,��������� hkeywordk �������͵ľͲ�Ӧ�ý��͡����keyword��Ӣ�Ŀ�ͷ��Ӣ�Ľ�β��,
		// ���keywordǰ����Ӣ�Ļ�keyword������Ӣ��,�ͷ�������,����-1. //2006-11-09 22:55 happy
		if (keyword.charAt(0) >= 'A' && keyword.charAt(0) <= 'Z'
				|| keyword.charAt(0) >= 'a' && keyword.charAt(0) <= 'z') {
			if (
			// �ؼ���֮ǰ��Ӣ���ַ���
			(keywordPos >= 1 && line.charAt(keywordPos - 1) >= 'A'
					&& line.charAt(keywordPos - 1) <= 'Z' || keywordPos >= 1
					&& line.charAt(keywordPos - 1) >= 'a'
					&& line.charAt(keywordPos - 1) <= 'z')
					|| // //�ؼ���֮���Ӣ���ַ���
						// ��ֹ��ֵ����.
					((keywordPos + keyword.length()) < line.length()
							&& line.charAt(keywordPos + keyword.length()) >= 'A'
							&& line.charAt(keywordPos + keyword.length()) <= 'Z' || (keywordPos + keyword
							.length()) < line.length()
							&& line.charAt(keywordPos + keyword.length()) >= 'a'
							&& line.charAt(keywordPos + keyword.length()) <= 'z')) {
				return -1;
			}
		}

		// 4. ��ȷ�İ�����.

		return keywordPos;
	}

	/**
	 * 
	 * @param currentInfoCatalogTree
	 *            �ʵ�Ŀ¼��
	 * @param ctv
	 *            Ŀ¼������ͼ
	 * @return
	 */
	public static Hashtable<String, String> getDictionay(CatalogTree ct,
			CatalogTreeView ctv) {
		// 1.��λ��IT���� һ�ܸ���һ��.
		if (System.currentTimeMillis() - lastestUpdateTime.getTime() < 1000 * 3600 * 24 * 7
				&& dictionary.size() > 0) {
			return dictionary;
		}

		dictionary = new Hashtable<String, String>(); // ���� IT����, ��ֵ URL��̬��ҳ����

		if (ct == null) {
			System.out
					.println("�ʵ��ǿյ�,public static Hashtable getITDictionay(CatalogTree ct, CatalogTreeView ctv)    CatalogTree ct ==null");
			return dictionary;
		}
		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(ct);
		int alert = 100000; // ����������ֵ,��ϵͳ�϶��������쳣,����ǿ���˳�.
		int count = 0;

		while (stack.size() > 0) {

			CatalogTree tmp = (CatalogTree) stack.pop();
			count++;
			if (tmp != null && tmp.getSons().size() > 0) {
				Iterator<CatalogTree> itor = tmp.getSons().iterator();
				while (itor.hasNext()) {
					stack.push(itor.next());
				}
			}

			if (count > alert) {
				System.out
						.println("ϵͳ�������쳣.com.happyfreeangel.magictree.application.HealthHTMLMaker  public static Hashtable getITDictionay(CatalogTree ct,CatalogTreeView ctv)");
				break;
			}

			// ��ǰ�ڵ�
			// 1.��ӵ�ǰĿ¼������
			if (tmp != null) {
				dictionary.put(tmp.getName(),
						ctv.getURI() + tmp.getVirtualFullWebDirPath()
								+ "/index.html");
			}

			// 2.��ӵ�ǰĿ¼�µ����������
			String SQL = " select Info.title,InfoCatalogTreeHTMLWebPath.virtualWebPath from "
					+ ctv.getCatalogTreeTableName()
					+ ",Info,InfoCatalogTreeHTMLWebPath where "
					+ ctv.getCatalogTreeTableName()
					+ ".catalogTreeID = "
					+ ctv.getCatalogTreeID()
					+ " and "
					+ ctv.getCatalogTreeTableName()
					+ ".folderID="
					+ tmp.getID()
					+ " and "
					+ ctv.getCatalogTreeTableName()
					+ ".infoID = Info.infoID and InfoCatalogTreeHTMLWebPath.infoID=Info.infoID ";
			String title = null, htmlURL = null;
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = ctv.getPool().createStatement();
				rs = stmt.executeQuery(SQL);
				while (rs.next()) {
					title = rs.getString("Info.title");
					htmlURL = rs
							.getString("InfoCatalogTreeHTMLWebPath.virtualWebPath");
					 dictionary.put(title,ctv.getURI() + htmlURL); // ������ظ�,�����������.
					// if (chongtu != null)
					// {
					// System.out.println(" ����ͬ����������: " + title + "   " +
					// chongtu);
					// }
				}
			} catch (SQLException e) {
				System.out.println(e);
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException e) {
					System.out.println(e);
				}
			}
		}
		// ��ʼ��keywords�б�

		keywords = getDictionaryKeys();
		return dictionary;
	}

	/**
	 * �Դʵ䰴�չؼ��ʵĳ���������.
	 * 
	 * @return
	 */
	public static String[] getDictionaryKeys() {
		if (keywords != null) {
			return keywords;
		}
		Object[] tmpitems = dictionary.keySet().toArray();
		keywords = new String[tmpitems.length];

		// ��items�ַ�����������ַ�������������,������������ǰ��,���2�����ʵȳ�,�����ȡһ��,
		for (int p = 0; p < keywords.length; p++) {
			keywords[p] = (String) tmpitems[p];
		}
		// ð�ݷ�����
		for (int m = 0; m < keywords.length; m++) {
			for (int n = m + 1; n < keywords.length; n++) {
				if (keywords[m].length() < keywords[n].length()) {
					String tmp = keywords[n];
					keywords[n] = keywords[m];
					keywords[m] = tmp;
				}
			}
		}
		return keywords;
	}

	/**
	 * �ж�������ַ��ǵ��ֽڵ�Ӣ�Ļ�����
	 */
	public static boolean isNumOrABC(char c) {
		if (c > 255) // �ǰ����.
		{
			return false;
		}
		if (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A'
				&& c <= 'Z') {
			return true;
		}

		return false;
	}

	/***
	 * �ж�һ��Ӣ�ﵥ���ھ������Ƿ�߽������û�����塣 A dog is black. Adog is black.
	 * 
	 * @param line
	 * @param keyword
	 * @return
	 */
	public static boolean isEnglishWordBoardClear(String line, String keyword) {
		// 1.���ж��Ƿ���Ӣ�ﵥ�ʣ���������������˳�.
		if (!isEnglishKeyword(keyword)) {
			return false;
		}
		boolean isOK = false;
		int from = line.indexOf(keyword);
		int to = from + keyword.length();

		// ȷ���߽��Ƿ������

		// 1.�ؼ�����һ�п�ͷ.
		if (from == 0) {
			// a.һ�о�һ���ؼ���.
			if (line.length() == keyword.length()) {
				isOK = true;
			}
			// b.�ؼ��ʺ����ǿո���� Ҳ�����
			if (line.length() > keyword.length()) {
				if (!isNumOrABC(line.charAt(keyword.length()))
						|| isHanZi(line.charAt(keyword.length()))) {
					isOK = true;
				}
			}
		}
		// 2.�ؼ�����һ���м�.
		else if (from > 0 && to < (line.length() - 1)) {
			if ((!isNumOrABC(line.charAt(from - 1)) || isHanZi(line
					.charAt(from - 1)))
					&& (!isNumOrABC(line.charAt(to)) || isHanZi(line.charAt(to)))) {
				isOK = true;
			}
		}
		// 3.�ؼ�����һ��ĩβ
		else if (from > 0 && to == (line.length() - 1)) {
			if (!isNumOrABC(line.charAt(from - 1))
					|| isHanZi(line.charAt(from - 1))) {
				isOK = true;
			}
		}
		return isOK;
	}

	/***
	 * ��ָ�����ı����ؼ���Web����
	 * 
	 * @return
	 */
	public static String makeDictionaryLink(String content,
			Hashtable<String, String> dic) {
		StringBuffer contentBuf = new StringBuffer();
		BufferedReader buf = new BufferedReader(new StringReader(content));
		String line = null;
		String tmpLine = null;
		int ignorelevel = 0;
		int alertCount = 100000;
		int p = 0;

		dictionary = dic;
		if (keywords == null) {
			keywords = getDictionaryKeys();
		}
		// ��¼�ؼ����Ƿ��Ѿ����ʹ�,һһ��Ӧ
		// keywords[]
		// visitedkeywords[]
		int[] visitedKeyword = new int[keywords.length];
		for (int i = 0; i < visitedKeyword.length; i++) {
			visitedKeyword[i] = 0; // 0��ʾ��δ�ɹ����͹�����0��������ʾ�Ѿ��ɹ����ʵĴ���.
		}
		while (true) {
			p++;
			if (p > alertCount) {
				System.out.println("ϵͳ������ѭ��.");
				break;
			}
			try {
				line = buf.readLine();
			} catch (IOException e) {
				System.out.println(e);
			}

			if (line == null) {
				break;
			}
			tmpLine = line.toLowerCase();
			if (tmpLine.indexOf("<table") >= 0) {
				ignorelevel++;
			} else if (tmpLine.indexOf("</table>") >= 0) {
				ignorelevel--;
			}

			if (ignorelevel == 0) // ��ʾ���ڲ��ڱ������,���ܱ���ж��ٲ�Ƕ��.
			{
				
				//ʹ���˱༭���Ͳ��������򵥵����,�������
//				if (tmpLine.indexOf("<br>") < 0) {
//					line = "<br>" + line;
//				}
			}

			// ??��Ҫ����,����ȷһ��. ��ÿһ���ı�������Ƿ����keywords������Ĺؼ��ʣ��ҷ����趨�ı�׼��
			for (int h = 0; h < keywords.length; h++) {
				int keywordPos = isKeywordInMark(line, keywords[h],
						"<a href=\"", "</a>");
				if (keywordPos <= 0) {
					continue;
				}

				// �������ȷʵ�����йؼ���,������Ӣ��ؼ��ʱ߽粻��������⣬��Ҫע�͡�
				// 1.�����Ӣ��ؼ���,�ұ߽����.
				if (isEnglishKeyword(keywords[h])
						&& (!isEnglishWordBoardClear(line, keywords[h]))) {
					// for debug
					System.out
							.println("!!!!!!!!!������Ӣ��ؼ��ʣ��߽粻��������������ͣ������û��������. keyword="
									+ keywords[h] + " line=" + line);
				} else // 2.����Ǻ���ؼ���
				{
					if (visitedKeyword[h] < maxKeywordExplainCount) {
						line = line.substring(0, keywordPos)
								+ "<a href=\""
								+ dictionary.get(keywords[h])
								+ "\" class=dictionaryKeyword>"
								+ keywords[h]
								+ "</a>"
								+ line.substring(keywordPos
										+ keywords[h].length());
						visitedKeyword[h]++; // ��¼�ɹ�����Ϊ,���ϵ�����.
												// //ÿ���ؼ�����ÿ��ҳ��ֻ��maxKeywordExplainCount�ν���.
					}
				}
			}

			contentBuf.append(line + "\n");
		} // end of while

		return contentBuf.toString();
	}

	/**
	 * ��һ����Ϣת��ΪHTML��ʽ.
	 * 
	 * @param t
	 *            CatalogTree
	 * @param infoID
	 *            int
	 */
	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, int infoID, StringBuffer model) {
		// System.out.println(" ���㿴����Щ����ʱ,infoID="+infoID+" ���ڱ�ת��,��Ҫ��ʼ.");
		int viewedCount = 0;
		String title = null, infoType_str = null, content = null, author = null, format_str = null, languageType_str = null;
		String keywords = null, derivation = null, sourceURI = null, whoCollectIt = null, latestModifiedMemberID = null, activeStatus = null;
		String hasVerify_str = null, securityLevel_str = null, isAuthorship_str = null;
		Timestamp publishedDate = null, expiredDate = null, latestUpdateTime = null, insertedTime = null;

		ctv.getCatalogTreeDefinitionTableName();
		ctv.getCatalogTreeTableName();

		String SQL = " select Info.infoID,title,content,author,publishedDate,expiredDate,keywords,derivation,sourceURI,whoCollectIt,latestUpdateTime,insertedTime,viewedCount,latestModifiedMemberID,hasVerify,activeStatus,securityLevel,isAuthorship from Info "
				+ " where Info.infoType = "
				+ currentInfoCatalogTree.getID()
				+ " and Info.infoID=" + infoID;

		Statement stmt = null;
		ResultSet rs = null;

		boolean hasFound = false;

		// ��ȡIT�ֵ�. folderID = �ֵ��Ŀ¼ID
		Hashtable<String, String> webDesignDictionary = getDictionay(
				currentInfoCatalogTree.getRoot().findByID(dictionaryFolderID),
				ctv);
		if (debug) {
			System.out.println("\n�ʵ��б�����: dictionaryFolderID="
					+ dictionaryFolderID + "\n" + webDesignDictionary);
		}

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(SQL);

			if (rs != null && rs.next()) {
				infoID = rs.getInt("infoID");
				title = rs.getString("title");
				content = rs.getString("content");

				// �Է��ϱ�׼���ı����н��ͣ���������
				content = makeDictionaryLink(content, webDesignDictionary);
				//content = "<br>&nbsp;&nbsp;" + content; // ���ݵ�һ�ο�2���ַ���ʼ.

				hasFound = true;

				author = rs.getString("author");
				publishedDate = rs.getTimestamp("publishedDate");
				expiredDate = rs.getTimestamp("expiredDate");
				keywords = rs.getString("keywords");
				derivation = rs.getString("derivation");
				sourceURI = rs.getString("sourceURI");
				whoCollectIt = rs.getString("whoCollectIt");
				latestUpdateTime = rs.getTimestamp("latestUpdateTime");
				insertedTime = rs.getTimestamp("insertedTime");
				viewedCount = rs.getInt("viewedCount");
				latestModifiedMemberID = rs.getString("latestModifiedMemberID");
				// hasVerify_str = rs.getInt("hasVerify");
				activeStatus = rs.getString("activeStatus");
				// securityLevel_str = rs.getInt("securityLevel");
				// isAuthorship_str = rs.getInt("isAuthorship");
			} else {
				System.out.println(" SQL ��ѯ,�Ҳ������� infoID=" + infoID + " SQL="
						+ SQL);
			}
		} catch (SQLException e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				System.out.println(e);
			}
		}

		// if has found.

		if (!hasFound) {
			System.out.println("�Ҳ�����Ϣ:" + ctv.getCatalogTreeTableName()
					+ "=    infoID=" + infoID + " catalogName="
					+ currentInfoCatalogTree.getName() + " ����HTML�ļ��޷�����");
			return;
		}
		boolean retainMark = false;

		// System.out.println("ģ���滻��ʼ...infoID="+infoID);

		// 0.�ж��Ƿ���ڶ����ҳ.��������з�ҳ�������ɶ��ҳ��
		String mutiPageMark = "\\[--��Ϣ�������ݷ�ҳ--\\]";
		String[] mutiPageContent = content.split(mutiPageMark);

		StringBuffer copyModel = new StringBuffer(model.toString());
		// �򵥻��ļ�����
		String basefullPathDir = currentInfoCatalogTree
				.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

		String basefileNameWithFullPath = basefullPathDir
				+ java.io.File.separator + +ctv.getCatalogTreeID() + "_"
				+ currentInfoCatalogTree.getID() + "_" + infoID; // + ".html";

		for (int i = 0; i < mutiPageContent.length; i++) {
			model = new StringBuffer(copyModel.toString()); // ��ÿ�����ݷ�ҳ������ͬ��ģ���滻.

			// 1.�滻����
			retainMark = false;
			String infoTitleStartMark = "<!--��Ϣ���⿪ʼ-->";
			String infoTitleEndMark = "<!--��Ϣ�������-->";
			model = findAndReplace(model, infoTitleStartMark, infoTitleEndMark,
					title, retainMark);

			// 2.�滻�˵�
			retainMark = false;
			String infoMenuStartMark = "<!--�˵���ʼ-->";
			String infoMenuEndMark = "<!--�˵�����-->";

			StringBuffer infoMenu = new StringBuffer();

			infoMenu.append(lineSeparator
					+ "<table width=\"100%\" height=\"100%\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
			infoMenu.append(lineSeparator
					+ "                                <tr>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"12%\" valign=\"bottom\" bgcolor=\"#FFFFFF\">&nbsp;</td>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"88%\" height=\"25\" valign=\"bottom\" bgcolor=\"#FFFFFF\"><div align=\"left\"></div></td>");
			infoMenu.append(lineSeparator
					+ "                                </tr>");

			String cellStr = null;
			Iterator<CatalogTree> itor = currentInfoCatalogTree.getSons()
					.iterator();
			CatalogTree tmp = null;
			while (itor.hasNext()) {
				tmp = (CatalogTree) itor.next();

				cellStr = lineSeparator + "<tr>";
				cellStr += lineSeparator
						+ "                           <td>&nbsp;</td>";
				cellStr += lineSeparator
						+ "                            <td height=\"24\"><div align=\"left\"><A";
				cellStr += lineSeparator + "                           href=\""
						+ ctv.getURI() + tmp.getVirtualFullWebDirPath()
						+ "/index.html\" class=\"menuListLink\">"
						+ tmp.getName() + "</A></div></td>";
				cellStr += lineSeparator + "                         </tr>";

				infoMenu.append(lineSeparator + cellStr);
			}

			int maxShowMenuItem = 35;
			if (currentInfoCatalogTree.getSons().size() < maxShowMenuItem) {

			}
			infoMenu.append(lineSeparator
					+ "                            </table>");
			model = findAndReplace(model, infoMenuStartMark, infoMenuEndMark,
					infoMenu.toString(), retainMark);

			// 3����Ϣ���򵼺���
			String infoPathStartMark = "<!--��Ϣ����Ŀ¼��ʼ-->";
			String infoPathEndMark = "<!--��Ϣ����Ŀ¼����-->";

			String catalogInfo = "";
			CatalogTree tmpCatalogTree = currentInfoCatalogTree;
			if (tmpCatalogTree != null) {
				String tmpURL = "";
				while (true) {
					tmpURL = "<a href=\"" + ctv.getURI()
							+ tmpCatalogTree.getVirtualFullWebDirPath()
							+ "/index.html\" class=\"infoPath\">"
							+ tmpCatalogTree.getName() + "</a>";
					catalogInfo = tmpURL + " ->" + catalogInfo;
					if (tmpCatalogTree.isRoot()) {
						break;
					}
					tmpCatalogTree = (CatalogTree) tmpCatalogTree.getFather();
				}
			}

			model = findAndReplace(model, infoPathStartMark, infoPathEndMark,
					catalogInfo, retainMark);
			
			
			

			// 4. ��Ϣ�ؼ�������

			String infoKeywordLinkValueStartMark = "<!--��Ϣ�ؼ��ֿ�ʼ-->";
			String infoKeywordLinkValueEndMark = "<!--��Ϣ�ؼ��ֽ���-->";

			String keywordLinkContent = "";
			if (keywords == null) {
				keywordLinkContent = "";
			} else {
				keywordLinkContent = "<a href=\""
						+ ctv.getURI()
						+ "/info/jsp/queryInfo.jsp?keyword=Info.title&value="
						+ keywords
						+ "&actionType=requery&pageNumber=1&pageSize=100\" class=\"infoKeywordValue\">"
						+ keywords + "</a>";
			}

			model = findAndReplace(model, infoKeywordLinkValueStartMark,
					infoKeywordLinkValueEndMark, keywordLinkContent, retainMark);
			
			
			// 4. ��Ϣ�ؼ��� �ڱ���meta keywords ����ʾ

			String infoKeywordValueStartMark = "<!--��Ϣmeta�ؼ��ֿ�ʼ-->";
			String infoKeywordValueEndMark = "<!--��Ϣmeta�ؼ��ֽ���-->";
			String 	keywordContent = (keywords==null) ? "" : keywords;
			model = findAndReplace(model, infoKeywordValueStartMark,
					infoKeywordValueEndMark, keywordContent, retainMark);

			// 5. ��Ϣ����

			String infoAuthorStartMark = "<!--��Ϣ���߿�ʼ-->";
			String infoAuthorEndMark = "<!--��Ϣ���߽���-->";

			model = findAndReplace(model, infoAuthorStartMark,
					infoAuthorEndMark, author, retainMark);

			// 5.��Ϣ����
			String infoDerivationStartMark = "<!--��Ϣ������ʼ-->";
			String infoDerivationEndMark = "<!--��Ϣ��������-->";

			String infoDerivation = "";

			if (sourceURI != null
					&& sourceURI.toLowerCase().indexOf("http://") >= 0) {
				infoDerivation = "<a href=\"" + sourceURI
						+ "\" class=infoDerivationLink target=_blank>"
						+ derivation + "</a>";
			} else {
				infoDerivation = "" + derivation;
			}

			// 6.��Ϣ����ʱ��

			String infoPublishedTimeStartMark = "<!--��Ϣ����ʱ�俪ʼ-->";
			String infoPublishedTimeEndMark = "<!--��Ϣ����ʱ�����-->";

			String publisheTimeStr = "";

			// ���û�����÷���ʱ��,���������ǰʱ��Ϊ��׼����Ϊ����ʱ�䡣 2009-10-12 �����޸�
			if (publishedDate == null) {
				publishedDate = new Timestamp(System.currentTimeMillis());
				// ��¼�������ݿ������¼

				PreparedStatement pstmt = null;
				String updateSQL = "update Info set publishedDate=?  where infoID="
						+ infoID;
				try {
					pstmt = ctv.getPool().prepareStatement(updateSQL);
					pstmt.setTimestamp(1, publishedDate);
					pstmt.executeUpdate();
					pstmt.close();
				} catch (SQLException e) {
					System.out.println(e);
				}
			}
			if (publishedDate != null) {
				// ȫ������Ӣ����ʾ��ʽ 2007-03-02 11:41
				Timestamp t = new Timestamp(publishedDate.getTime());
				publisheTimeStr = t.toString().substring(0, 16);
			}
			model = findAndReplace(model, infoPublishedTimeStartMark,
					infoPublishedTimeEndMark, publisheTimeStr, retainMark);
			// 2005��04��28��01ʱ27��

			// 7.��Ϣ���

			String infoTypeStartMark = "<!--��Ϣ���ʼ-->";
			String infoTypeEndMark = "<!--��Ϣ������-->";

			// ��ǰĿ¼������,��Ϣ���
			String htmlURLOfInfoType = "<a href=\"" + ctv.getURI()
					+ currentInfoCatalogTree.getVirtualFullWebDirPath()
					+ "/index.html\" class=\"infoPath\">"
					+ currentInfoCatalogTree.getName() + "</a>";

			model = findAndReplace(model, infoTypeStartMark, infoTypeEndMark,
					htmlURLOfInfoType, retainMark);

			// 8. <!--��Ϣ�������ݿ�ʼ-->

			String infoContentStartMark = "<!--��Ϣ�������ݿ�ʼ-->";
			String infoContentEndMark = "<!--��Ϣ�������ݽ���-->";

			// //8.1 ��������з�ҳ�������ɶ��ҳ��
			// String mutiPageMark="<!--��Ϣ�������ݷ�ҳ-->";
			// String[] mutiPageContent=content.split(mutiPageMark);

			int num = i + 1;
			int totalPage = mutiPageContent.length;
			int urlSize = 10;
			String baseURLWithFullPath = ctv.getURI()
					+ currentInfoCatalogTree.getVirtualFullWebDirPath() + "/"
					+ ctv.getCatalogTreeID() + "_"
					+ currentInfoCatalogTree.getID() + "_" + infoID;
			String urls = getPageFootURL(num, baseURLWithFullPath, urlSize,
					totalPage);

			// 8.1 �������ݷ�ҳ�ķ�ҳ����.
			// ��ҳ ��һҳ [1] [2] [3] [4] [5] [6] [7] [8] [9] [10] .. ��һҳ βҳ

			if (mutiPageContent.length == 1) {
				model = findAndReplace(model, infoContentStartMark,
						infoContentEndMark, mutiPageContent[0].toString(),
						retainMark);
			} else {
				model = findAndReplace(model, infoContentStartMark,
						infoContentEndMark, mutiPageContent[i].toString()
								+ "<br><div align=center>" + urls + "</div>",
						retainMark);

			}

			// 9.��ϢID�滻

			String infoIDStartMark = "<!--��ϢID��ʼ-->";
			String infoIDEndMark = "<!--��ϢID����-->";
			retainMark = false;
			model = findAndReplace(model, infoIDStartMark, infoIDEndMark,
					infoID + "", retainMark);

			// 10.��Ϣ���鿴����
			String infoViewedCountStartMark = "<!--��Ϣ�����˴ο�ʼ-->";
			String infoViewedCountEndMark = "<!--��Ϣ�����˴ν���-->";

			model = findAndReplace(model, infoViewedCountStartMark,
					infoViewedCountEndMark, viewedCount + "", retainMark);
			// 11.��Ϣ�������

			StringBuffer relativeLinks = new StringBuffer();

			String infoRelativeStartMark = "<!--��Ϣ������ӿ�ʼ-->";
			String infoRelativeEndMark = "<!--��Ϣ������ӽ���-->";

			relativeLinks
					.append(lineSeparator
							+ "        <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");

			relativeLinks.append(lineSeparator + " <tr>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"7%\"><div align=\"center\"><strong>��� </strong></div></td>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"60%\"><strong>���� </strong></td>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"33%\"><strong>�������� </strong></td>");
			relativeLinks.append(lineSeparator + "                </tr>");

			String keywordCondition = "";
			if (keywords != null) {
				// ���� keywords
				String delim = ", ��";//ȫ�ǵĶ��źͰ�ǵĶ��Ŷ����ԣ����пո�.
				java.util.StringTokenizer st = new StringTokenizer(keywords,
						delim);
				while (st.hasMoreTokens()) {
					keywordCondition += "(Info.keywords like '%"
							+ st.nextToken() + "%') or ";
				}
			}

			keywordCondition += " (1>2) "; // ������,������.
			keywordCondition = "  ( " + keywordCondition + " ) ";

			String keywordSQL = " select Info.infoID,Info.title,Info.publishedDate,InfoCatalogTreeHTMLWebPath.virtualWebPath from InfoCatalogTreeHTMLWebPath,Info where "
					+ keywordCondition
					+ " and InfoCatalogTreeHTMLWebPath.infoID=Info.infoID and Info.infoID!="
					+ infoID + " order by Info.publishedDate desc";

			Statement tmpStmt = null;
			ResultSet tmpRS = null;
			try {
				tmpStmt = ctv.getPool().createStatement();
				tmpRS = tmpStmt.executeQuery(keywordSQL);
				int tmpInfoID = 0, xuhao = 0;
				int maxShowCount = 10; // �����ʾ����Ŀ.
				Timestamp tmpPublishedDate = null;
				String tmpTitle = null, tmpVirtualWebPath = null;

				//ѡȡ�㷨...
				
				List<VirtualWebInfo> relativeWebInfoList=new LinkedList<VirtualWebInfo>();
				
				while (tmpRS.next()) {
					tmpInfoID = tmpRS.getInt("infoID");
					tmpTitle = tmpRS.getString("title");
					tmpVirtualWebPath = tmpRS.getString("virtualWebPath");
					tmpPublishedDate = tmpRS.getTimestamp("publishedDate");

					VirtualWebInfo webInfo=new VirtualWebInfo(tmpInfoID,tmpTitle,tmpVirtualWebPath,tmpPublishedDate);
					relativeWebInfoList.add(webInfo); //��С�������� ��һ������ɵ�.
				}
				
				List<VirtualWebInfo> selectedWebInfoList=getRelativeList(relativeWebInfoList,maxShowCount);
				Collections.sort(selectedWebInfoList);
				while(selectedWebInfoList.size()>0){
					VirtualWebInfo webInfo=selectedWebInfoList.remove(0);
				    xuhao++;

					// ����������һ����¼.
					relativeLinks.append(lineSeparator + " <tr>");
					relativeLinks.append(lineSeparator
							+ "             <td><div align=\"center\">" + xuhao
							+ "</div></td>");
					relativeLinks.append(lineSeparator
							+ "             <td><div align=\"left\"><a href=\""
							+ ctv.getURI() + webInfo.getVirtualURL()
							+ "\" class=\"relativeLink\">" + webInfo.getTitle()
							+ "</a></div></td>");
					relativeLinks.append(lineSeparator
							+ "             <td>"
							+ (webInfo.getInsertTime() == null ? "" : webInfo.getInsertTime()
									.toString().substring(0, 19)) + "</td>");
					relativeLinks.append(lineSeparator + "           </tr>");				
				}	
					
				

				if (relativeWebInfoList.size()>maxShowCount) {
					relativeLinks.append(lineSeparator + "<tr>");
					relativeLinks.append(lineSeparator
							+ "         <td>&nbsp;</td>");
					relativeLinks.append(lineSeparator
							+ "         <td>&nbsp;</td>");
					relativeLinks
							.append(lineSeparator
									+ "         <td><a href=\""
									+ ctv.getURI()
									+ "/info/jsp/queryInfo.jsp?keyword=Info.keywords&value="
									+ keywords
									+ "&actionType=requery&pageNumber=1\"\" class=\"relativeLink\">�鿴������ص���Ϣ...</a></td>");
					relativeLinks.append(lineSeparator + "       </tr>");
				}

				relativeLinks.append(lineSeparator + "          </table>");

				if (relativeLinks.indexOf(ctv.getURI()) < 0) {
					relativeLinks = new StringBuffer(); // һ����¼Ҳû��,��ȫ���.
				}
			} catch (SQLException e) {
				System.out.println(e + "\nSQL=" + keywordSQL);
			} finally {
				try {
					if (tmpRS != null) {
						tmpRS.close();
					}
					if (tmpStmt != null) {
						tmpStmt.close();
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}

			model = findAndReplace(model, infoRelativeStartMark,
					infoRelativeEndMark, relativeLinks.toString(), retainMark);

			// System.out.println("ģ���滻����...infoID="+infoID);

			// write to html file.
			String fullPathDir = currentInfoCatalogTree
					.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

			File f = new File(fullPathDir);
			if (!f.exists()) {
				f.mkdirs();
			}

			// �򵥻��ļ�����
			String fileNameWithFullPath = fullPathDir + java.io.File.separator
					+ +ctv.getCatalogTreeID() + "_"
					+ currentInfoCatalogTree.getID() + "_" + infoID + ".html";

			// Ҳ����д�ڸ�Ŀ¼��.
			boolean htmlCreateOK = false;

			String mutiPageFileNameWithFullPath = basefileNameWithFullPath
					+ "-" + (i + 1) + ".html";
			if (mutiPageContent.length == 1) {
				mutiPageFileNameWithFullPath = basefileNameWithFullPath
						+ ".html";
			}

			try {

				FileOutputStream fout = new FileOutputStream(
						mutiPageFileNameWithFullPath);
				byte[] buf = null;

				try {
					buf = model.toString().getBytes("GBK"); // ??"GBK"
				} catch (Exception ex) {
					System.out.println("��������Ϣ�����޷�����GBK������ת��." + ex);
				}

				if (buf != null) {
					// ������һҳ. fileNameWithFullPath
					if (i == 0 && mutiPageContent.length > 1) {
						// mutiPageFileNameWithFullPath = fileNameWithFullPath;
						FileOutputStream fout2 = new FileOutputStream(
								fileNameWithFullPath);
						fout2.write(buf);
						fout2.close();
					}

					fout.write(buf); // �����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ.
					fout.close();

					// �ж��ļ��Ƿ񴴽��ɹ�.
					File tmpF = new File(mutiPageFileNameWithFullPath);
					if (tmpF.length() == buf.length) {
						htmlCreateOK = true;
					} else if (tmpF.length() > 0 && tmpF.length() < buf.length) {
						System.out.println("�ļ�:" + fileNameWithFullPath
								+ "����ʧ��,�������ݶ�ʧ. ԭ���ļ���С" + buf.length
								+ "�ֽ�,�����ɺ�Ĵ�С��" + tmpF.length() + "�ֽ�.");
					} else if (tmpF.length() == 0) {
						System.out.println("�ļ�:" + mutiPageFileNameWithFullPath
								+ "����ʧ��,�ļ���С��0�ֽ�.");
					}
				} else {
					System.out
							.println("buf==null  ��Model д���ļ�ʱ�����˴���.fileNameWithFullPath="
									+ fileNameWithFullPath);
				}
			} catch (IOException e) {
				System.out.println(e);
			}

			// ��¼�����ݿ���ȥ,����ж�ҳ��ֻ��¼��һҳ��Ϣ.

			if (htmlCreateOK && i == 0) {
				// record to database;
				/*
				 * drop table InfoCatalogTreeHTMLWebPath; create table
				 * InfoCatalogTreeHTMLWebPath ( catalogTreeID int not null,
				 * #Ŀ¼��ID folderID int not null, #Ŀ¼ID infoID int not null,
				 * #��ϢID virtualWebPath varchar(255) not null, #��Ϣ��ŵ���վ·��
				 * ����Ŀ¼�ĸ�Ŀ¼��ʼ����. http://localhost/html /it-waibao/ primary
				 * key(catalogTreeID,folderID,infoID) );
				 */
				PreparedStatement pstmt = null, pstmt2 = null, pstmt3 = null;
				
				try {
					String sql = " delete from InfoCatalogTreeHTMLWebPath where catalogTreeID = ? and folderID= ? and infoID=?";
					String insertSQL = " insert into InfoCatalogTreeHTMLWebPath(catalogTreeID,folderID,infoID,virtualWebPath) values(?,?,?,?) ";
					// ��¼������Ϣ�Ѿ�����,���ķ���״̬����¼����ʱ��.
					String updateCatalogTreeSQL = "update "
							+ ctv.getCatalogTreeTableName()
							+ " set infoPublishStatus=1,infoPublishTime=now() where infoID="
							+ infoID + " and infoPublishStatus=0 ";

					pstmt = ctv.getPool().prepareStatement(sql);

					pstmt.setLong(1, ctv.getCatalogTreeID());
					pstmt.setLong(2, currentInfoCatalogTree.getID());
					pstmt.setLong(3, infoID);
					pstmt.executeUpdate();
					pstmt.close();

					pstmt2 = ctv.getPool().prepareStatement(insertSQL);
					pstmt2.setLong(1, ctv.getCatalogTreeID());
					pstmt2.setLong(2, currentInfoCatalogTree.getID());
					pstmt2.setLong(3, infoID);

					String tmpPath = fileNameWithFullPath.replace('\\', '/');

					// ???
					String virtualWebPath = tmpPath.substring(ctv
							.getCatalogRootInSystemDir().length());

					pstmt2.setString(4, virtualWebPath);
				    pstmt2.executeUpdate();
					pstmt2.close();

					pstmt3 = ctv.getPool().prepareStatement(
							updateCatalogTreeSQL);
					pstmt3.executeUpdate();
					pstmt3.close();
				} catch (SQLException e) {
					System.out.println(e);
				} finally {
					try {
						if (pstmt != null) {
							pstmt.close();
						}
						if (pstmt2 != null) {
							pstmt2.close();
						}
						if (pstmt3 != null) {
							pstmt3.close();
						}
					} catch (SQLException e) {
						System.out.println(e);
					}
				}
				// System.out.println("record to database..." +
				// fileNameWithFullPath);
			}
		}
		
		//generatePrcode
		
	}
	
	
	public List<VirtualWebInfo> getRelativeList(List<VirtualWebInfo> relativeWebInfoList, final int maxShowCount){
		List<VirtualWebInfo> webInfoList=new LinkedList<VirtualWebInfo>();
		Timestamp theDayBeforeYesterday=new Timestamp(System.currentTimeMillis()-1000*3600*24*3);
		//�����պã�ȫ��ѡ��,����Ҫ����
		if(relativeWebInfoList.size()<=maxShowCount) {
			webInfoList.addAll(relativeWebInfoList);
			return webInfoList;
		}

		for(int count=0;count<maxShowCount;count++){
			VirtualWebInfo webInfo=relativeWebInfoList.get(0);
			if(webInfo.getInsertTime().after(theDayBeforeYesterday)){
				webInfoList.add(webInfo); //100% ���3��֮�ڵ�����
				continue;
			}
			int whichOne=(int)(relativeWebInfoList.size()*Math.random());
			webInfoList.add(relativeWebInfoList.remove(whichOne)); //100% ���3��֮�ڵ�����
			continue;
		}
		return webInfoList;
	}

	
	
	
	/**
	 * ��һ����Ϣת��ΪHTML��ʽ.
	 * 
	 * @param t
	 *            CatalogTree
	 * @param Info
	 *            info 2009.7.18 �Ϻ� �������ٶȹ��ƻ�ܿ죬������ȷ��.
	 */
	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, Info info, StringBuffer model) {
		    if(info==null){
		    	System.err.println("Info==null in infoToHTMLForUserView(CatalogTree currentInfoCatalogTree, "
		    			+ "CatalogTreeView ctv, Info info, StringBuffer model)");
		    	return;
		    }
		    infoToHTMLForUserView(currentInfoCatalogTree,ctv, info.getInfoID(), model);
	}

	/**
	 * 
	 * @param num
	 *            Ҫ��ʾ�ĵ�ǰҳ
	 * @param baseHTMLURL
	 *            baseHTMLURL
	 * @param paramName
	 * @return
	 */
	// ��ҳ ��һҳ [1] [2] [3] [4] [5] [6] [7] [8] [9] [10] .. ��һҳ βҳ
	private static String getPageFootURL(int num, String baseHTMLURL,
			int urlSize, int totalPage) {
		int base = (int) ((num - 1) / urlSize);
		int first = urlSize * base;
		String info = "";
		if (num == 1) {
			info += "&nbsp;��ҳ&nbsp;&nbsp;<<��һҳ";
		}

		if (num > 1) {
			info += "<A HREF=\"" + baseHTMLURL + "-1.html"
					+ "\">&nbsp;��ҳ</A>&nbsp;&nbsp;";
			info += "<A HREF=\"" + baseHTMLURL + "-" + (num - 1) + ".html"
					+ "\">&nbsp;<<��һҳ</A>";
		}

		String url = "";
		// ��
		for (int k = 1; k <= urlSize; k++) {
			int tmp = first + k;
			if (tmp <= 0) {
				continue;
			}
			if (tmp > totalPage) {
				break;
			}
			if (num == k) {
				url = "&nbsp;[" + tmp + "]"; // ��ǰҳ�Ͳ�������.
			} else {
				url = "<A HREF=\"" + baseHTMLURL + "-" + tmp + ".html"
						+ "\">&nbsp;[" + tmp + "]</A>";
			}
			info += url;
		}

		if (num < totalPage) {
			info += "<A HREF=\"" + baseHTMLURL + "-" + (num + 1) + ".html"
					+ "\">&nbsp;��һҳ>></A>";
			info += "<A HREF=\"" + baseHTMLURL + "-" + totalPage + ".html"
					+ "\">&nbsp;&nbsp;&nbsp;���һҳ</A>";
		} else if (num == totalPage) {
			info += "&nbsp;��һҳ>>";
			info += "&nbsp;&nbsp;&nbsp;���һҳ";
		}
		return info;
	}

	/**
	 * 
	 * @param t
	 *            Ҫת��ΪHTML��Ŀ¼
	 * @param ctv
	 *            Ŀ¼����Ϣ��ͼ
	 * @param model
	 *            ģ��
	 * @param dirModel
	 *            Ŀ¼ģ��
	 * @return
	 */
	public String dirToHTMLForUserView(CatalogTree t, CatalogTreeView ctv,
			StringBuffer model, StringBuffer dirModel) {
		StringBuffer errorInfo = new StringBuffer();
		CatalogTree currentInfoCatalogTree = t;

		String SQL = " select 'xuhao',Info.infoID,title,"
				+ ctv.getCatalogTreeTableName()
				+ ".folderID,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".name,author,FileFormat.name,LanguageType.name,publishedDate,'�༭' from "
				+ ctv.getCatalogTreeTableName()
				+ ",Info,FileFormat,LanguageType,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ " where "
				+ ctv.getCatalogTreeTableName()
				+ ".catalogTreeID = "
				+ ctv.getCatalogTreeID()
				+ " and "
				+ ctv.getCatalogTreeTableName()
				+ ".infoID = Info.infoID and FileFormat.id = Info.format and LanguageType.id= Info.languageType and ("
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".id = Info.infoType and "
				+ ctv.getCatalogTreeDefinitionTableName() + ".catalogTreeID="
				+ ctv.getCatalogTreeID() + " ) and "
				+ ctv.getCatalogTreeTableName() + ".folderID = " + t.getID()
				+ " order by insertedTime desc ";

		String actionType = "requery";
		Statement stmt = null;

		String tableContent = null;
		String urls = null;
		int pageSize = 20;

		happy.web.ShowByPageBean searchInfo = new ShowByPageBean();

		if (actionType != null && actionType.equals("requery")) {
			try {
				stmt = ctv.getPool().createStatement(
						java.sql.ResultSet.TYPE_SCROLL_SENSITIVE,
						java.sql.ResultSet.CONCUR_READ_ONLY);
				ResultSet RS = stmt.executeQuery(SQL);
				searchInfo.init(RS);

				searchInfo.setPageSize(pageSize);
				searchInfo.setUrlSize(10); // ���������ʾ10������

			} catch (java.sql.SQLException ex) {
				System.out.println(ex + "\nSQL=" + SQL);
				errorInfo.append("\nex=" + ex + "SQL=" + SQL + "\n");
			}
		}

		int pageCount = searchInfo.getTotalPagesNum();
		if (pageCount <= 0) {
			pageCount = 1;
		}

		for (int i = 1; i <= pageCount; i++) {
			int currentShowingPage = i;
			Object[][] data = searchInfo.getPage(currentShowingPage);
			QueryByPageInfoBean info = searchInfo.getQueryByPageInfo();

			if ((data == null || data.length == 0) || info == null) // ����޷���ȡ����,�������ʾ���޷�����.
			{
				tableContent = "<br><br><div align=\"center\">;&nbsp;��ǰĿ¼����δ�����Ϣ.&nbsp;</div>";
				// ���Ͽ���.
				for (int k = 0; k < pageSize; k++) {
					tableContent += "\n\r <br>";
				}

			} else {
				String[] columnNames = { "���", "��ϢID", "����", "��Ϣ����ID", "��Ϣ����",
						"����", "��ʽ", "��������", "��������", "�༭" };
				info.setColumnNames(columnNames);
				happy.web.HtmlTable h = new happy.web.HtmlTable(data, info);
				HtmlTableCell xuhaoCell = null, infoIDCell = null, titleCell = null, infoCatalogTreeIDCell = null, editCell = null;
				String title = null, tmpStr = null, edit = null, xuhaoStr = null;
				Integer infoID = null;
				int limitBytes = 50;
				int xuhaoID = 0;

				Integer tmpInt = null;

				String sql = " select virtualWebPath from InfoCatalogTreeHTMLWebPath  where catalogTreeID=? and folderID=? and infoID = ?";

				PreparedStatement pstmt = null;
				ResultSet rs = null;
				String virtualWebPath = null;

				for (int row = 1; row < h.getRowCount(); row++) // ��
																// h.getRowCount()
																// ��,��һ���Ǳ�ͷ.
				{
					xuhaoCell = h.getTableCell(row, 0);
					infoIDCell = h.getTableCell(row, 1);
					titleCell = h.getTableCell(row, 2);
					infoCatalogTreeIDCell = h.getTableCell(row, 3);
					editCell = h.getTableCell(row, 9);

					xuhaoID = (currentShowingPage - 1)
							* searchInfo.getPageSize() + row;
					xuhaoStr = xuhaoID + "";

					if (xuhaoCell != null) {
						xuhaoCell.setContent(xuhaoStr);
					} else {
						System.out.println("xuhaoCell==null row=" + row
								+ " h.getRowCount()=" + h.getRowCount());
					}

					if (infoIDCell != null) {
						infoID = (Integer) infoIDCell.getContent();
					}

					if (infoCatalogTreeIDCell != null) {
						tmpInt = (Integer) infoCatalogTreeIDCell.getContent();
					}

					// ת����Ϣ����ΪHTML��ʽ���ļ�����ǰĿ¼��.
					// System.out.println("׼��ת��Info to HTML infoID="+infoID);
					infoToHTMLForUserView(currentInfoCatalogTree, ctv,
							infoID.intValue(), model);

					if (titleCell != null && infoID != null && tmpInt != null
							&& editCell != null) {
						title = (String) titleCell.getContent();
						if (title != null && title.length() >= 1
								&& title.getBytes().length > limitBytes) {
							title = truncate(title, limitBytes - 3) + "...";
						}

						try {
							pstmt = ctv.getPool().prepareStatement(sql);

							pstmt.setLong(1, ctv.getCatalogTreeID());
							pstmt.setLong(2, currentInfoCatalogTree.getID());
							pstmt.setLong(3, infoID.longValue());

							rs = pstmt.executeQuery();

							if (rs.next()) {
								virtualWebPath = rs.getString("virtualWebPath");
							}
							rs.close();
						} catch (SQLException e) {
							System.out.println(e);
							errorInfo.append("\n" + e + "\n");
						} finally {
							try {
								if (rs != null) {
									rs.close();
								}

								if (pstmt != null) {
									pstmt.close();
								}
							} catch (SQLException e) {
								System.out.println(e);
								errorInfo.append("\n" + e + "\n");
							}
						}

						tmpStr = "<a href=\"" + ctv.getURI() + virtualWebPath
								+ "\" class=\"infoListLink\">" + title + "</a>";
						titleCell.setContent(tmpStr);

						edit = "<a href=\""
								+ ctv.getURI()
								+ "/jsp/editInfo.jsp?infoID="
								+ infoID
								+ "&folderID="
								+ tmpInt.intValue()
								+ "\" target=\"_blank\" class=\"infoListLink\">�༭</a>";
						editCell.setContent(edit);
					}
				}

				h.setTableHeadAlign("left");
				h.setRowHeight(0, "30");
				h.setRowValign(0, "middle");
				// h.setBordercolordark("#ffffff");
				// h.setBordercolor("#888888");
				// h.setPixelsPerByteShow(2.6);
				h.setClassIDOfCSS("dirContentTable");
				h.hideColumn("��Ϣ����ID");
				h.hideColumn("��Ϣ����");
				h.hideColumn("����");
				h.hideColumn("��ʽ");
				h.hideColumn("��ϢID");
				h.hideColumn("��������");
				h.hideColumn("�༭");
				h.hideColumn("��Ϣ����"); // ��Ϊ����Ŀ¼�ṹ���Ѿ��ǳ�����ˣ�û�б�Ҫ��ʾ����������Ϣ.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("��������", "year-month%2-date%2");
				// h.setColumnDataFormat("���һ�α���ʱ��","year-month%2-date%2 hour%2:minute%2");//
				// 11:24:00.437
				// String eventsAndParams =
				// "onmouseover=\"over_change(this,'#C3E7FA');\" \" onmouseout=\"out_change(this,'');\"";
				// h.setBodyRowsEventsAndParams(eventsAndParams);
				h.setAlign("left");
				tableContent = h.toString();

				// ���Ͽ���.
				for (int k = 0; k < pageSize - h.getRowCount(); k++) {
					tableContent += "\n\r <br>";
				}

				// ��ʾҳ��
				// ��Ҫ��ǿ.
				urls = "";
				/*
				 * for (int k = 1; k < pageCount; k++) { urls += " <a href=\"" +
				 * ctv.getURI() +
				 * currentInfoCatalogTree.getVirtualFullWebDirPath() +"/" + k +
				 * ".html\">" + k + "</a>&nbsp;>&nbsp;"; }
				 */

				int num = currentShowingPage;
				int totalPage = searchInfo.getTotalPagesNum();
				int base = (int) ((num - 1) / searchInfo.getUrlSize());
				int first = searchInfo.getUrlSize() * base;

				urls = "<font size=\"-1\" color=\"#FF80FF\">��ǰҳ:&nbsp;��"
						+ currentShowingPage
						+ "ҳ</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				if (currentShowingPage > searchInfo.getUrlSize()) //
				{
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ "1.html\" class=\"infoListFootLink\">&nbsp;��һҳ</a>&nbsp;&nbsp;";
				}
				if (num > searchInfo.getUrlSize()) // ��ǰһҳ,��ӡǰһҳ��
				{
					int prePage = 0;
					if (num % searchInfo.getUrlSize() != 0) {
						prePage = num - num % searchInfo.getUrlSize();
					} else {
						prePage = num - searchInfo.getUrlSize(); // �պ��ڱ߽���,��ʱ��ǰһҳ����ǵ�ǰҳ��-ÿ��ҳ���С

					}
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ prePage
							+ ".html\" class=\"infoListFootLink\">&nbsp;ǰһҳ</a>";
				}
				// ��
				for (int k = 1; k <= searchInfo.getUrlSize(); k++) {
					int tmp = first + k;
					if (tmp <= 0) {
						continue;
					}
					if (tmp > totalPage) {
						break;
					}
					urls += "<a href=\"" + ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/" + tmp
							+ ".html\" class=\"infoListFootLink\">&nbsp;" + tmp
							+ "</a>";

				}

				int nextPage = -1;
				boolean hasNextPage = false;
				if (num % searchInfo.getUrlSize() != 0) {
					nextPage = (num / searchInfo.getUrlSize() + 1)
							* searchInfo.getUrlSize() + 1;
				} else {
					nextPage = num + 1;
				}
				if (nextPage <= totalPage && nextPage > 0) {
					hasNextPage = true;
				}
				if (hasNextPage) {
					urls += "<a href=\"" + ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/" + nextPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;"
							+ "&nbsp;��һҳ</a>";
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ totalPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;&nbsp;&nbsp;���һҳ</a>";
				}
			}

			// ���html �ַ�������.

			StringBuffer tmpDirModel = new StringBuffer(dirModel.toString());
			boolean retainMark = false;

			// 1.�滻����

			String infoTitleStartMark = "<!--��Ϣ���⿪ʼ-->";
			String infoTitleEndMark = "<!--��Ϣ�������-->";
			String dirInfoTitle = t.getFullCatalogPath();
			tmpDirModel = findAndReplace(tmpDirModel, infoTitleStartMark,
					infoTitleEndMark, dirInfoTitle, retainMark);

			// 2.��Ϣ����Ŀ¼
			String infoPathStartMark = "<!--��Ϣ����Ŀ¼��ʼ-->";
			String infoPathEndMark = "<!--��Ϣ����Ŀ¼����-->";

			String catalogInfo = "";
			CatalogTree tmpCatalogTree = t; // currentInfoCatalogTree;
			if (tmpCatalogTree != null) {
				String tmpURL = "";
				while (true) {
					tmpURL = "<a href=\"" + ctv.getURI()
							+ tmpCatalogTree.getVirtualFullWebDirPath()
							+ "/index.html\" class=\"infoPath\">"
							+ tmpCatalogTree.getName() + "</a>";
					catalogInfo = tmpURL + " ->" + catalogInfo;
					if (tmpCatalogTree.isRoot()) {
						break;
					}
					tmpCatalogTree = (CatalogTree) tmpCatalogTree.getFather();
				}
			}

			tmpDirModel = findAndReplace(tmpDirModel, infoPathStartMark,
					infoPathEndMark, catalogInfo, retainMark);

			// 9.<!--��ϢĿ¼ID��ʼ-->547<!--��ϢĿ¼ID����-->�滻

			String infoCatalogTreeIDStartMark = "<!--��ϢĿ¼ID��ʼ-->";
			String infoCatalogTreeIDEndMark = "<!--��ϢĿ¼ID����-->";
			retainMark = false;
			tmpDirModel = findAndReplace(tmpDirModel,
					infoCatalogTreeIDStartMark, infoCatalogTreeIDEndMark,
					currentInfoCatalogTree.getID() + "", retainMark);

			// 2.�滻�˵�
			retainMark = false;
			String infoMenuStartMark = "<!--�˵���ʼ-->";
			String infoMenuEndMark = "<!--�˵�����-->";

			StringBuffer infoMenu = new StringBuffer();

			infoMenu.append(lineSeparator
					+ "<table width=\"100%\" height=\"100%\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
			infoMenu.append(lineSeparator
					+ "                                <tr>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"12%\" valign=\"bottom\" bgcolor=\"#FFFFFF\">&nbsp;</td>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"88%\" height=\"25\" valign=\"bottom\" bgcolor=\"#FFFFFF\"><div align=\"left\"></div></td>");
			infoMenu.append(lineSeparator
					+ "                                </tr>");

			String cellStr = null;
			Iterator<CatalogTree> itor = currentInfoCatalogTree.getSons()
					.iterator();
			CatalogTree tmp = null;
			while (itor.hasNext()) {
				tmp = (CatalogTree) itor.next();

				cellStr = lineSeparator + "<tr>";
				cellStr += lineSeparator
						+ "                           <td>&nbsp;</td>";
				cellStr += lineSeparator
						+ "                            <td height=\"24\"><div align=\"left\"><A";
				cellStr += lineSeparator + "                           href=\""
						+ ctv.getURI() + tmp.getVirtualFullWebDirPath()
						+ "/index.html\" class=\"menuListLink\">"
						+ tmp.getName() + "</A></div></td>";
				cellStr += lineSeparator + "                         </tr>";

				infoMenu.append(lineSeparator + cellStr);
			}
			infoMenu.append(lineSeparator
					+ "                            </table>");
			tmpDirModel = findAndReplace(tmpDirModel, infoMenuStartMark,
					infoMenuEndMark, infoMenu.toString(), retainMark);

			//

			String infoContentStartMark = "<!--��Ϣ�������ݿ�ʼ-->";
			String infoContentEndMark = "<!--��Ϣ�������ݽ���-->";

			tmpDirModel = findAndReplace(tmpDirModel, infoContentStartMark,
					infoContentEndMark, tableContent, retainMark);

			// <!--��ҳ��ʾ���ӽ���-->

			String showByPageStartMark = "<!--��ҳ��ʾ���ӿ�ʼ-->";
			String showByPageEndMark = "<!--��ҳ��ʾ���ӽ���-->";

			tmpDirModel = findAndReplace(tmpDirModel, showByPageStartMark,
					showByPageEndMark, urls, retainMark);

			// write to html file.

			String fullPathDir = currentInfoCatalogTree
					.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

			File f = new File(fullPathDir);
			if (!f.exists()) {
				f.mkdirs();
			}

			String fileNameWithFullPath = fullPathDir + java.io.File.separator
					+ i + ".html";

			if (i == 1) {
				String indexFileName = fullPathDir + java.io.File.separator
						+ "/index.html";
				// ������ԸĽ����ܲ��ܲ���String, ֱ���ڶ�������Ϳ����滻.
				try {
					FileOutputStream fout = new FileOutputStream(indexFileName);
					fout.write(tmpDirModel.toString().getBytes("GBK")); // �����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ.
					fout.close();
				} catch (IOException e) {
					System.out.println(e);
					errorInfo.append("\n" + e + "\n");
				}
			}

			try {
				// System.out.println(fileNameWithFullPath);
				FileOutputStream fout = new FileOutputStream(
						fileNameWithFullPath);
				fout.write(tmpDirModel.toString().getBytes("GBK")); // �����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ.
				fout.close();
				System.out.println(fileNameWithFullPath + " �����ɹ�. ʱ��:"
						+ new java.sql.Timestamp(System.currentTimeMillis()));
			} catch (IOException e) {
				System.out.println(e);
				errorInfo.append("\n" + e + "\n");
			}
		} // end of for

		return errorInfo.toString();
	}

	/**
	 * 
	 * @param t
	 *            Ҫת��ΪHTML��Ŀ¼
	 * @param ctv
	 *            Ŀ¼����Ϣ��ͼ
	 * @param model
	 *            ģ��
	 * @param dirModel
	 *            Ŀ¼ģ��
	 * @return
	 */
	public String onlyDirToHTMLForUserView(CatalogTree t, CatalogTreeView ctv,
			StringBuffer model, StringBuffer dirModel) {
		StringBuffer errorInfo = new StringBuffer();
		CatalogTree currentInfoCatalogTree = t;

		String SQL = " select 'xuhao',Info.infoID,title,"
				+ ctv.getCatalogTreeTableName()
				+ ".folderID,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".name,author,FileFormat.name,LanguageType.name,publishedDate,'�༭' from "
				+ ctv.getCatalogTreeTableName()
				+ ",Info,FileFormat,LanguageType,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ " where "
				+ ctv.getCatalogTreeTableName()
				+ ".catalogTreeID = "
				+ ctv.getCatalogTreeID()
				+ " and "
				+ ctv.getCatalogTreeTableName()
				+ ".infoID = Info.infoID and FileFormat.id = Info.format and LanguageType.id= Info.languageType and ("
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".id = Info.infoType and "
				+ ctv.getCatalogTreeDefinitionTableName() + ".catalogTreeID="
				+ ctv.getCatalogTreeID() + " ) and "
				+ ctv.getCatalogTreeTableName() + ".folderID = " + t.getID()
				+ " and " + ctv.getCatalogTreeTableName()
				+ ".infoPublishStatus=1 " + // ��ʾ�Ѿ���������Ϣ
				" order by insertedTime desc ";

		String actionType = "requery";
		Statement stmt = null;

		String tableContent = null;
		String urls = null;
		int pageSize = 20;

		happy.web.ShowByPageBean searchInfo = new ShowByPageBean();

		if (actionType != null && actionType.equals("requery")) {
			try {
				stmt = ctv.getPool().createStatement(
						java.sql.ResultSet.TYPE_SCROLL_SENSITIVE,
						java.sql.ResultSet.CONCUR_READ_ONLY);
				ResultSet RS = stmt.executeQuery(SQL);
				searchInfo.init(RS);

				searchInfo.setPageSize(pageSize);
				searchInfo.setUrlSize(10); // ���������ʾ10������

			} catch (java.sql.SQLException ex) {
				System.out.println(ex + "\nSQL=" + SQL);
				errorInfo.append("\nex=" + ex + "SQL=" + SQL + "\n");
			}
		}

		int pageCount = searchInfo.getTotalPagesNum();
		if (pageCount <= 0) {
			pageCount = 1;
		}

		for (int i = 1; i <= pageCount; i++) {
			int currentShowingPage = i;
			Object[][] data = searchInfo.getPage(currentShowingPage);
			QueryByPageInfoBean info = searchInfo.getQueryByPageInfo();

			if ((data == null || data.length == 0) || info == null) // ����޷���ȡ����,�������ʾ���޷�����.
			{
				tableContent = "<br><br><div align=\"center\">;&nbsp;��ǰĿ¼����δ�����Ϣ.&nbsp;</div>";
				// ���Ͽ���.
				for (int k = 0; k < pageSize; k++) {
					tableContent += "\n\r <br>";
				}

			} else {
				String[] columnNames = { "���", "��ϢID", "����", "��Ϣ����ID", "��Ϣ����",
						"����", "��ʽ", "��������", "��������", "�༭" };
				info.setColumnNames(columnNames);
				happy.web.HtmlTable h = new happy.web.HtmlTable(data, info);
				HtmlTableCell xuhaoCell = null, infoIDCell = null, titleCell = null, infoCatalogTreeIDCell = null, editCell = null;
				String title = null, tmpStr = null, edit = null, xuhaoStr = null;
				Integer infoID = null;
				int limitBytes = 50;
				int xuhaoID = 0;

				Integer tmpInt = null;

				String sql = " select virtualWebPath from InfoCatalogTreeHTMLWebPath  where catalogTreeID=? and folderID=? and infoID = ?";

				PreparedStatement pstmt = null;
				ResultSet rs = null;
				String virtualWebPath = null;

				for (int row = 1; row < h.getRowCount(); row++) // ��
																// h.getRowCount()
																// ��,��һ���Ǳ�ͷ.
				{
					xuhaoCell = h.getTableCell(row, 0);
					infoIDCell = h.getTableCell(row, 1);
					titleCell = h.getTableCell(row, 2);
					infoCatalogTreeIDCell = h.getTableCell(row, 3);
					editCell = h.getTableCell(row, 9);

					xuhaoID = (currentShowingPage - 1)
							* searchInfo.getPageSize() + row;
					xuhaoStr = xuhaoID + "";

					if (xuhaoCell != null) {
						xuhaoCell.setContent(xuhaoStr);
					} else {
						System.out.println("xuhaoCell==null row=" + row
								+ " h.getRowCount()=" + h.getRowCount());
					}

					if (infoIDCell != null) {
						infoID = (Integer) infoIDCell.getContent();
					}

					if (infoCatalogTreeIDCell != null) {
						tmpInt = (Integer) infoCatalogTreeIDCell.getContent();
					}

					if (titleCell != null && infoID != null && tmpInt != null
							&& editCell != null) {
						title = (String) titleCell.getContent();
						if (title != null && title.length() >= 1
								&& title.getBytes().length > limitBytes) {
							title = truncate(title, limitBytes - 3) + "...";
						}

						try {
							pstmt = ctv.getPool().prepareStatement(sql);

							pstmt.setLong(1, ctv.getCatalogTreeID());
							pstmt.setLong(2, currentInfoCatalogTree.getID());
							pstmt.setLong(3, infoID.longValue());

							rs = pstmt.executeQuery();

							if (rs.next()) {
								virtualWebPath = rs.getString("virtualWebPath");
							}
							rs.close();
						} catch (SQLException e) {
							System.out.println(e);
							errorInfo.append("\n" + e + "\n");
						} finally {
							try {
								if (rs != null) {
									rs.close();
								}

								if (pstmt != null) {
									pstmt.close();
								}
							} catch (SQLException e) {
								System.out.println(e);
								errorInfo.append("\n" + e + "\n");
							}
						}

						tmpStr = "<a href=\"" + ctv.getURI() + virtualWebPath
								+ "\" class=\"infoListLink\">" + title + "</a>";
						titleCell.setContent(tmpStr);

						edit = "<a href=\""
								+ ctv.getURI()
								+ "/jsp/editInfo.jsp?infoID="
								+ infoID
								+ "&folderID="
								+ tmpInt.intValue()
								+ "\" target=\"_blank\" class=\"infoListLink\">�༭</a>";
						editCell.setContent(edit);
					}
				}

				h.setTableHeadAlign("left");
				h.setRowHeight(0, "30");
				h.setRowValign(0, "middle");
				// h.setBordercolordark("#ffffff");
				// h.setBordercolor("#888888");
				// h.setPixelsPerByteShow(2.6);
				h.setClassIDOfCSS("dirContentTable");
				h.hideColumn("��Ϣ����ID");
				h.hideColumn("��Ϣ����");
				h.hideColumn("����");
				h.hideColumn("��ʽ");
				h.hideColumn("��ϢID");
				h.hideColumn("��������");
				h.hideColumn("�༭");
				h.hideColumn("��Ϣ����"); // ��Ϊ����Ŀ¼�ṹ���Ѿ��ǳ�����ˣ�û�б�Ҫ��ʾ����������Ϣ.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("��������", "year-month%2-date%2");
				// h.setColumnDataFormat("���һ�α���ʱ��","year-month%2-date%2 hour%2:minute%2");//
				// 11:24:00.437
				// String eventsAndParams =
				// "onmouseover=\"over_change(this,'#C3E7FA');\" \" onmouseout=\"out_change(this,'');\"";
				// h.setBodyRowsEventsAndParams(eventsAndParams);
				h.setAlign("left");
				tableContent = h.toString();

				// ���Ͽ���.
				for (int k = 0; k < pageSize - h.getRowCount(); k++) {
					tableContent += "\n\r <br>";
				}

				// ��ʾҳ��

				// ��Ҫ��ǿ.
				urls = "";

				int num = currentShowingPage;
				int totalPage = searchInfo.getTotalPagesNum();
				int base = (int) ((num - 1) / searchInfo.getUrlSize());
				int first = searchInfo.getUrlSize() * base;

				urls = "<font size=\"-1\">��ǰҳ:&nbsp;��" + currentShowingPage
						+ "ҳ</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				if (currentShowingPage > searchInfo.getUrlSize()) //
				{
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ "1.html\" class=\"infoListFootLink\">&nbsp;��һҳ</a>&nbsp;&nbsp;";
				}
				
				if (num > searchInfo.getUrlSize()) // ��ǰһҳ,��ӡǰһҳ��
				{
					int prePage = 0;
					if (num % searchInfo.getUrlSize() != 0) {
						prePage = num - num % searchInfo.getUrlSize();
					} else {
						prePage = num - searchInfo.getUrlSize(); // �պ��ڱ߽���,��ʱ��ǰһҳ����ǵ�ǰҳ��-ÿ��ҳ���С

					}
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ prePage
							+ ".html\" class=\"infoListFootLink\">&nbsp;ǰһҳ</a>";
				}
				// ��
				for (int k = 1; k <= searchInfo.getUrlSize(); k++) {
					int tmp = first + k;
					if (tmp <= 0) {
						continue;
					}
					if (tmp > totalPage) {
						break;
					}
					urls += "<a href=\"" + ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/" + tmp
							+ ".html\" class=\"infoListFootLink\">&nbsp;" + tmp
							+ "</a>";

				}

				int nextPage = -1;
				boolean hasNextPage = false;
				if (num % searchInfo.getUrlSize() != 0) {
					nextPage = (num / searchInfo.getUrlSize() + 1)
							* searchInfo.getUrlSize() + 1;
				} else {
					nextPage = num + 1;
				}
				if (nextPage <= totalPage && nextPage > 0) {
					hasNextPage = true;
				}
				if (hasNextPage) {
					urls += "<a href=\"" + ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/" + nextPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;"
							+ "&nbsp;��һҳ</a>";
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ totalPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;&nbsp;&nbsp;���һҳ</a>";
				}
			}

			// ���html �ַ�������.

			StringBuffer tmpDirModel = new StringBuffer(dirModel.toString());
			boolean retainMark = false;

			// 1.�滻����

			String infoTitleStartMark = "<!--��Ϣ���⿪ʼ-->";
			String infoTitleEndMark = "<!--��Ϣ�������-->";
			String dirInfoTitle = t.getFullCatalogPath();
			tmpDirModel = findAndReplace(tmpDirModel, infoTitleStartMark,
					infoTitleEndMark, dirInfoTitle, retainMark);

			// 2.��Ϣ����Ŀ¼
			String infoPathStartMark = "<!--��Ϣ����Ŀ¼��ʼ-->";
			String infoPathEndMark = "<!--��Ϣ����Ŀ¼����-->";

			String catalogInfo = "";
			CatalogTree tmpCatalogTree = t; // currentInfoCatalogTree;
			if (tmpCatalogTree != null) {
				String tmpURL = "";
				while (true) {
					tmpURL = "<a href=\"" + ctv.getURI()
							+ tmpCatalogTree.getVirtualFullWebDirPath()
							+ "/index.html\" class=\"infoPath\">"
							+ tmpCatalogTree.getName() + "</a>";
					catalogInfo = tmpURL + " ->" + catalogInfo;
					if (tmpCatalogTree.isRoot()) {
						break;
					}
					tmpCatalogTree = (CatalogTree) tmpCatalogTree.getFather();
				}
			}

			tmpDirModel = findAndReplace(tmpDirModel, infoPathStartMark,
					infoPathEndMark, catalogInfo, retainMark);

			// 9.<!--��ϢĿ¼ID��ʼ-->547<!--��ϢĿ¼ID����-->�滻

			String infoCatalogTreeIDStartMark = "<!--��ϢĿ¼ID��ʼ-->";
			String infoCatalogTreeIDEndMark = "<!--��ϢĿ¼ID����-->";
			retainMark = false;
			tmpDirModel = findAndReplace(tmpDirModel,
					infoCatalogTreeIDStartMark, infoCatalogTreeIDEndMark,
					currentInfoCatalogTree.getID() + "", retainMark);

			// 2.�滻�˵�
			retainMark = false;
			String infoMenuStartMark = "<!--�˵���ʼ-->";
			String infoMenuEndMark = "<!--�˵�����-->";

			StringBuffer infoMenu = new StringBuffer();

			infoMenu.append(lineSeparator
					+ "<table width=\"100%\" height=\"100%\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
			infoMenu.append(lineSeparator
					+ "                                <tr>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"12%\" valign=\"bottom\" bgcolor=\"#FFFFFF\">&nbsp;</td>");
			infoMenu.append(lineSeparator
					+ "                                  <td width=\"88%\" height=\"25\" valign=\"bottom\" bgcolor=\"#FFFFFF\"><div align=\"left\"></div></td>");
			infoMenu.append(lineSeparator
					+ "                                </tr>");

			String cellStr = null;
			Iterator<CatalogTree> itor = currentInfoCatalogTree.getSons().iterator();
			CatalogTree tmp = null;
			while (itor.hasNext()) {
				tmp = (CatalogTree) itor.next();

				cellStr = lineSeparator + "<tr>";
				cellStr += lineSeparator
						+ "                           <td>&nbsp;</td>";
				cellStr += lineSeparator
						+ "                            <td height=\"24\"><div align=\"left\"><A";
				cellStr += lineSeparator + "                           href=\""
						+ ctv.getURI() + tmp.getVirtualFullWebDirPath()
						+ "/index.html\" class=\"menuListLink\">"
						+ tmp.getName() + "</A></div></td>";
				cellStr += lineSeparator + "                         </tr>";

				infoMenu.append(lineSeparator + cellStr);
			}
			infoMenu.append(lineSeparator
					+ "                            </table>");
			tmpDirModel = findAndReplace(tmpDirModel, infoMenuStartMark,
					infoMenuEndMark, infoMenu.toString(), retainMark);

			//

			String infoContentStartMark = "<!--��Ϣ�������ݿ�ʼ-->";
			String infoContentEndMark = "<!--��Ϣ�������ݽ���-->";

			tmpDirModel = findAndReplace(tmpDirModel, infoContentStartMark,
					infoContentEndMark, tableContent, retainMark);

			// <!--��ҳ��ʾ���ӽ���-->

			String showByPageStartMark = "<!--��ҳ��ʾ���ӿ�ʼ-->";
			String showByPageEndMark = "<!--��ҳ��ʾ���ӽ���-->";

			tmpDirModel = findAndReplace(tmpDirModel, showByPageStartMark,
					showByPageEndMark, urls, retainMark);

			// write to html file.

			String fullPathDir = currentInfoCatalogTree
					.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

			File f = new File(fullPathDir);
			if (!f.exists()) {
				f.mkdirs();
			}

			String fileNameWithFullPath = fullPathDir + java.io.File.separator
					+ i + ".html";

			if (i == 1) {
				String indexFileName = fullPathDir + java.io.File.separator
						+ "/index.html";
				// ������ԸĽ����ܲ��ܲ���String, ֱ���ڶ�������Ϳ����滻.
				try {
					FileOutputStream fout = new FileOutputStream(indexFileName);
					fout.write(tmpDirModel.toString().getBytes("GBK")); // �����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ.
					fout.close();
				} catch (IOException e) {
					System.out.println(e);
					errorInfo.append("\n" + e + "\n");
				}
			}

			try {
				// System.out.println(fileNameWithFullPath);
				FileOutputStream fout = new FileOutputStream(
						fileNameWithFullPath);
				fout.write(tmpDirModel.toString().getBytes("GBK")); // �����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ.
				fout.close();
			} catch (IOException e) {
				System.out.println(e);
				errorInfo.append("\n" + e + "\n");
			}
		} // end of for

		return errorInfo.toString();
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

		if (str.length() >= pos) {
			tmpStr = str.substring(0, pos);
		} else {
			// System.out.println("String truncate(String str, int limitBytes)  error");
			return str;
		}

		while (tmpStr.getBytes().length + 1 < limitBytes) {
			pos++;
			tmpStr = str.substring(0, pos);
		}
		return str.substring(0, pos);
	}

	/**
	 * �����ַ�����������Ҫ����״̬
	 * 
	 * @param t
	 *            CatalogTree
	 * @param ctv
	 *            CatalogTreeView
	 */
	public static String allDirToHTMLForUserView(CatalogTree ct,
			CatalogTreeView ctv, StringBuffer model, StringBuffer dirModel) {
		StringBuffer errorInfo = new StringBuffer();
		HealthHTMLMaker ihm = new HealthHTMLMaker();
		String errorReport = ihm.dirToHTMLForUserView(ct, ctv, model, dirModel);
		errorInfo.append(errorReport);

		if (ct.getSons().size() <= 0) {
			return errorInfo.toString();
		}

		Iterator<CatalogTree> itor = ct.getSons().iterator();
		while (itor.hasNext()) {
			CatalogTree tmp = (CatalogTree) itor.next();
			errorInfo
					.append(allDirToHTMLForUserView(tmp, ctv, model, dirModel));
		}

		return errorInfo.toString();

	}

	/**
	 * ��ȡָ��Ŀ¼�µ���ϢID,ֻ��ȡһ��.
	 * 
	 * @param ctv
	 * @param ct
	 * @return
	 */
	private static int[] getInfoIDFromCatalog(CatalogTreeView ctv,
			CatalogTree ct) {
		int[] infoIDs = null;
		int infoID = 0;
		Vector<Integer> tmpV = new Vector<Integer>();

		String sql = " select infoID from " + ctv.getCatalogTreeTableName()
				+ " where catalogTreeID=" + ctv.getCatalogTreeID()
				+ " and folderID=" + ct.getID() + " order by infoID desc";
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				infoID = rs.getInt("infoID");
				tmpV.add(new Integer(infoID));
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infoIDs = new int[tmpV.size()];
			Integer tmpInt = null;
			for (int i = 0; i < tmpV.size(); i++) {
				tmpInt = (Integer) tmpV.get(i);
				infoIDs[i] = tmpInt.intValue();
			}

		}

		return infoIDs;
	}

	/***
	 * ��Ŀ¼�л�ȡ��������Ϣ
	 * 
	 * @param ctv
	 * @param ct
	 * @return
	 */
	private static Info[] getFullInfoFromCatalog(CatalogTreeView ctv,
			CatalogTree ct) {
		// long startTime =System.currentTimeMillis();
		Info[] infos = null;
		Vector<Info> tmpV = new Vector<Info>();

		// infoPublishStatus=��ʾ�������� infoPublishStatus=1��ʾ�Ѿ�����
		// infoPublishStatus=2��ʾ������˼��δ���塣
		String sql = " select Info.infoID,title,infoType,content,author,format,languageType,publishedDate,expiredDate,keywords,"
				+ "derivation,sourceURI,whoCollectIt,latestUpdateTime,insertedTime,viewedCount,latestModifiedMemberID,hasVerify,activeStatus,securityLevel,"
				+ "isAuthorship,elite from Info,"
				+ ctv.getCatalogTreeTableName()
				+ " where Info.infoID="
				+ ctv.getCatalogTreeTableName()
				+ ".infoID and "
				+ ctv.getCatalogTreeTableName()
				+ ".folderID="
				+ ct.getID()
				+ "  and  "
				+ ctv.getCatalogTreeTableName()
				+ ".infoPublishStatus=1 order by Info.insertedTime desc; ";
		try {
			Statement stmt = ctv.getPool().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("title"));
				info.setInfoType(rs.getInt("infoType"));
				info.setContent(rs.getString("content"));
				info.setAuthor(rs.getString("author"));
				info.setFormat(rs.getInt("format"));
				info.setLanguageType(rs.getInt("languageType"));
				info.setPublishedDate(rs.getTimestamp("publishedDate"));
				info.setExpiredDate(rs.getTimestamp("expiredDate"));
				info.setKeywords(rs.getString("keywords"));
				info.setDerivation(rs.getString("derivation"));
				info.setSourceURI(rs.getString("sourceURI"));
				info.setWhoCollectIt(rs.getInt("whoCollectIt") + "");
				info.setLatestUpdateTime(rs.getTimestamp("latestUpdateTime"));
				// info.setInsertedTime(rs.getTimestamp("insertedTime"));
				info.setViewedCount(rs.getInt("viewedCount"));
				// info.setLatestModifiedMemeberID(rs.getString("latestModifiedMemberID"));
				info.setHasVerify(rs.getInt("hasVerify"));
				// info.setActiveStatus(rs.getString("activeStatus"));

				info.setSecurityLevel(rs.getInt("securityLevel"));
				info.setIsAuthorship(rs.getInt("isAuthorship"));
				info.setElite(rs.getInt("elite"));
				tmpV.add(info);
			}
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException e) {
			System.out.println(e);
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		// long endTime = System.currentTimeMillis();
		// float timeCostInSecond=(endTime-startTime)/1000;
		// System.out.println("�����ݿ��ȡ"+infos.length+"��Ϣ������¼���ķ�"+timeCostInSecond+"�롣ƽ��ÿ���ȡ��"+((infos.length*1.0)/timeCostInSecond)+"��¼.");
		return infos;
	}

	/**
	 * ����վ��ӵ���Ϣ�л�ȡ���ܻ�ӭ����Ϣ---������������Ϣ
	 * 
	 * @param ctv
	 * @param returnCount
	 * @return
	 */
	public static Info[] getFavoriteInfoFromWebSite(CatalogTreeView ctv,
			int returnCount) {
		if (returnCount <= 0) {
			return null;
		}
		Info[] infos = null;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID  order by viewedCount desc limit "
				+ returnCount + ";";

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setVirtualWebPath(rs
						.getString("InfoCatalogTreeHTMLWebPath.virtualWebPath"));
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("Info.title"));
				info.setPublishedDate(rs.getTimestamp("publishedDate"));
				tmpV.add(info);
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		return infos;
	}

	/**
	 * ����վ��ӵ���Ϣ�л�ȡ������ӵ���Ϣ,������ӵ�ʱ�䰴���--����������
	 * 
	 * @param ctv
	 *            ��Ϣ�����٣�ID�����⣬Html���ӵ�ַ
	 * @param returnCount
	 * @return
	 */
	public static Info[] getLatestInfoFromWebSite(CatalogTreeView ctv,
			int returnCount) {
		if (returnCount <= 0) {
			return null;
		}
		Info[] infos = null;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID  order by Info.publishedDate desc limit "
				+ returnCount + ";";

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setVirtualWebPath(rs
						.getString("InfoCatalogTreeHTMLWebPath.virtualWebPath"));
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("Info.title"));
				info.setPublishedDate(rs.getTimestamp("publishedDate"));
				tmpV.add(info);
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		return infos;
	}

	/**
	 * �����ݿ��л�ȡδ�������Ϣ,������ӵ�ʱ�䰴���--����������
	 * 
	 * @param ctv
	 *            ��Ϣ�����٣�ID�����⣬Html���ӵ�ַ
	 * @param returnCount
	 * @return
	 */
	public static Info[] getUnClassifyInfo(CatalogTreeView ctv, int returnCount) {
		if (returnCount <= 0) {
			return null;
		}
		Info[] infos = null;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select Info.infoID,Info.title,Info.publishedDate from Info where infoID>1000000  order by Info.publishedDate desc limit "
				+ returnCount + ";";

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("Info.title"));
				info.setPublishedDate(rs.getTimestamp("publishedDate"));
				tmpV.add(info);
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		return infos;
	}

	/**
	 * ����վ��ӵ���Ϣ�л�ȡ������ӵ���Ϣ
	 * 
	 * @param ctv
	 * @param returnCount
	 * @return
	 */
	public static Info[] getLatestInfoFromWebSiteByKeyword(String keyword,
			CatalogTreeView ctv, int returnCount) {
		if (returnCount <= 0) {
			return null;
		}
		Info[] infos = null;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID and Info.title like '%"
				+ keyword
				+ "%'  order by InfoID desc limit "
				+ returnCount
				+ ";";

		System.out.println("keyword=" + keyword);
		// latestInfo ������ؼ���.
		if ("latestInfo".equals(keyword)) {
			sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
					+ ctv.getCatalogTreeID()
					+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID  order by Info.publishedDate desc limit "
					+ returnCount + ";";
		}
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setVirtualWebPath(rs
						.getString("InfoCatalogTreeHTMLWebPath.virtualWebPath"));
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("Info.title"));
				tmpV.add(info);
				if ("latestInfo".equals(keyword)) {
					System.out.println(info.getInfoID() + "<------>"
							+ info.getTitle() + "  ");
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		return infos;
	}

	/**
	 * ��ȡָ��Ŀ¼�µ���ϢID,ֻ��ȡһ��.
	 * 
	 * @param ctv
	 * @param ct
	 * @return 2008-04-26
	 */
	public static Info[] getInfoFromCatalog(CatalogTreeView ctv, CatalogTree ct) {
		Info[] infos = null;

		int[] infoIDs = getInfoIDFromCatalog(ctv, ct);

		if (infoIDs == null) {
			return null;
		}
		Vector<Info> tmpV = new Vector<Info>();
		// select virtualWebPath from InfoCatalogTreeHTMLWebPath where
		// catalogTreeID=? and infoID = ?
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID  and (";
		for (int i = 0; i < infoIDs.length; i++) {
			if (i == infoIDs.length - 1) {
				sql += " Info.infoID=" + infoIDs[i]
						+ ") order by Info.insertedTime desc; ";
			} else {
				sql += " Info.infoID=" + infoIDs[i] + " or ";
			}
		}

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Info info = new Info();
				info.setVirtualWebPath(rs
						.getString("InfoCatalogTreeHTMLWebPath.virtualWebPath"));
				info.setInfoID(rs.getInt("Info.infoID"));
				info.setTitle(rs.getString("Info.title"));
				info.setPublishedDate(rs.getTimestamp("publishedDate"));
				tmpV.add(info);
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex);
			}
		}

		if (tmpV.size() > 0) {
			infos = new Info[tmpV.size()];
			for (int i = 0; i < tmpV.size(); i++) {
				infos[i] = (Info) tmpV.get(i);
			}
		}
		return infos;
	}

	/**
	 * ���Ŀ¼����Ϣ�ļ��Ƿ�����
	 * 
	 * @return
	 */
	public static java.io.BufferedOutputStream repairInfo(CatalogTree ct,
			CatalogTreeView ctv, StringBuffer model, StringBuffer dirModel,
			boolean checkInfo, boolean checkDir) {
		HealthHTMLMaker ihm = new HealthHTMLMaker();
		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(ct);

		while (stack.size() > 0) {
			CatalogTree tmp = (CatalogTree) stack.pop();
			int[] infoIDs = null;
			if (checkInfo) {
				// 1.��ȡ��ǰĿ¼�µ�����infoID
				infoIDs = getInfoIDFromCatalog(ctv, tmp);
				if (infoIDs != null) {
					for (int i = 0; i < infoIDs.length; i++) {
						String fullPathDir = tmp.getSystemFullDirPath(ctv
								.getCatalogRootInSystemDir());
						String fileNameWithFullPath = fullPathDir
								+ java.io.File.separator
								+ +ctv.getCatalogTreeID() + "_" + tmp.getID()
								+ "_" + infoIDs[i] + ".html";

						// ����Ƿ���ڲ�����,������˾����½���.
						if (!checkHTMLFileStatus(fileNameWithFullPath)) // ?
						{
							System.out.println(fileNameWithFullPath
									+ " �ļ�����,���ϴ���.");
							ihm.infoToHTMLForUserView(tmp, ctv, infoIDs[i],
									model);
							if (!checkHTMLFileStatus(fileNameWithFullPath)) {
								System.out.println(fileNameWithFullPath
										+ " ����ʧ��.");
							}
						}
					}
				}
			}

			if (checkDir) {
				// 2.����Ŀ¼�б�
				// String errorReport = ihm.onlyDirToHTMLForUserView(tmp, ctv,
				// model,
				// dirModel);
				// errorInfo.append(errorReport);
			}

			Iterator<CatalogTree> itor = tmp.getSons().iterator();
			while (itor.hasNext()) {
				stack.push(itor.next());
			}

			System.out.println("Ŀ¼ID:" + tmp.getID() + " Ŀ¼����·��:"
					+ tmp.getFullCatalogPath() + " Ŀ¼·��:" + ctv.getURI()
					+ tmp.getVirtualFullWebDirPath() + "  �Ѿ��������. ����"
					+ ((infoIDs != null) ? infoIDs.length : 0) + "����Ϣ");

		}
		return null;
	}

	public void safePublishNewInfoToHTMLForUserView(CatalogTree ct,
			CatalogTreeView ctv, StringBuffer model, StringBuffer dirModel,
			boolean updateInfo, boolean updateDir, int updateCount) {
		String querySQL = " select catalogTreeID,folderID,infoID,infoPublishStatus,infoPublishTime,infoInsertTime from "
				+ ctv.getCatalogTreeTableName()
				+ " where infoPublishStatus=0 order by infoInsertTime";

		// String querySQL
		// =" select catalogTreeID,folderID,infoID,infoPublishStatus,infoPublishTime,infoInsertTime from "+ctv.getCatalogTreeTableName()+" where infoPublishStatus=0 order by infoInsertTime";
		Statement stmt = null;
		Hashtable<Integer, Integer> tmp = new Hashtable<Integer, Integer>();
		Vector<Integer> tmpV = new Vector<Integer>();

		try {
			stmt = ctv.getPool().createStatement();
			ResultSet rs = stmt.executeQuery(querySQL);
			while (rs != null && rs.next()) {
				int folderID = rs.getInt("folderID");
				int infoID = rs.getInt("infoID");
				tmp.put(new Integer(infoID), new Integer(folderID));
				tmpV.add(new Integer(infoID));
			}

			if (rs != null) {
				rs.close();
			}

			if (stmt != null) {
				stmt.close();
			}

			int howMany = 0; // ����Ҫ���µ�����
			if (tmpV.size() < updateCount) {
				howMany = tmpV.size();
			} else {
				howMany = updateCount;
			}

			for (int i = 0; i < howMany; i++) {
				Integer whichInfoID = tmpV.remove((int) (Math.random() * tmpV
						.size()));
				Integer whichFolderID = tmp.get(whichInfoID);
				CatalogTree currentInfoCatalogTree = ct.findByID(whichFolderID
						.intValue());

				// ������Ϣ
				if (updateInfo) {
					infoToHTMLForUserView(currentInfoCatalogTree, ctv,
							whichInfoID.intValue(), model);
					System.out.println("��ϢID:" + whichInfoID + "�Ѿ��ɹ�����. Ŀ¼:"
							+ currentInfoCatalogTree.getFullCatalogPath());
				}
				// ����Ŀ¼.
				if (updateDir) {
					onlyDirToHTMLForUserView(currentInfoCatalogTree, ctv,
							model, dirModel);
				}
			}

			if (howMany > 0) {
				System.out.println("�Ѿ��ɹ�����" + howMany + "������Ϣ.");
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	/**
	 * �����ַ�����������Ҫ����״̬
	 * 
	 * @param ct
	 *            CatalogTree
	 * @param ctv
	 *            CatalogTreeView
	 * @param model
	 *            ��Ϣģ��HTML
	 * @param dirModel
	 *            ��ϢĿ¼ģ��HTML
	 * @param updateInfo
	 *            �Ƿ������Ϣ
	 * @param updateDir
	 *            �Ƿ����Ŀ¼
	 * @return
	 */
	public String safeUpdateTotalInfoToHTMLForUserView(CatalogTree ct,
			CatalogTreeView ctv, StringBuffer model, StringBuffer dirModel,
			boolean updateInfo, boolean updateDir) {
		long totalStartTime = System.currentTimeMillis();
		long totalCount = 0;
		StringBuffer errorInfo = new StringBuffer();
		HealthHTMLMaker ihm = new HealthHTMLMaker();
		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(ct);
		int dirSize = 0;
		while (stack.size() > 0) {
			long startTime = System.currentTimeMillis();
			CatalogTree currentCatalogTree = (CatalogTree) stack.pop();
			dirSize++;
			int infoCount = 0;
			if (updateInfo) {
				Info[] infos = getFullInfoFromCatalog(ctv, currentCatalogTree);
				if (infos != null) {
					infoCount = infos.length;
					totalCount += infoCount;
					for (int i = 0; i < infos.length; i++) {
						infoToHTMLForUserView(currentCatalogTree, ctv,
								infos[i], model);
					}
				}
			}

			if (updateDir) {
				// 2.����Ŀ¼�б�
				String errorReport = ihm.onlyDirToHTMLForUserView(
						currentCatalogTree, ctv, model, dirModel);
				errorInfo.append(errorReport);
			}

			Iterator<CatalogTree> itor = currentCatalogTree.getSons()
					.iterator();
			while (itor.hasNext()) {
				stack.push(itor.next());
			}
			long endTime = System.currentTimeMillis();
			long timeCostInSecond = (long) ((endTime - startTime) / 1000.0);
			System.out
					.println("Ŀ¼ID:"
							+ currentCatalogTree.getID()
							+ " Ŀ¼����·��:"
							+ currentCatalogTree.getFullCatalogPath()
							+ " Ŀ¼·��:"
							+ ctv.getURI()
							+ currentCatalogTree.getVirtualFullWebDirPath()
							+ "  �Ѿ��������. ����"
							+ infoCount
							+ "����Ϣ, Ŀ¼"
							+ dirSize
							+ "��. �ܹ�����ʱ��:"
							+ timeCostInSecond
							+ "��"
							+ (timeCostInSecond > 0 ? "ƽ��ÿ����¼�¼"
									+ ((infoCount * 1.0) / (timeCostInSecond + 0.00001))
									+ "��"
									: ""));
		}
		long totalEndTime = System.currentTimeMillis();
		long totalTimeCostInSecond = totalEndTime - totalStartTime;
		System.out.println("\n��ϲ�ˡ�������Ϣ���¼ƻ��Ѿ��ɹ�����. ����" + totalCount
				+ "����Ϣ������ ����ʱ��:" + totalTimeCostInSecond + "�� ƽ��ÿ����¼�¼"
				+ ((totalCount * 1.0) / totalTimeCostInSecond) + "��");
		return errorInfo.toString();
	}

	private static boolean checkHTMLFileStatus(String fileNameWithFullPath) {
		boolean isOK = false;
		long fileLength = 0;
		try {
			File f = new File(fileNameWithFullPath);
			fileLength = f.length();
			if (fileLength <= 0) {
				return false;
			}

			FileInputStream fis = new java.io.FileInputStream(
					fileNameWithFullPath);

			byte[] buf = new byte[(int) fileLength];
			fis.read(buf);
			fis.close();
			String tmp = new String(buf, "GBK");
			int from = tmp.indexOf("<html>");
			int to = tmp.indexOf("</html");
			if (to > from && from >= 0) {
				return true;
			}

		} catch (IOException e) {
			System.out.println(e);
		}

		return isOK;
	}

}