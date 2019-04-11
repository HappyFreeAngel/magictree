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

public class InfoserviceHTMLMaker implements HTMLMaker {
	private static String lineSeparator = System.getProperty("line.separator");
	private static String prefix = "InfoService";

	private static Hashtable<String, String> dictionary = new Hashtable<String, String>(); // IT�ֵ�,ÿ��1Сʱ����һ��.
	private static Timestamp lastestUpdateTime = new Timestamp(
			System.currentTimeMillis());

	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, Info info, StringBuffer model) {
		System.out
				.println("infoToHTMLForUserView(CatalogTree currentInfoCatalogTree, ��������������δʵ�֡�");
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
			if (buf == null) {
				System.out.println(" buf����Ϊ��ֵ.");
			}
			if (newValue == null) {
				System.out.println("newValue����Ϊ��ֵ.");
			}
			if (startMark == null) {
				System.out.println("startMark����Ϊ��ֵ.");
			}
			if (endMark == null) {
				System.out.println("endMark����Ϊ��ֵ.");
			}
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
				currentCatalogTree = root.findByID(Integer
						.parseInt(whichDir[i]));
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

	public InfoserviceHTMLMaker() {
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

	public static void main(String[] args) throws Exception {

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

		WebDesignHTMLMaker maker = new WebDesignHTMLMaker();
		maker.updateInfo(propertiesFileNameWithFullPath);
		// String line = "<object-> flash /new", keyword = "flash";
		// System.out.println(isEnglishKeyword(line,keyword));
		// String testStr="��sss����!$���� ";
		// for(int i=0;i<testStr.length();i++)
		// {
		// if(isHanZi(testStr.charAt(i)))
		// {
		// System.out.println(testStr.charAt(i));
		// }
		// }
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

		int endPos = line.indexOf(endMark, keywordPos);
		int startPos = line.indexOf(startMark, 0);

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
	 *            Ŀ¼��
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
						.println("ϵͳ�������쳣.com.happyfreeangel.magictree.util.ITWaibaoHTMLMaker  public static Hashtable getITDictionay(CatalogTree ct,CatalogTreeView ctv)");
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
					String chongtu = (String) dictionary.put(title,
							ctv.getURI() + htmlURL); // ������ظ�,�����������.
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
		return dictionary;
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

		float budget_lowest = 0.0f;
		float budget_highest = 0.0f;

		ctv.getCatalogTreeDefinitionTableName();
		ctv.getCatalogTreeTableName();

		String SQL = " select "
				+ prefix
				+ "Info.infoID,title,content,author,publishedDate,expiredDate,keywords,derivation,sourceURI,whoCollectIt,latestUpdateTime,insertedTime,viewedCount,latestModifiedMemberID,hasVerify,activeStatus,securityLevel,isAuthorship,budget_lowest,budget_hightest from "
				+ prefix + "Info " + " where " + prefix + "Info.infoType = "
				+ currentInfoCatalogTree.getID() + " and " + prefix
				+ "Info.infoID=" + infoID;

		Statement stmt = null;
		ResultSet rs = null;

		boolean hasFound = false;

		// ��ȡIT�ֵ�. folderID = 124
		Hashtable<String, String> itDictionary = getDictionay(
				currentInfoCatalogTree.getRoot().findByID(28), ctv);

		Object[] tmpitems = dictionary.keySet().toArray();
		String[] items = new String[tmpitems.length];

		// ��items�ַ�����������ַ�������������,������������ǰ��,���2�����ʵȳ�,�����ȡһ��,
		for (int p = 0; p < items.length; p++) {
			items[p] = (String) tmpitems[p];
		}

		// ð�ݷ�����
		for (int m = 0; m < items.length; m++) {
			for (int n = m + 1; n < items.length; n++) {
				if (items[m].length() < items[n].length()) {
					String tmp = items[n];
					items[n] = items[m];
					items[m] = tmp;
				}
			}
		}

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(SQL);

			if (rs != null && rs.next()) {
				infoID = rs.getInt("infoID");
				title = rs.getString("title");
				content = rs.getString("content");
				StringBuffer contentBuf = new StringBuffer();

				BufferedReader buf = new BufferedReader(new StringReader(
						content));

				String line = null;

				String tmpLine = null;
				int ignorelevel = 0;
				int alertCount = 100000;
				int p = 0;
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
						if (tmpLine.indexOf("<br>") >= 0) {
							// �Ѿ�����<br>�Ͳ����ټ���.
						} else {
							line = "<br>" + line;
						}
					}

					// ??��Ҫ����,����ȷһ��.
					for (int h = 0; h < items.length; h++) {
						int keywordPos = isKeywordInMark(line, items[h],
								"<a href=\"", "</a>");
						if (keywordPos <= 0) {
							continue;
						}

						boolean isOK = false;
						if (isEnglishKeyword(items[h])) {
							// ��Ӣ�ﵥ��
							int from = line.indexOf(items[h]);
							int to = from + items[h].length();

							if (from == 0) {
								if (line.length() > items[h].length()) {
									if (line.charAt(items[h].length()) == ' '
											|| line.charAt(items[h].length()) == '��'
											|| isHanZi(line.charAt(items[h]
													.length()))) {
										isOK = true;
									}
								}
							} else if (from > 0 && to < (line.length() - 1)) {
								if ((line.charAt(from - 1) == ' '
										|| line.charAt(from - 1) == '��'
										|| isHanZi(line.charAt(from - 1))
										&& (line.charAt(to) == ' ' || line
												.charAt(to) == '��') || isHanZi(line
											.charAt(to)))) {
									isOK = true;
								}
							} else if (from > 0 && to == (line.length() - 1)) {
								if (line.charAt(from - 1) == ' '
										|| line.charAt(from - 1) == '��'
										|| isHanZi(line.charAt(from - 1))) {
									isOK = true;
								}
							}
							if (isOK) {
								line = line.substring(0, keywordPos)
										+ "<a href=\""
										+ dictionary.get(items[h])
										+ "\" class=IT_Dictionary>"
										+ items[h]
										+ "</a>"
										+ line.substring(keywordPos
												+ items[h].length());
							}
						}

						else {

							line = line.substring(0, keywordPos)
									+ "<a href=\""
									+ dictionary.get(items[h])
									+ "\" class=IT_Dictionary>"
									+ items[h]
									+ "</a>"
									+ line.substring(keywordPos
											+ items[h].length());
						}
					}

					contentBuf.append(line + "\n");
				} // end of while

				content = "<br>&nbsp;&nbsp;" + contentBuf.toString();

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
				budget_lowest = rs.getFloat("budget_lowest");
				budget_highest = rs.getFloat("budget_hightest");// ���ݿ���ֶ���д����.
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

			// 4. ��Ϣ�ؼ���

			String infoKeywordValueStartMark = "<!--��Ϣ�ؼ��ֿ�ʼ-->";
			String infoKeywordValueEndMark = "<!--��Ϣ�ؼ��ֽ���-->";

			String keywordContent = "";
			if (keywords == null) {
				keywordContent = "";
			} else {
				keywordContent = "<a href=\"http://webdesign.it-waibao.com/info/jsp/queryInfo.jsp?keyword=Info.keywords&value="
						+ keywords
						+ "&actionType=requery&pageNumber=1\" class=\"infoKeywordValue\">"
						+ keywords + "</a>";
			}

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

			Calendar C = Calendar.getInstance();

			if (publishedDate != null) {
				// �����������
				// C.setTimeInMillis(publishedDate.getTime());
				// int year = C.get(Calendar.YEAR);
				// int month = C.get(Calendar.MONTH) + 1;
				// int date = C.get(Calendar.DATE);
				// int hour = C.get(Calendar.HOUR);
				// int minute = C.get(Calendar.MINUTE);
				// publisheTimeStr = year + "��" + (month < 10 ? "0" + month :
				// month + "") +
				// "��" + (date < 10 ? "0" + date : date + "") + "��" +
				// (hour < 10 ? "0" + hour : hour + "") + "ʱ" +
				// (minute < 10 ? "0" + minute : minute + "") + "��";

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
				String delim = ", ";
				java.util.StringTokenizer st = new StringTokenizer(keywords,
						delim);
				while (st.hasMoreTokens()) {
					keywordCondition += "(Info.keywords like '%"
							+ st.nextToken() + "%') or ";
				}
			}

			keywordCondition += " (1>2) "; // ������,������.
			keywordCondition = "  ( " + keywordCondition + " ) ";

			String keywordSQL = " select " + prefix + "Info.infoID," + prefix
					+ "Info.title," + prefix + "Info.publishedDate," + prefix
					+ "CatalogTreeHTMLWebPath.virtualWebPath from " + prefix
					+ "CatalogTreeHTMLWebPath," + prefix + "Info where "
					+ keywordCondition + " and " + prefix
					+ "CatalogTreeHTMLWebPath.infoID=" + prefix
					+ "Info.infoID and " + prefix + "Info.infoID!=" + infoID
					+ " order by " + prefix + "Info.publishedDate desc";

			Statement tmpStmt = null;
			ResultSet tmpRS = null;
			try {
				tmpStmt = ctv.getPool().createStatement();
				tmpRS = tmpStmt.executeQuery(keywordSQL);
				int tmpInfoID = 0, xuhao = 0;
				int maxShowCount = 10; // �����ʾ����Ŀ.
				Timestamp tmpPublishedDate = null;
				String tmpTitle = null, tmpVirtualWebPath = null;

				while (tmpRS.next()) {
					tmpInfoID = tmpRS.getInt("infoID");
					tmpTitle = tmpRS.getString("title");
					tmpPublishedDate = tmpRS.getTimestamp("publishedDate");
					tmpVirtualWebPath = tmpRS.getString("virtualWebPath");
					xuhao++;

					// ����������һ����¼.
					relativeLinks.append(lineSeparator + " <tr>");
					relativeLinks.append(lineSeparator
							+ "             <td><div align=\"center\">" + xuhao
							+ "</div></td>");
					relativeLinks.append(lineSeparator
							+ "             <td><div align=\"left\"><a href=\""
							+ ctv.getURI() + tmpVirtualWebPath
							+ "\" class=\"relativeLink\">" + tmpTitle
							+ "</a></div></td>");
					relativeLinks.append(lineSeparator
							+ "             <td>"
							+ (tmpPublishedDate == null ? "" : tmpPublishedDate
									.toString().substring(0, 19)) + "</td>");
					relativeLinks.append(lineSeparator + "           </tr>");
					if (xuhao >= maxShowCount) {
						break;
					}
				}

				if (tmpRS.next()) {
					relativeLinks.append(lineSeparator + "<tr>");
					relativeLinks.append(lineSeparator
							+ "         <td>&nbsp;</td>");
					relativeLinks.append(lineSeparator
							+ "         <td>&nbsp;</td>");
					relativeLinks
							.append(lineSeparator
									+ "         <td><a href=\"http://webdesign.it-waibao.com/info/jsp/queryInfo.jsp?keyword=Info.keywords&value="
									+ keywords
									+ "&actionType=requery&pageNumber=1\"\" class=\"relativeLink\">�鿴������ص���Ϣ...</a></td>");
					relativeLinks.append(lineSeparator + "       </tr>");
				}

				relativeLinks.append(lineSeparator + "          </table>");

				if (relativeLinks.indexOf("http://webdesign.it-waibao.com") < 0) {
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
				PreparedStatement pstmt = null, pstmt2 = null;
				int affectedRow = 0;
				try {
					String sql = " delete from "
							+ prefix
							+ "CatalogTreeHTMLWebPath where catalogTreeID = ? and folderID= ? and infoID=?";
					String insertSQL = " insert into "
							+ prefix
							+ "CatalogTreeHTMLWebPath(catalogTreeID,folderID,infoID,virtualWebPath) values(?,?,?,?) ";

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

					// System.out.println("virtualWebPath=" + virtualWebPath);
					pstmt2.setString(4, virtualWebPath);
					affectedRow = pstmt2.executeUpdate();
					pstmt2.close();

					// if (affectedRow == 1)
					// {
					// //System.out.println(virtualWebPath +
					// " has been record in database. ");
					// }
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
					} catch (SQLException e) {
						System.out.println(e);
					}
				}
				// System.out.println("record to database..." +
				// fileNameWithFullPath);
			}
		}
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

		String SQL = " select 'xuhao'," + prefix + "Info.infoID,title,"
				+ ctv.getCatalogTreeTableName() + ".folderID,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".name,author,publishedDate,'�༭' from "
				+ ctv.getCatalogTreeTableName() + ",Info where "
				+ ctv.getCatalogTreeTableName() + ".catalogTreeID = "
				+ ctv.getCatalogTreeID() + " and "
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
				String[] columnNames = { "���", "��ϢID", "����", "��Ϣ����ID", "����",
						"��������", "�༭" };
				info.setColumnNames(columnNames);
				happy.web.HtmlTable h = new happy.web.HtmlTable(data, info);
				HtmlTableCell xuhaoCell = null, infoIDCell = null, titleCell = null, infoCatalogTreeIDCell = null, editCell = null;
				String title = null, tmpStr = null, edit = null, xuhaoStr = null;
				Integer infoID = null;
				int limitBytes = 50;
				int tmpInfoCatalogTreeID = 0;
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
					// infoCatalogTreeIDCell = h.getTableCell(row, 3);
					editCell = h.getTableCell(row, 6);

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
				h.setBordercolordark("#ffffff");
				h.setBordercolor("#888888");
				h.setPixelsPerByteShow(2.6);
				h.hideColumn("��Ϣ����ID");
				h.hideColumn("��ʽ");
				h.hideColumn("��ϢID");
				h.hideColumn("��������");
				h.hideColumn("�༭");
				h.hideColumn("��Ϣ����"); // ��Ϊ����Ŀ¼�ṹ���Ѿ��ǳ�����ˣ�û�б�Ҫ��ʾ����������Ϣ.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("��������",
						"year-month%2-date%2 hour%2:minute%2");
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
				String formName = "form1";
				String eventName = "onClick";
				String functionName = "test";

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
				String url = "";
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
			Iterator itor = currentInfoCatalogTree.getSons().iterator();
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
				int tmpInfoCatalogTreeID = 0;
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
					// infoToHTMLForUserView(currentInfoCatalogTree, ctv,
					// infoID.intValue(),
					// model);

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
				h.setBordercolordark("#ffffff");
				h.setBordercolor("#888888");
				h.setPixelsPerByteShow(2.6);
				h.hideColumn("��Ϣ����ID");
				h.hideColumn("��ʽ");
				h.hideColumn("��ϢID");
				h.hideColumn("��������");
				h.hideColumn("�༭");
				h.setColumnDataFormat("��������",
						"year-month%2-date%2 hour%2:minute%2");
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
				String formName = "form1";
				String eventName = "onClick";
				String functionName = "test";

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
				String url = "";
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
			Iterator itor = currentInfoCatalogTree.getSons().iterator();
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
		WebDesignHTMLMaker ihm = new WebDesignHTMLMaker();
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

	/**
	 * ����վ��ӵ���Ϣ�л�ȡ������ӵ���Ϣ
	 * 
	 * @param ctv
	 * @param returnCount
	 * @return
	 */
	public static Info[] getLatestInfoFromWebSite(CatalogTreeView ctv,
			int returnCount) {
		if (returnCount <= 0) {
			return null;
		}
		Info[] infos = null;
		int infoID = 0;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID  order by InfoID desc limit "
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
			Info info = null;
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
		int infoID = 0;
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID and Info.keywords like '%"
				+ keyword
				+ "%'  order by InfoID desc limit "
				+ returnCount
				+ ";";

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
			Info info = null;
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

		int infoID = 0;
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
			Info info = null;
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
		/**
		 * //write to html file. String fullPathDir =
		 * currentInfoCatalogTree.getSystemFullDirPath(ctv.
		 * getCatalogRootInSystemDir()); File f = new File(fullPathDir); if
		 * (!f.exists()) { f.mkdirs(); } // String fileNameWithFullPath =
		 * fullPathDir + java.io.File.separator + // "catalogTreeID" +
		 * ctv.getCatalogTreeID() + "_folderID" + //
		 * currentInfoCatalogTree.getID() + // "_infoID" + infoID + ".html";
		 * //�򵥻��ļ����� String fileNameWithFullPath = fullPathDir +
		 * java.io.File.separator + + ctv.getCatalogTreeID() + "_"
		 * +currentInfoCatalogTree.getID() +"_" + infoID + ".html"; //Ҳ����д�ڸ�Ŀ¼��.
		 * boolean htmlCreateOK = false; try { FileOutputStream fout = new
		 * FileOutputStream(fileNameWithFullPath); byte[] buf = null; try {
		 * //=========== String temp = model.toString(); buf =
		 * model.toString().getBytes("GBK");//??"GBK" }catch(Exception ex) {
		 * System.out.println("��������Ϣ�����޷�����GBK������ת��."+ex); } if(buf!=null) {
		 * //System.out.println("buf size="+buf.length+"�ֽ�"); fout.write(buf);
		 * //�����ַ�����ԭʼ��ʽֱ��д���ļ���ȥ. fout.close(); //�ж��ļ��Ƿ񴴽��ɹ�. File tmpF = new
		 * File(fileNameWithFullPath); if (tmpF.length() == buf.length) {
		 * htmlCreateOK = true; } else if (tmpF.length() > 0 && tmpF.length() <
		 * buf.length) { System.out.println("�ļ�:" + fileNameWithFullPath +
		 * "����ʧ��,�������ݶ�ʧ. ԭ���ļ���С" + buf.length + "�ֽ�,�����ɺ�Ĵ�С��" + tmpF.length() +
		 * "�ֽ�."); } else if (tmpF.length() == 0) { System.out.println("�ļ�:" +
		 * fileNameWithFullPath + "����ʧ��,�ļ���С��0�ֽ�."); } } else {
		 * System.out.println
		 * ("buf==null  ��Model д���ļ�ʱ�����˴���.fileNameWithFullPath="
		 * +fileNameWithFullPath); } } catch (IOException e) {
		 * System.out.println(e); }
		 */

		StringBuffer errorInfo = new StringBuffer();
		WebDesignHTMLMaker ihm = new WebDesignHTMLMaker();

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
		StringBuffer errorInfo = new StringBuffer();
		WebDesignHTMLMaker ihm = new WebDesignHTMLMaker();

		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(ct);

		while (stack.size() > 0) {
			CatalogTree tmp = (CatalogTree) stack.pop();
			int[] infoIDs = null;
			if (updateInfo) {
				// 1.��ȡ��ǰĿ¼�µ�����infoID
				infoIDs = getInfoIDFromCatalog(ctv, tmp);
				if (infoIDs != null) {
					for (int i = 0; i < infoIDs.length; i++) {
						infoToHTMLForUserView(tmp, ctv, infoIDs[i], model);
					}
				}
			}

			if (updateDir) {
				// 2.����Ŀ¼�б�
				String errorReport = ihm.onlyDirToHTMLForUserView(tmp, ctv,
						model, dirModel);
				errorInfo.append(errorReport);
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