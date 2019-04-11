package com.happyfreeangel.application;

/**
 * <p>Title: 魔树</p>
 * <p>Description: 魔树</p>
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

	private static Hashtable<String, String> dictionary = new Hashtable<String, String>(); // IT字典,每隔1小时更新一次.
	private static Timestamp lastestUpdateTime = new Timestamp(
			System.currentTimeMillis());

	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, Info info, StringBuffer model) {
		System.out
				.println("infoToHTMLForUserView(CatalogTree currentInfoCatalogTree, 。。。）功能尚未实现。");
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
			if (buf == null) {
				System.out.println(" buf不能为空值.");
			}
			if (newValue == null) {
				System.out.println("newValue不能为空值.");
			}
			if (startMark == null) {
				System.out.println("startMark不能为空值.");
			}
			if (endMark == null) {
				System.out.println("endMark不能为空值.");
			}
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

			fin.close();
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
			System.out.println("error! catalogTreeView 加载失败.");
			return;
		}

		if (catalogTreeView != null) {
			root = CatalogTree.loadFromDatabase(catalogTreeView);
			currentCatalogTree = root;
		}
		if (root == null) {
			System.out.println("catalogTree 加载失败.");
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
			System.out.println("没有什么需要更新的.");
			return;
		}

		// 一个个更新指定的目录.
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
		String regEx = "^[a-zA-Z]"; // 表示a或f
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(keyword);
		if (m != null) {
			return m.find();
		}
		return false;
	}

	/**
	 * 判断输入的字符是否是汉字.
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
		// String testStr="你sss开心!$　呵 ";
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
	 *            字符串
	 * @param keyword
	 *            关键字
	 * @param startMark
	 *            开始标志
	 * @param endMark
	 *            结束标志
	 * @return keywordPos 判断 是否是 startMark+keyword+endMark 这种结构
	 */
	private static int isKeywordInMark(String line, String keyword,
			String startMark, String endMark) {
		if (line == null || keyword == null || startMark == null
				|| endMark == null) {
			return -1;
		}

		// 1.根本就不存在
		int keywordPos = line.indexOf(keyword);
		if (keywordPos < 0) {
			return -1;
		}

		int endPos = line.indexOf(endMark, keywordPos);
		int startPos = line.indexOf(startMark, 0);

		// 2.已处理过的,不能再重复处理
		if (startPos < keywordPos && keywordPos < endPos) {
			return 0;
		}

		// 3.有包含,如果包含了 hkeywordk 这种类型的就不应该解释。如果keyword是英文开头或英文结尾的,
		// 如果keyword前面是英文或keyword后面是英文,就放弃解释,返回-1. //2006-11-09 22:55 happy
		if (keyword.charAt(0) >= 'A' && keyword.charAt(0) <= 'Z'
				|| keyword.charAt(0) >= 'a' && keyword.charAt(0) <= 'z') {
			if (
			// 关键字之前的英文字符号
			(keywordPos >= 1 && line.charAt(keywordPos - 1) >= 'A'
					&& line.charAt(keywordPos - 1) <= 'Z' || keywordPos >= 1
					&& line.charAt(keywordPos - 1) >= 'a'
					&& line.charAt(keywordPos - 1) <= 'z')
					|| // //关键字之后的英文字符号
						// 防止空值出错.
					((keywordPos + keyword.length()) < line.length()
							&& line.charAt(keywordPos + keyword.length()) >= 'A'
							&& line.charAt(keywordPos + keyword.length()) <= 'Z' || (keywordPos + keyword
							.length()) < line.length()
							&& line.charAt(keywordPos + keyword.length()) >= 'a'
							&& line.charAt(keywordPos + keyword.length()) <= 'z')) {
				return -1;
			}
		}

		// 4. 正确的包含了.

		return keywordPos;
	}

	/**
	 * 
	 * @param currentInfoCatalogTree
	 *            目录树
	 * @param ctv
	 *            目录树的视图
	 * @return
	 */
	public static Hashtable<String, String> getDictionay(CatalogTree ct,
			CatalogTreeView ctv) {
		// 1.定位到IT术语 一周更新一次.
		if (System.currentTimeMillis() - lastestUpdateTime.getTime() < 1000 * 3600 * 24 * 7
				&& dictionary.size() > 0) {
			return dictionary;
		}

		dictionary = new Hashtable<String, String>(); // 主键 IT术语, 键值 URL静态网页链接

		if (ct == null) {
			System.out
					.println("词典是空的,public static Hashtable getITDictionay(CatalogTree ct, CatalogTreeView ctv)    CatalogTree ct ==null");
			return dictionary;
		}
		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(ct);
		int alert = 100000; // 如果超过这个值,则系统肯定发生了异常,必须强行退出.
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
						.println("系统发生了异常.com.happyfreeangel.magictree.util.ITWaibaoHTMLMaker  public static Hashtable getITDictionay(CatalogTree ct,CatalogTreeView ctv)");
				break;
			}

			// 当前节点
			// 1.添加当前目录的链接
			if (tmp != null) {
				dictionary.put(tmp.getName(),
						ctv.getURI() + tmp.getVirtualFullWebDirPath()
								+ "/index.html");
			}

			// 2.添加当前目录下的术语的链接
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
							ctv.getURI() + htmlURL); // 如果有重复,则后来者优先.
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
	 * 把一个信息转化为HTML格式.
	 * 
	 * @param t
	 *            CatalogTree
	 * @param infoID
	 *            int
	 */
	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, int infoID, StringBuffer model) {
		// System.out.println(" 当你看到这些文字时,infoID="+infoID+" 正在被转化,正要开始.");
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

		// 获取IT字典. folderID = 124
		Hashtable<String, String> itDictionary = getDictionay(
				currentInfoCatalogTree.getRoot().findByID(28), ctv);

		Object[] tmpitems = dictionary.keySet().toArray();
		String[] items = new String[tmpitems.length];

		// 把items字符串数组根据字符串长度来排序,长的优先排在前面,如果2个单词等长,就随便取一个,
		for (int p = 0; p < items.length; p++) {
			items[p] = (String) tmpitems[p];
		}

		// 冒泡法排序
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
						System.out.println("系统陷入死循环.");
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

					if (ignorelevel == 0) // 表示现在不在表格里面,不管表格有多少层嵌套.
					{
						if (tmpLine.indexOf("<br>") >= 0) {
							// 已经有了<br>就不用再加了.
						} else {
							line = "<br>" + line;
						}
					}

					// ??需要完善,更精确一点.
					for (int h = 0; h < items.length; h++) {
						int keywordPos = isKeywordInMark(line, items[h],
								"<a href=\"", "</a>");
						if (keywordPos <= 0) {
							continue;
						}

						boolean isOK = false;
						if (isEnglishKeyword(items[h])) {
							// 是英语单词
							int from = line.indexOf(items[h]);
							int to = from + items[h].length();

							if (from == 0) {
								if (line.length() > items[h].length()) {
									if (line.charAt(items[h].length()) == ' '
											|| line.charAt(items[h].length()) == '　'
											|| isHanZi(line.charAt(items[h]
													.length()))) {
										isOK = true;
									}
								}
							} else if (from > 0 && to < (line.length() - 1)) {
								if ((line.charAt(from - 1) == ' '
										|| line.charAt(from - 1) == '　'
										|| isHanZi(line.charAt(from - 1))
										&& (line.charAt(to) == ' ' || line
												.charAt(to) == '　') || isHanZi(line
											.charAt(to)))) {
									isOK = true;
								}
							} else if (from > 0 && to == (line.length() - 1)) {
								if (line.charAt(from - 1) == ' '
										|| line.charAt(from - 1) == '　'
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
				budget_highest = rs.getFloat("budget_hightest");// 数据库的字段名写错了.
			} else {
				System.out.println(" SQL 查询,找不到资料 infoID=" + infoID + " SQL="
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
			System.out.println("找不到信息:" + ctv.getCatalogTreeTableName()
					+ "=    infoID=" + infoID + " catalogName="
					+ currentInfoCatalogTree.getName() + " 所以HTML文件无法生成");
			return;
		}
		boolean retainMark = false;

		// System.out.println("模版替换开始...infoID="+infoID);

		// 0.判断是否存在多个分页.如果内容有分页，则生成多个页面
		String mutiPageMark = "\\[--信息正文内容分页--\\]";
		String[] mutiPageContent = content.split(mutiPageMark);

		StringBuffer copyModel = new StringBuffer(model.toString());
		// 简单化文件名称
		String basefullPathDir = currentInfoCatalogTree
				.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

		String basefileNameWithFullPath = basefullPathDir
				+ java.io.File.separator + +ctv.getCatalogTreeID() + "_"
				+ currentInfoCatalogTree.getID() + "_" + infoID; // + ".html";

		for (int i = 0; i < mutiPageContent.length; i++) {
			model = new StringBuffer(copyModel.toString()); // 对每个内容分页都用相同的模板替换.

			// 1.替换标题
			retainMark = false;
			String infoTitleStartMark = "<!--信息标题开始-->";
			String infoTitleEndMark = "<!--信息标题结束-->";
			model = findAndReplace(model, infoTitleStartMark, infoTitleEndMark,
					title, retainMark);

			// 2.替换菜单
			retainMark = false;
			String infoMenuStartMark = "<!--菜单开始-->";
			String infoMenuEndMark = "<!--菜单结束-->";

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

			// 3。信息横向导航栏
			String infoPathStartMark = "<!--信息横向目录开始-->";
			String infoPathEndMark = "<!--信息横向目录结束-->";

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

			// 4. 信息关键字

			String infoKeywordValueStartMark = "<!--信息关键字开始-->";
			String infoKeywordValueEndMark = "<!--信息关键字结束-->";

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

			// 5. 信息作者

			String infoAuthorStartMark = "<!--信息作者开始-->";
			String infoAuthorEndMark = "<!--信息作者结束-->";

			model = findAndReplace(model, infoAuthorStartMark,
					infoAuthorEndMark, author, retainMark);

			// 5.信息出处
			String infoDerivationStartMark = "<!--信息出处开始-->";
			String infoDerivationEndMark = "<!--信息出处结束-->";

			String infoDerivation = "";

			if (sourceURI != null
					&& sourceURI.toLowerCase().indexOf("http://") >= 0) {
				infoDerivation = "<a href=\"" + sourceURI
						+ "\" class=infoDerivationLink target=_blank>"
						+ derivation + "</a>";
			} else {
				infoDerivation = "" + derivation;
			}

			// 6.信息发布时间

			String infoPublishedTimeStartMark = "<!--信息发布时间开始-->";
			String infoPublishedTimeEndMark = "<!--信息发布时间结束-->";

			String publisheTimeStr = "";

			Calendar C = Calendar.getInstance();

			if (publishedDate != null) {
				// 不带纳秒毫秒
				// C.setTimeInMillis(publishedDate.getTime());
				// int year = C.get(Calendar.YEAR);
				// int month = C.get(Calendar.MONTH) + 1;
				// int date = C.get(Calendar.DATE);
				// int hour = C.get(Calendar.HOUR);
				// int minute = C.get(Calendar.MINUTE);
				// publisheTimeStr = year + "年" + (month < 10 ? "0" + month :
				// month + "") +
				// "月" + (date < 10 ? "0" + date : date + "") + "日" +
				// (hour < 10 ? "0" + hour : hour + "") + "时" +
				// (minute < 10 ? "0" + minute : minute + "") + "分";

				// 全部换成英文显示格式 2007-03-02 11:41
				Timestamp t = new Timestamp(publishedDate.getTime());
				publisheTimeStr = t.toString().substring(0, 16);
			}
			model = findAndReplace(model, infoPublishedTimeStartMark,
					infoPublishedTimeEndMark, publisheTimeStr, retainMark);
			// 2005年04月28日01时27分

			// 7.信息类别

			String infoTypeStartMark = "<!--信息类别开始-->";
			String infoTypeEndMark = "<!--信息类别结束-->";

			// 当前目录的名称,信息类别
			String htmlURLOfInfoType = "<a href=\"" + ctv.getURI()
					+ currentInfoCatalogTree.getVirtualFullWebDirPath()
					+ "/index.html\" class=\"infoPath\">"
					+ currentInfoCatalogTree.getName() + "</a>";

			model = findAndReplace(model, infoTypeStartMark, infoTypeEndMark,
					htmlURLOfInfoType, retainMark);

			// 8. <!--信息正文内容开始-->

			String infoContentStartMark = "<!--信息正文内容开始-->";
			String infoContentEndMark = "<!--信息正文内容结束-->";

			// //8.1 如果内容有分页，则生成多个页面
			// String mutiPageMark="<!--信息正文内容分页-->";
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

			// 8.1 插入内容分页的分页链接.
			// 首页 上一页 [1] [2] [3] [4] [5] [6] [7] [8] [9] [10] .. 下一页 尾页

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

			// 9.信息ID替换

			String infoIDStartMark = "<!--信息ID开始-->";
			String infoIDEndMark = "<!--信息ID结束-->";
			retainMark = false;
			model = findAndReplace(model, infoIDStartMark, infoIDEndMark,
					infoID + "", retainMark);

			// 10.信息被查看次数
			String infoViewedCountStartMark = "<!--信息访问人次开始-->";
			String infoViewedCountEndMark = "<!--信息访问人次结束-->";

			model = findAndReplace(model, infoViewedCountStartMark,
					infoViewedCountEndMark, viewedCount + "", retainMark);
			// 11.信息相关链接

			StringBuffer relativeLinks = new StringBuffer();

			String infoRelativeStartMark = "<!--信息相关链接开始-->";
			String infoRelativeEndMark = "<!--信息相关链接结束-->";

			relativeLinks
					.append(lineSeparator
							+ "        <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");

			relativeLinks.append(lineSeparator + " <tr>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"7%\"><div align=\"center\"><strong>序号 </strong></div></td>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"60%\"><strong>标题 </strong></td>");
			relativeLinks
					.append(lineSeparator
							+ "                  <td align=\"left\" width=\"33%\"><strong>发布日期 </strong></td>");
			relativeLinks.append(lineSeparator + "                </tr>");

			String keywordCondition = "";
			if (keywords != null) {
				// 解析 keywords
				String delim = ", ";
				java.util.StringTokenizer st = new StringTokenizer(keywords,
						delim);
				while (st.hasMoreTokens()) {
					keywordCondition += "(Info.keywords like '%"
							+ st.nextToken() + "%') or ";
				}
			}

			keywordCondition += " (1>2) "; // 假条件,充数的.
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
				int maxShowCount = 10; // 最多显示的数目.
				Timestamp tmpPublishedDate = null;
				String tmpTitle = null, tmpVirtualWebPath = null;

				while (tmpRS.next()) {
					tmpInfoID = tmpRS.getInt("infoID");
					tmpTitle = tmpRS.getString("title");
					tmpPublishedDate = tmpRS.getTimestamp("publishedDate");
					tmpVirtualWebPath = tmpRS.getString("virtualWebPath");
					xuhao++;

					// 往表格里添加一条记录.
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
									+ "&actionType=requery&pageNumber=1\"\" class=\"relativeLink\">查看所有相关的信息...</a></td>");
					relativeLinks.append(lineSeparator + "       </tr>");
				}

				relativeLinks.append(lineSeparator + "          </table>");

				if (relativeLinks.indexOf("http://webdesign.it-waibao.com") < 0) {
					relativeLinks = new StringBuffer(); // 一条记录也没有,完全清空.
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

			// System.out.println("模版替换结束...infoID="+infoID);

			// write to html file.
			String fullPathDir = currentInfoCatalogTree
					.getSystemFullDirPath(ctv.getCatalogRootInSystemDir());

			File f = new File(fullPathDir);
			if (!f.exists()) {
				f.mkdirs();
			}

			// 简单化文件名称
			String fileNameWithFullPath = fullPathDir + java.io.File.separator
					+ +ctv.getCatalogTreeID() + "_"
					+ currentInfoCatalogTree.getID() + "_" + infoID + ".html";

			// 也可以写在根目录下.
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
					System.out.println("文章信信息内容无法按照GBK编码来转化." + ex);
				}

				if (buf != null) {
					// 创建第一页. fileNameWithFullPath
					if (i == 0 && mutiPageContent.length > 1) {
						// mutiPageFileNameWithFullPath = fileNameWithFullPath;
						FileOutputStream fout2 = new FileOutputStream(
								fileNameWithFullPath);
						fout2.write(buf);
						fout2.close();
					}

					fout.write(buf); // 按照字符串的原始格式直接写到文件中去.
					fout.close();

					// 判断文件是否创建成功.
					File tmpF = new File(mutiPageFileNameWithFullPath);
					if (tmpF.length() == buf.length) {
						htmlCreateOK = true;
					} else if (tmpF.length() > 0 && tmpF.length() < buf.length) {
						System.out.println("文件:" + fileNameWithFullPath
								+ "生成失败,部分内容丢失. 原来文件大小" + buf.length
								+ "字节,而生成后的大小是" + tmpF.length() + "字节.");
					} else if (tmpF.length() == 0) {
						System.out.println("文件:" + mutiPageFileNameWithFullPath
								+ "生成失败,文件大小是0字节.");
					}
				} else {
					System.out
							.println("buf==null  把Model 写到文件时发生了错误.fileNameWithFullPath="
									+ fileNameWithFullPath);
				}
			} catch (IOException e) {
				System.out.println(e);
			}

			// 记录到数据库中去,如果有多页，只记录第一页信息.

			if (htmlCreateOK && i == 0) {
				// record to database;
				/*
				 * drop table InfoCatalogTreeHTMLWebPath; create table
				 * InfoCatalogTreeHTMLWebPath ( catalogTreeID int not null,
				 * #目录树ID folderID int not null, #目录ID infoID int not null,
				 * #信息ID virtualWebPath varchar(255) not null, #信息存放的网站路径
				 * 从树目录的根目录开始计算. http://localhost/html /it-waibao/ primary
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
	 *            要显示的当前页
	 * @param baseHTMLURL
	 *            baseHTMLURL
	 * @param paramName
	 * @return
	 */
	// 首页 上一页 [1] [2] [3] [4] [5] [6] [7] [8] [9] [10] .. 下一页 尾页
	private static String getPageFootURL(int num, String baseHTMLURL,
			int urlSize, int totalPage) {
		int base = (int) ((num - 1) / urlSize);
		int first = urlSize * base;
		String info = "";
		if (num == 1) {
			info += "&nbsp;首页&nbsp;&nbsp;<<上一页";
		}

		if (num > 1) {
			info += "<A HREF=\"" + baseHTMLURL + "-1.html"
					+ "\">&nbsp;首页</A>&nbsp;&nbsp;";
			info += "<A HREF=\"" + baseHTMLURL + "-" + (num - 1) + ".html"
					+ "\">&nbsp;<<上一页</A>";
		}

		String url = "";
		// 有
		for (int k = 1; k <= urlSize; k++) {
			int tmp = first + k;
			if (tmp <= 0) {
				continue;
			}
			if (tmp > totalPage) {
				break;
			}
			if (num == k) {
				url = "&nbsp;[" + tmp + "]"; // 当前页就不用链接.
			} else {
				url = "<A HREF=\"" + baseHTMLURL + "-" + tmp + ".html"
						+ "\">&nbsp;[" + tmp + "]</A>";
			}
			info += url;
		}

		if (num < totalPage) {
			info += "<A HREF=\"" + baseHTMLURL + "-" + (num + 1) + ".html"
					+ "\">&nbsp;下一页>></A>";
			info += "<A HREF=\"" + baseHTMLURL + "-" + totalPage + ".html"
					+ "\">&nbsp;&nbsp;&nbsp;最后一页</A>";
		} else if (num == totalPage) {
			info += "&nbsp;下一页>>";
			info += "&nbsp;&nbsp;&nbsp;最后一页";
		}
		return info;
	}

	/**
	 * 
	 * @param t
	 *            要转化为HTML的目录
	 * @param ctv
	 *            目录树信息视图
	 * @param model
	 *            模版
	 * @param dirModel
	 *            目录模版
	 * @return
	 */
	public String dirToHTMLForUserView(CatalogTree t, CatalogTreeView ctv,
			StringBuffer model, StringBuffer dirModel) {
		StringBuffer errorInfo = new StringBuffer();
		CatalogTree currentInfoCatalogTree = t;

		String SQL = " select 'xuhao'," + prefix + "Info.infoID,title,"
				+ ctv.getCatalogTreeTableName() + ".folderID,"
				+ ctv.getCatalogTreeDefinitionTableName()
				+ ".name,author,publishedDate,'编辑' from "
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
				searchInfo.setUrlSize(10); // 底栏最多显示10个链接

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

			if ((data == null || data.length == 0) || info == null) // 如果无法获取数据,下面的显示将无法进行.
			{
				tableContent = "<br><br><div align=\"center\">;&nbsp;当前目录下尚未添加信息.&nbsp;</div>";
				// 补上空行.
				for (int k = 0; k < pageSize; k++) {
					tableContent += "\n\r <br>";
				}

			} else {
				String[] columnNames = { "序号", "信息ID", "标题", "信息类型ID", "作者",
						"发布日期", "编辑" };
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

				for (int row = 1; row < h.getRowCount(); row++) // 共
																// h.getRowCount()
																// 行,第一行是表头.
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

					// 转化信息内容为HTML格式的文件到当前目录下.
					// System.out.println("准备转化Info to HTML infoID="+infoID);
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
								+ "\" target=\"_blank\" class=\"infoListLink\">编辑</a>";
						editCell.setContent(edit);
					}
				}

				h.setTableHeadAlign("left");
				h.setRowHeight(0, "30");
				h.setRowValign(0, "middle");
				h.setBordercolordark("#ffffff");
				h.setBordercolor("#888888");
				h.setPixelsPerByteShow(2.6);
				h.hideColumn("信息类型ID");
				h.hideColumn("格式");
				h.hideColumn("信息ID");
				h.hideColumn("语言类型");
				h.hideColumn("编辑");
				h.hideColumn("信息类型"); // 因为文章目录结构里已经非常清楚了，没有必要显示这个冗余的信息.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("发布日期",
						"year-month%2-date%2 hour%2:minute%2");
				// h.setColumnDataFormat("最近一次报修时间","year-month%2-date%2 hour%2:minute%2");//
				// 11:24:00.437
				// String eventsAndParams =
				// "onmouseover=\"over_change(this,'#C3E7FA');\" \" onmouseout=\"out_change(this,'');\"";
				// h.setBodyRowsEventsAndParams(eventsAndParams);
				h.setAlign("left");
				tableContent = h.toString();

				// 补上空行.
				for (int k = 0; k < pageSize - h.getRowCount(); k++) {
					tableContent += "\n\r <br>";
				}

				// 显示页角
				String formName = "form1";
				String eventName = "onClick";
				String functionName = "test";

				// 需要加强.
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

				urls = "<font size=\"-1\" color=\"#FF80FF\">当前页:&nbsp;第"
						+ currentShowingPage
						+ "页</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				if (currentShowingPage > searchInfo.getUrlSize()) //
				{
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ "1.html\" class=\"infoListFootLink\">&nbsp;第一页</a>&nbsp;&nbsp;";
				}
				String url = "";
				if (num > searchInfo.getUrlSize()) // 有前一页,打印前一页。
				{
					int prePage = 0;
					if (num % searchInfo.getUrlSize() != 0) {
						prePage = num - num % searchInfo.getUrlSize();
					} else {
						prePage = num - searchInfo.getUrlSize(); // 刚好在边界上,这时候前一页面就是当前页面-每个页面大小

					}
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ prePage
							+ ".html\" class=\"infoListFootLink\">&nbsp;前一页</a>";
				}
				// 有
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
							+ "&nbsp;下一页</a>";
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ totalPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;&nbsp;&nbsp;最后一页</a>";
				}
			}

			// 存放html 字符串数据.

			StringBuffer tmpDirModel = new StringBuffer(dirModel.toString());
			boolean retainMark = false;

			// 1.替换标题

			String infoTitleStartMark = "<!--信息标题开始-->";
			String infoTitleEndMark = "<!--信息标题结束-->";
			String dirInfoTitle = t.getFullCatalogPath();
			tmpDirModel = findAndReplace(tmpDirModel, infoTitleStartMark,
					infoTitleEndMark, dirInfoTitle, retainMark);

			// 2.信息横向目录
			String infoPathStartMark = "<!--信息横向目录开始-->";
			String infoPathEndMark = "<!--信息横向目录结束-->";

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

			// 9.<!--信息目录ID开始-->547<!--信息目录ID结束-->替换

			String infoCatalogTreeIDStartMark = "<!--信息目录ID开始-->";
			String infoCatalogTreeIDEndMark = "<!--信息目录ID结束-->";
			retainMark = false;
			tmpDirModel = findAndReplace(tmpDirModel,
					infoCatalogTreeIDStartMark, infoCatalogTreeIDEndMark,
					currentInfoCatalogTree.getID() + "", retainMark);

			// 2.替换菜单
			retainMark = false;
			String infoMenuStartMark = "<!--菜单开始-->";
			String infoMenuEndMark = "<!--菜单结束-->";

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

			String infoContentStartMark = "<!--信息正文内容开始-->";
			String infoContentEndMark = "<!--信息正文内容结束-->";

			tmpDirModel = findAndReplace(tmpDirModel, infoContentStartMark,
					infoContentEndMark, tableContent, retainMark);

			// <!--分页显示链接结束-->

			String showByPageStartMark = "<!--分页显示链接开始-->";
			String showByPageEndMark = "<!--分页显示链接结束-->";

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
				// 这里可以改进，能不能不用String, 直接在二进制里就可以替换.
				try {
					FileOutputStream fout = new FileOutputStream(indexFileName);
					fout.write(tmpDirModel.toString().getBytes("GBK")); // 按照字符串的原始格式直接写到文件中去.
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
				fout.write(tmpDirModel.toString().getBytes("GBK")); // 按照字符串的原始格式直接写到文件中去.
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
	 *            要转化为HTML的目录
	 * @param ctv
	 *            目录树信息视图
	 * @param model
	 *            模版
	 * @param dirModel
	 *            目录模版
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
				+ ".name,author,FileFormat.name,LanguageType.name,publishedDate,'编辑' from "
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
				searchInfo.setUrlSize(10); // 底栏最多显示10个链接

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

			if ((data == null || data.length == 0) || info == null) // 如果无法获取数据,下面的显示将无法进行.
			{
				tableContent = "<br><br><div align=\"center\">;&nbsp;当前目录下尚未添加信息.&nbsp;</div>";
				// 补上空行.
				for (int k = 0; k < pageSize; k++) {
					tableContent += "\n\r <br>";
				}

			} else {
				String[] columnNames = { "序号", "信息ID", "标题", "信息类型ID", "信息类型",
						"作者", "格式", "语言类型", "发布日期", "编辑" };
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

				for (int row = 1; row < h.getRowCount(); row++) // 共
																// h.getRowCount()
																// 行,第一行是表头.
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

					// 转化信息内容为HTML格式的文件到当前目录下.
					// System.out.println("准备转化Info to HTML infoID="+infoID);
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
								+ "\" target=\"_blank\" class=\"infoListLink\">编辑</a>";
						editCell.setContent(edit);
					}
				}

				h.setTableHeadAlign("left");
				h.setRowHeight(0, "30");
				h.setRowValign(0, "middle");
				h.setBordercolordark("#ffffff");
				h.setBordercolor("#888888");
				h.setPixelsPerByteShow(2.6);
				h.hideColumn("信息类型ID");
				h.hideColumn("格式");
				h.hideColumn("信息ID");
				h.hideColumn("语言类型");
				h.hideColumn("编辑");
				h.setColumnDataFormat("发布日期",
						"year-month%2-date%2 hour%2:minute%2");
				// h.setColumnDataFormat("最近一次报修时间","year-month%2-date%2 hour%2:minute%2");//
				// 11:24:00.437
				// String eventsAndParams =
				// "onmouseover=\"over_change(this,'#C3E7FA');\" \" onmouseout=\"out_change(this,'');\"";
				// h.setBodyRowsEventsAndParams(eventsAndParams);
				h.setAlign("left");
				tableContent = h.toString();

				// 补上空行.
				for (int k = 0; k < pageSize - h.getRowCount(); k++) {
					tableContent += "\n\r <br>";
				}

				// 显示页角
				String formName = "form1";
				String eventName = "onClick";
				String functionName = "test";

				// 需要加强.
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

				urls = "<font size=\"-1\" color=\"#FF80FF\">当前页:&nbsp;第"
						+ currentShowingPage
						+ "页</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				if (currentShowingPage > searchInfo.getUrlSize()) //
				{
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ "1.html\" class=\"infoListFootLink\">&nbsp;第一页</a>&nbsp;&nbsp;";
				}
				String url = "";
				if (num > searchInfo.getUrlSize()) // 有前一页,打印前一页。
				{
					int prePage = 0;
					if (num % searchInfo.getUrlSize() != 0) {
						prePage = num - num % searchInfo.getUrlSize();
					} else {
						prePage = num - searchInfo.getUrlSize(); // 刚好在边界上,这时候前一页面就是当前页面-每个页面大小

					}
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ prePage
							+ ".html\" class=\"infoListFootLink\">&nbsp;前一页</a>";
				}
				// 有
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
							+ "&nbsp;下一页</a>";
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ totalPage
							+ ".html\" class=\"infoListFootLink\">&nbsp;&nbsp;&nbsp;最后一页</a>";
				}
			}

			// 存放html 字符串数据.

			StringBuffer tmpDirModel = new StringBuffer(dirModel.toString());
			boolean retainMark = false;

			// 1.替换标题

			String infoTitleStartMark = "<!--信息标题开始-->";
			String infoTitleEndMark = "<!--信息标题结束-->";
			String dirInfoTitle = t.getFullCatalogPath();
			tmpDirModel = findAndReplace(tmpDirModel, infoTitleStartMark,
					infoTitleEndMark, dirInfoTitle, retainMark);

			// 2.信息横向目录
			String infoPathStartMark = "<!--信息横向目录开始-->";
			String infoPathEndMark = "<!--信息横向目录结束-->";

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

			// 9.<!--信息目录ID开始-->547<!--信息目录ID结束-->替换

			String infoCatalogTreeIDStartMark = "<!--信息目录ID开始-->";
			String infoCatalogTreeIDEndMark = "<!--信息目录ID结束-->";
			retainMark = false;
			tmpDirModel = findAndReplace(tmpDirModel,
					infoCatalogTreeIDStartMark, infoCatalogTreeIDEndMark,
					currentInfoCatalogTree.getID() + "", retainMark);

			// 2.替换菜单
			retainMark = false;
			String infoMenuStartMark = "<!--菜单开始-->";
			String infoMenuEndMark = "<!--菜单结束-->";

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

			String infoContentStartMark = "<!--信息正文内容开始-->";
			String infoContentEndMark = "<!--信息正文内容结束-->";

			tmpDirModel = findAndReplace(tmpDirModel, infoContentStartMark,
					infoContentEndMark, tableContent, retainMark);

			// <!--分页显示链接结束-->

			String showByPageStartMark = "<!--分页显示链接开始-->";
			String showByPageEndMark = "<!--分页显示链接结束-->";

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
				// 这里可以改进，能不能不用String, 直接在二进制里就可以替换.
				try {
					FileOutputStream fout = new FileOutputStream(indexFileName);
					fout.write(tmpDirModel.toString().getBytes("GBK")); // 按照字符串的原始格式直接写到文件中去.
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
				fout.write(tmpDirModel.toString().getBytes("GBK")); // 按照字符串的原始格式直接写到文件中去.
				fout.close();
			} catch (IOException e) {
				System.out.println(e);
				errorInfo.append("\n" + e + "\n");
			}
		} // end of for

		return errorInfo.toString();
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
	 * 返回字符串的意义是要报告状态
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
	 * 获取指定目录下的信息ID,只获取一层.
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
	 * 从网站添加的信息中获取最新添加的信息
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
	 * 从网站添加的信息中获取最新添加的信息
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
	 * 获取指定目录下的信息ID,只获取一层.
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
	 * 检查目录和信息文件是否完整
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
		 * //简单化文件名称 String fileNameWithFullPath = fullPathDir +
		 * java.io.File.separator + + ctv.getCatalogTreeID() + "_"
		 * +currentInfoCatalogTree.getID() +"_" + infoID + ".html"; //也可以写在根目录下.
		 * boolean htmlCreateOK = false; try { FileOutputStream fout = new
		 * FileOutputStream(fileNameWithFullPath); byte[] buf = null; try {
		 * //=========== String temp = model.toString(); buf =
		 * model.toString().getBytes("GBK");//??"GBK" }catch(Exception ex) {
		 * System.out.println("文章信信息内容无法按照GBK编码来转化."+ex); } if(buf!=null) {
		 * //System.out.println("buf size="+buf.length+"字节"); fout.write(buf);
		 * //按照字符串的原始格式直接写到文件中去. fout.close(); //判断文件是否创建成功. File tmpF = new
		 * File(fileNameWithFullPath); if (tmpF.length() == buf.length) {
		 * htmlCreateOK = true; } else if (tmpF.length() > 0 && tmpF.length() <
		 * buf.length) { System.out.println("文件:" + fileNameWithFullPath +
		 * "生成失败,部分内容丢失. 原来文件大小" + buf.length + "字节,而生成后的大小是" + tmpF.length() +
		 * "字节."); } else if (tmpF.length() == 0) { System.out.println("文件:" +
		 * fileNameWithFullPath + "生成失败,文件大小是0字节."); } } else {
		 * System.out.println
		 * ("buf==null  把Model 写到文件时发生了错误.fileNameWithFullPath="
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
				// 1.获取当前目录下的所有infoID
				infoIDs = getInfoIDFromCatalog(ctv, tmp);
				if (infoIDs != null) {
					for (int i = 0; i < infoIDs.length; i++) {
						String fullPathDir = tmp.getSystemFullDirPath(ctv
								.getCatalogRootInSystemDir());
						String fileNameWithFullPath = fullPathDir
								+ java.io.File.separator
								+ +ctv.getCatalogTreeID() + "_" + tmp.getID()
								+ "_" + infoIDs[i] + ".html";

						// 检查是否存在并完整,如果坏了就重新建立.
						if (!checkHTMLFileStatus(fileNameWithFullPath)) // ?
						{
							System.out.println(fileNameWithFullPath
									+ " 文件坏掉,马上创建.");
							ihm.infoToHTMLForUserView(tmp, ctv, infoIDs[i],
									model);
							if (!checkHTMLFileStatus(fileNameWithFullPath)) {
								System.out.println(fileNameWithFullPath
										+ " 创建失败.");
							}
						}
					}
				}
			}

			if (checkDir) {
				// 2.更新目录列表
				// String errorReport = ihm.onlyDirToHTMLForUserView(tmp, ctv,
				// model,
				// dirModel);
				// errorInfo.append(errorReport);
			}

			Iterator<CatalogTree> itor = tmp.getSons().iterator();
			while (itor.hasNext()) {
				stack.push(itor.next());
			}

			System.out.println("目录ID:" + tmp.getID() + " 目录名称路径:"
					+ tmp.getFullCatalogPath() + " 目录路径:" + ctv.getURI()
					+ tmp.getVirtualFullWebDirPath() + "  已经更新完毕. 共有"
					+ ((infoIDs != null) ? infoIDs.length : 0) + "个信息");

		}
		return null;
	}

	/**
	 * 返回字符串的意义是要报告状态
	 * 
	 * @param ct
	 *            CatalogTree
	 * @param ctv
	 *            CatalogTreeView
	 * @param model
	 *            信息模板HTML
	 * @param dirModel
	 *            信息目录模板HTML
	 * @param updateInfo
	 *            是否更新信息
	 * @param updateDir
	 *            是否更新目录
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
				// 1.获取当前目录下的所有infoID
				infoIDs = getInfoIDFromCatalog(ctv, tmp);
				if (infoIDs != null) {
					for (int i = 0; i < infoIDs.length; i++) {
						infoToHTMLForUserView(tmp, ctv, infoIDs[i], model);
					}
				}
			}

			if (updateDir) {
				// 2.更新目录列表
				String errorReport = ihm.onlyDirToHTMLForUserView(tmp, ctv,
						model, dirModel);
				errorInfo.append(errorReport);
			}

			Iterator<CatalogTree> itor = tmp.getSons().iterator();
			while (itor.hasNext()) {
				stack.push(itor.next());
			}

			System.out.println("目录ID:" + tmp.getID() + " 目录名称路径:"
					+ tmp.getFullCatalogPath() + " 目录路径:" + ctv.getURI()
					+ tmp.getVirtualFullWebDirPath() + "  已经更新完毕. 共有"
					+ ((infoIDs != null) ? infoIDs.length : 0) + "个信息");

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