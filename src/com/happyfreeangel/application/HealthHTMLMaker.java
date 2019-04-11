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

	private static int maxKeywordExplainCount = 1;// 表示每篇文章关键词最多解释的次数.
	private static Hashtable<String, String> dictionary = new Hashtable<String, String>(); // IT字典,每隔1小时更新一次.
	private static int dictionaryFolderID = 379; // 字典树的ID号。字典是本棵树中的一个子目录，用户可以指定。
	private static Timestamp lastestUpdateTime = new Timestamp(
			System.currentTimeMillis());
	private static boolean debug = false; // 判断是否是调试模式,主要用于输出信息,判断错误在哪里.
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
	 * 把输入的字符串转换为Unicode
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
	 * 把一个信息放到指定的目录下,一个信息可以同时放到不同的目录下.
	 * 
	 * @param ctv
	 * @param ct
	 * @param info
	 * @param keywords
	 */
	public static void classifyInfo(CatalogTreeView ctv, CatalogTree ct,
			Info info, String keywords) {
		// 如果没有提示关键字,则把目录名称做为信息关键词,刚收集的未分类的信息是没有关键词的.
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
		infoID = infoID + 1; // 已经存在的发布的信息ID+1

		String updateSQL = "update Info set infoID=?, infoType = ?,keywords = ?,latestUpdateTime = ?,latestModifiedMemberID = ? where infoID = ? "; // and
		int robotID = 1000;// 机器人ID
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
				System.out.println("<div align=center>记录已经成功更新.时间:"
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
				System.out.println("<br>更新失败! updateSQL=" + updateSQL);

			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	/**
	 * 实现信息自动分类 后序遍历整个给定的树,
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

		// 通过标题判断是否合适,包含目录关键字看作是合适，否则不合适。
		CatalogTree dic = ct.getRoot().findByID(dictionaryFolderID); //

		if (info.getTitle().indexOf(ct.getName()) >= 0) {
			if (ct.isOffspring(dic)) // 信息标题就是目录名称是才允许放入词典目录
			{
				if (info.getTitle().equals(ct.getName())) {
					classifyInfo(ctv, ct, info, null);
					System.out.println("infoID=" + info.getInfoID() + " title="
							+ info.getTitle() + " 放到目录:"
							+ ct.getFullCatalogPath() + " 目录ID:" + ct.getID());
				}
			} else {
				classifyInfo(ctv, ct, info, null);
				System.out.println("infoID=" + info.getInfoID() + " title="
						+ info.getTitle() + " 放到目录:" + ct.getFullCatalogPath()
						+ " 目录ID:" + ct.getID());
			}
		}
		return;
	}

	/**
	 * 实现信息自动分类 后序遍历整个给定的树, 批量处理信息，快速完成.
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
		// 字典目录
		CatalogTree dic = ct.getRoot().findByID(dictionaryFolderID); //
		while (itor.hasNext()) {
			Info info = (Info) itor.next();
			// 通过标题判断是否合适,包含目录关键字看作是合适，否则不合适。
			if (info.getTitle().indexOf(ct.getName()) >= 0) {
				if (ct.isOffspring(dic)) // 信息标题就是目录名称是才允许放入词典目录
				{
					if (info.getTitle().equals(ct.getName())) {
						classifyInfo(ctv, ct, info, null);
						System.out.println("infoID=" + info.getInfoID()
								+ " title=" + info.getTitle() + " 放到目录:"
								+ ct.getFullCatalogPath() + " 目录ID:"
								+ ct.getID());
					}
				} else {
					classifyInfo(ctv, ct, info, null);
					System.out.println("infoID=" + info.getInfoID() + " title="
							+ info.getTitle() + " 放到目录:"
							+ ct.getFullCatalogPath() + " 目录ID:" + ct.getID());
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

		// 2009-07-01 shanghai happy addd
		// 字典树的目录ID
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
			System.out.println("没有什么需要更新的.");
			return;
		}

		// 一个个更新指定的目录.
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
			System.out.println("error! catalogTreeView 加载失败.");
			return;
		}

		if (catalogTreeView != null) {
			root = CatalogTree.loadFromDatabase(catalogTreeView);
		}
		if (root == null) {
			System.out.println("catalogTree 加载失败.");
			return;
		}

		// 屏蔽掉词典,词典必须人工放入,因为这影响到全局.
		dictionaryFolderID = Integer.parseInt(profile
				.getProperty("dictionaryFolderID"));
		debug = Boolean.parseBoolean(profile.getProperty("debug")); // true or
																	// false
		CatalogTree dic = root.findByID(dictionaryFolderID);
		CatalogTree father = (CatalogTree) dic.getFather();
		father.removeSon(dic);

		// 要分类多少信息
		int classifyInfoCount = 1;

		classifyInfoCount = Integer.parseInt(profile
				.getProperty("classifyInfoCount"));
		Info[] infos = getUnClassifyInfo(catalogTreeView, classifyInfoCount);// getLatestInfoFromWebSite(catalogTreeView,
																				// classifyInfoCount);

		System.out.println("classifyInfoCount=" + classifyInfoCount);
		if (infos != null) {
			System.out.println("获取的InfoCount=" + infos.length);
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

	public static void test() {

		Hashtable<String, String> dic = new Hashtable<String, String>();
		dic.put("网站设计",
				"http://www.541service.com/design/dictionary/100_28_150.html");
		dic.put("搜索引擎",
				"http://www.541service.com/design/dictionary/100_28_176.html");
		dic.put("标志设计",
				"http://www.541service.com/design/dictionary/100_28_131.html");
		dic.put("搜索引擎",
				"http://www.541service.com/design/dictionary/100_28_176.html");
		dic.put("VI设计",
				"http://www.541service.com/design/dictionary/100_28_129.html");
		dic.put("Dreamweaver",
				"http://www.541service.com/design/dictionary/100_28_111.html");
		dic.put("用户体验",
				"http://www.541service.com/design/dictionary/100_28_150.html");

		String content = "对于大多数网友来说，网站设计就是网页制作，通过搜索引擎，\n\r 很多学校都教网站设计，其中用到软件Dreamweaver。我们可以知道VI设计，用户体验是一种纯主观的在用户使用一个产品（服务）\n\r的过程中建立起来的心理感受。\n\r因为它是纯主观的，就带有一定的不确定因素。个体差异也决定了每个用户的真实体验是无法通过其他途径来完全模拟或再现的。\n\r但是对于一个界定明确的用户群体来讲，其用户体验的共性是能够经由良好设计的实验来认识到。";

		System.out.println(makeDictionaryLink(content, dic));
	}

	public static void test2() {
		String content = "<br><P>　　临床最常见的致聋性疾病包括慢性化脓性中耳炎及其后遗症(中耳粘连、鼓室硬化)、耳硬化症以及先天性耳畸形等。显微外科手术仍是最主要的治疗手段。近十年随着我国耳科学的发展和显微外科技术的提高，传导性耳聋的外科治疗水平有了长足的进步。"
				+ "<br><P>　　1保留或重建耳部结构与功能成为传导性聋外科治疗的主流"
				+ "<br><P>　　依据循证医学的准则根据不同病因和病变范围选择最佳的诊疗策略已逐渐成为耳外科临床医师的共识。基本原则为：彻底去除病变，最大限度地保留、修复或重建耳部解剖形态，恢复外耳、中耳的传音功能。"
				+ "<br><P>　　1.1慢性化脓性中耳炎"
				+ "<br><P>　　近十年来的一个显著变化是：传统的中耳乳突根治术逐渐为各种改良与成形术式所取代。开放式手术的同时施行鼓室或外耳道重建并缩小乳突腔，最大限度地保留中耳结构成为慢性化脓性中耳炎外科治疗的主流。许多大宗病例报告了同期进行听骨链重建的临床经验。对于中耳粘膜严重不可逆病变(粘膜化生、肥厚增生或鳞状上皮化)，主张分阶段手术，即分期鼓室成形，彻底清除不可逆组织(通常形成鼓室骨壁裸露)并置人硅胶膜(O.1～1.O mm)防止粘连，引导粘膜再生并恢复鼓室含气腔。6～1 2个月后二期探查并进行听骨链重建，此法对于咽鼓管功能尚存者的效果较好。保留外耳道后壁的闭合式手术仍为某些类型胆脂瘤中耳炎的治疗选择之一。郑军、李超仁、刘阳等报告了完壁式手术的治疗效果。鉴于较高的胆脂瘤复发率7.7%～ 24.O% ，主张严格掌握适应证， 即上鼓室病变、不伴有严重并发症及非硬化型乳突者宜选择施行， 以确保远期效果。对于多数慢性胆脂瘤型与骨疡型中耳乳突炎而言，如何选择术式仍无一致意见。孙建军报告了一种改进的术式，即保留完整骨桥的IBM 手术。关键技术是充分开放上鼓室与乳突，切除外耳道后壁，保留低位骨桥。适情况开放面神经隐窝，一期或分期鼓室成形。其特点是充分去除病灶，保持原有鼓室腔容积，重建中耳传声机构。IBM手术的干耳率及听力改善优于其它改良术式，为临床提供了又一合理的选择。IBM术式综合了开放式手术与闭合式手术的优点，有望成为胆脂瘤型与骨疡型中耳乳突炎外科治疗的主流术式。隐蔽性中耳炎是～ 类以临床症状不典型、鼓膜完整为特征的中耳炎<a href=\"http://www.541fuwu.com/health/dictionary/medicine/infectious_disease/social_disease/index.html\" class=dictionaryKeyword>性病</a>变。张全安报告了一组3O6例颞骨切片的观察结果，认为此病变为咽鼓管功能不良基础上伴发鼓室低度感染所致，本组中8.1%病例有不同程度的粘膜炎性改变，提醒临床医师进行详尽的听力学和影像学检查，以防误诊误治 。"
				+ "<br><P>　　1.2耳硬化症。临床性耳硬化症的有效治疗仍以手术为主。近十年在术式选择、激光显微技术与人工镫骨应用方面均有明显进步，治疗的效果也有提高。对于早期或轻度耳硬化症，曹钰霖报告了镫骨撼动并提升的术式，随访1～1 2年，近期有效率94.7%，远期86.8%。本术式优点：简便、微创、疗效稳定，可施行再次手术或因术中镫上结构损伤而改行底板手术。孙琴对两组病例进行比较，发现镫骨提升术与传统术式的疗效无显著差异”。激光底板开窗和活塞式人工镫骨的应用是这一时期的主要特点。王正敏报告了CO2激光镫骨底板开窗并植入活塞式人工镫骨的经验。44耳术后随访3个月～1年，气导听力提高31.2dB，此法优点为非接触、易控制和微创性。开窗适于底板增厚或阻塞型、足板浮动者 。新型Er-YAG激光也用于耳硬化症及鼓室硬化症听骨链病变手术，获得了良好效果。作者们报告了Co2激光和Er-YAG激光的最佳使用参数，以保证严格的穿透深度，最小的热损伤和机械袭扰。"
				+ "<br><P>　　1.3先天性耳畸形。随着高分辨CT影像和导航技术的应用，对各种先天耳畸形术前的诊断水平明显提高，术中面神经损伤率有所下降。同步听力重建与耳廓成形已在多家医院开展。汪吉宝报告了53例前上径路的直入式技术，除1耳外，52耳均顺利探入鼓室。此法适用于硬化型或松质，随访1～9年，听力恢复较满意，冷同嘉、韩东一等报告术后听力改善的效果明显提高。戴海江报告89例(99耳)经鼓室投影区探查中耳均获成功，此入路有安全、省时、易于掌握的特点。"
				+ "<br><P align=right>（实习编辑：王俏茹）";

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

		int startPos = line.indexOf(startMark, 0);
		int endPos = line.indexOf(endMark, keywordPos);

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
	 *            词典目录树
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
						.println("系统发生了异常.com.happyfreeangel.magictree.application.HealthHTMLMaker  public static Hashtable getITDictionay(CatalogTree ct,CatalogTreeView ctv)");
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
					 dictionary.put(title,ctv.getURI() + htmlURL); // 如果有重复,则后来者优先.
					// if (chongtu != null)
					// {
					// System.out.println(" 有相同的名词术语: " + title + "   " +
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
		// 初始化keywords列表

		keywords = getDictionaryKeys();
		return dictionary;
	}

	/**
	 * 对词典按照关键词的长短来排序.
	 * 
	 * @return
	 */
	public static String[] getDictionaryKeys() {
		if (keywords != null) {
			return keywords;
		}
		Object[] tmpitems = dictionary.keySet().toArray();
		keywords = new String[tmpitems.length];

		// 把items字符串数组根据字符串长度来排序,长的优先排在前面,如果2个单词等长,就随便取一个,
		for (int p = 0; p < keywords.length; p++) {
			keywords[p] = (String) tmpitems[p];
		}
		// 冒泡法排序
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
	 * 判断输入的字符是单字节的英文或数字
	 */
	public static boolean isNumOrABC(char c) {
		if (c > 255) // 非半角字.
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
	 * 判断一个英语单词在句子里是否边界清楚，没有歧义。 A dog is black. Adog is black.
	 * 
	 * @param line
	 * @param keyword
	 * @return
	 */
	public static boolean isEnglishWordBoardClear(String line, String keyword) {
		// 1.先判断是否是英语单词，如果不是则马上退出.
		if (!isEnglishKeyword(keyword)) {
			return false;
		}
		boolean isOK = false;
		int from = line.indexOf(keyword);
		int to = from + keyword.length();

		// 确定边界是否清楚。

		// 1.关键词在一行开头.
		if (from == 0) {
			// a.一行就一个关键词.
			if (line.length() == keyword.length()) {
				isOK = true;
			}
			// b.关键词后面是空格或汉字 也算独立
			if (line.length() > keyword.length()) {
				if (!isNumOrABC(line.charAt(keyword.length()))
						|| isHanZi(line.charAt(keyword.length()))) {
					isOK = true;
				}
			}
		}
		// 2.关键词在一行中间.
		else if (from > 0 && to < (line.length() - 1)) {
			if ((!isNumOrABC(line.charAt(from - 1)) || isHanZi(line
					.charAt(from - 1)))
					&& (!isNumOrABC(line.charAt(to)) || isHanZi(line.charAt(to)))) {
				isOK = true;
			}
		}
		// 3.关键词在一行末尾
		else if (from > 0 && to == (line.length() - 1)) {
			if (!isNumOrABC(line.charAt(from - 1))
					|| isHanZi(line.charAt(from - 1))) {
				isOK = true;
			}
		}
		return isOK;
	}

	/***
	 * 对指定的文本做关键词Web链接
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
		// 记录关键词是否已经访问过,一一对应
		// keywords[]
		// visitedkeywords[]
		int[] visitedKeyword = new int[keywords.length];
		for (int i = 0; i < visitedKeyword.length; i++) {
			visitedKeyword[i] = 0; // 0表示尚未成功解释过，非0正整数表示已经成功访问的次数.
		}
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
				
				//使用了编辑器就不能这样简单的添加,必须忽略
//				if (tmpLine.indexOf("<br>") < 0) {
//					line = "<br>" + line;
//				}
			}

			// ??需要完善,更精确一点. 对每一行文本，检查是否包含keywords数组里的关键词，且符合设定的标准。
			for (int h = 0; h < keywords.length; h++) {
				int keywordPos = isKeywordInMark(line, keywords[h],
						"<a href=\"", "</a>");
				if (keywordPos <= 0) {
					continue;
				}

				// 如果本行确实包含有关键词,除了是英语关键词边界不清楚的以外，都要注释。
				// 1.如果是英语关键词,且边界清楚.
				if (isEnglishKeyword(keywords[h])
						&& (!isEnglishWordBoardClear(line, keywords[h]))) {
					// for debug
					System.out
							.println("!!!!!!!!!放弃，英语关键词，边界不清楚，不能做解释，否则用户看不清楚. keyword="
									+ keywords[h] + " line=" + line);
				} else // 2.如果是汉语关键词
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
						visitedKeyword[h]++; // 记录成功的行为,不断的增加.
												// //每个关键词再每个页面只做maxKeywordExplainCount次解释.
					}
				}
			}

			contentBuf.append(line + "\n");
		} // end of while

		return contentBuf.toString();
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

		ctv.getCatalogTreeDefinitionTableName();
		ctv.getCatalogTreeTableName();

		String SQL = " select Info.infoID,title,content,author,publishedDate,expiredDate,keywords,derivation,sourceURI,whoCollectIt,latestUpdateTime,insertedTime,viewedCount,latestModifiedMemberID,hasVerify,activeStatus,securityLevel,isAuthorship from Info "
				+ " where Info.infoType = "
				+ currentInfoCatalogTree.getID()
				+ " and Info.infoID=" + infoID;

		Statement stmt = null;
		ResultSet rs = null;

		boolean hasFound = false;

		// 获取IT字典. folderID = 字典的目录ID
		Hashtable<String, String> webDesignDictionary = getDictionay(
				currentInfoCatalogTree.getRoot().findByID(dictionaryFolderID),
				ctv);
		if (debug) {
			System.out.println("\n词典列表如下: dictionaryFolderID="
					+ dictionaryFolderID + "\n" + webDesignDictionary);
		}

		try {
			stmt = ctv.getPool().createStatement();
			rs = stmt.executeQuery(SQL);

			if (rs != null && rs.next()) {
				infoID = rs.getInt("infoID");
				title = rs.getString("title");
				content = rs.getString("content");

				// 对符合标准的文本进行解释，做超链接
				content = makeDictionaryLink(content, webDesignDictionary);
				//content = "<br>&nbsp;&nbsp;" + content; // 内容第一段空2个字符开始.

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
			
			
			

			// 4. 信息关键字链接

			String infoKeywordLinkValueStartMark = "<!--信息关键字开始-->";
			String infoKeywordLinkValueEndMark = "<!--信息关键字结束-->";

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
			
			
			// 4. 信息关键字 在标题meta keywords 里显示

			String infoKeywordValueStartMark = "<!--信息meta关键字开始-->";
			String infoKeywordValueEndMark = "<!--信息meta关键字结束-->";
			String 	keywordContent = (keywords==null) ? "" : keywords;
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

			// 如果没有设置发布时间,则以这个当前时刻为标准设置为发布时间。 2009-10-12 开心修改
			if (publishedDate == null) {
				publishedDate = new Timestamp(System.currentTimeMillis());
				// 记录更新数据库这个记录

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
				String delim = ", ，";//全角的逗号和半角的逗号都可以，还有空格.
				java.util.StringTokenizer st = new StringTokenizer(keywords,
						delim);
				while (st.hasMoreTokens()) {
					keywordCondition += "(Info.keywords like '%"
							+ st.nextToken() + "%') or ";
				}
			}

			keywordCondition += " (1>2) "; // 假条件,充数的.
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
				int maxShowCount = 10; // 最多显示的数目.
				Timestamp tmpPublishedDate = null;
				String tmpTitle = null, tmpVirtualWebPath = null;

				//选取算法...
				
				List<VirtualWebInfo> relativeWebInfoList=new LinkedList<VirtualWebInfo>();
				
				while (tmpRS.next()) {
					tmpInfoID = tmpRS.getInt("infoID");
					tmpTitle = tmpRS.getString("title");
					tmpVirtualWebPath = tmpRS.getString("virtualWebPath");
					tmpPublishedDate = tmpRS.getTimestamp("publishedDate");

					VirtualWebInfo webInfo=new VirtualWebInfo(tmpInfoID,tmpTitle,tmpVirtualWebPath,tmpPublishedDate);
					relativeWebInfoList.add(webInfo); //从小到大排序 第一个是最旧的.
				}
				
				List<VirtualWebInfo> selectedWebInfoList=getRelativeList(relativeWebInfoList,maxShowCount);
				Collections.sort(selectedWebInfoList);
				while(selectedWebInfoList.size()>0){
					VirtualWebInfo webInfo=selectedWebInfoList.remove(0);
				    xuhao++;

					// 往表格里添加一条记录.
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
									+ "&actionType=requery&pageNumber=1\"\" class=\"relativeLink\">查看所有相关的信息...</a></td>");
					relativeLinks.append(lineSeparator + "       </tr>");
				}

				relativeLinks.append(lineSeparator + "          </table>");

				if (relativeLinks.indexOf(ctv.getURI()) < 0) {
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
				PreparedStatement pstmt = null, pstmt2 = null, pstmt3 = null;
				
				try {
					String sql = " delete from InfoCatalogTreeHTMLWebPath where catalogTreeID = ? and folderID= ? and infoID=?";
					String insertSQL = " insert into InfoCatalogTreeHTMLWebPath(catalogTreeID,folderID,infoID,virtualWebPath) values(?,?,?,?) ";
					// 记录这条信息已经发布,更改发布状态，记录发布时间.
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
		//不足或刚好，全部选中,不需要概率
		if(relativeWebInfoList.size()<=maxShowCount) {
			webInfoList.addAll(relativeWebInfoList);
			return webInfoList;
		}

		for(int count=0;count<maxShowCount;count++){
			VirtualWebInfo webInfo=relativeWebInfoList.get(0);
			if(webInfo.getInsertTime().after(theDayBeforeYesterday)){
				webInfoList.add(webInfo); //100% 添加3天之内的资料
				continue;
			}
			int whichOne=(int)(relativeWebInfoList.size()*Math.random());
			webInfoList.add(relativeWebInfoList.remove(whichOne)); //100% 添加3天之内的资料
			continue;
		}
		return webInfoList;
	}

	
	
	
	/**
	 * 把一个信息转化为HTML格式.
	 * 
	 * @param t
	 *            CatalogTree
	 * @param Info
	 *            info 2009.7.18 上海 这样的速度估计会很快，待测试确定.
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
				// h.setBordercolordark("#ffffff");
				// h.setBordercolor("#888888");
				// h.setPixelsPerByteShow(2.6);
				h.setClassIDOfCSS("dirContentTable");
				h.hideColumn("信息类型ID");
				h.hideColumn("信息类型");
				h.hideColumn("作者");
				h.hideColumn("格式");
				h.hideColumn("信息ID");
				h.hideColumn("语言类型");
				h.hideColumn("编辑");
				h.hideColumn("信息类型"); // 因为文章目录结构里已经非常清楚了，没有必要显示这个冗余的信息.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("发布日期", "year-month%2-date%2");
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
				System.out.println(fileNameWithFullPath + " 创建成功. 时间:"
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
				+ " and " + ctv.getCatalogTreeTableName()
				+ ".infoPublishStatus=1 " + // 表示已经发布的信息
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
				// h.setBordercolordark("#ffffff");
				// h.setBordercolor("#888888");
				// h.setPixelsPerByteShow(2.6);
				h.setClassIDOfCSS("dirContentTable");
				h.hideColumn("信息类型ID");
				h.hideColumn("信息类型");
				h.hideColumn("作者");
				h.hideColumn("格式");
				h.hideColumn("信息ID");
				h.hideColumn("语言类型");
				h.hideColumn("编辑");
				h.hideColumn("信息类型"); // 因为文章目录结构里已经非常清楚了，没有必要显示这个冗余的信息.
										// 2007-03-04 23:47 shanghai Happy
				h.setColumnDataFormat("发布日期", "year-month%2-date%2");
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

				// 需要加强.
				urls = "";

				int num = currentShowingPage;
				int totalPage = searchInfo.getTotalPagesNum();
				int base = (int) ((num - 1) / searchInfo.getUrlSize());
				int first = searchInfo.getUrlSize() * base;

				urls = "<font size=\"-1\">当前页:&nbsp;第" + currentShowingPage
						+ "页</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				if (currentShowingPage > searchInfo.getUrlSize()) //
				{
					urls += "<a href=\""
							+ ctv.getURI()
							+ currentInfoCatalogTree.getVirtualFullWebDirPath()
							+ "/"
							+ "1.html\" class=\"infoListFootLink\">&nbsp;第一页</a>&nbsp;&nbsp;";
				}
				
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

	/***
	 * 从目录中获取完整的信息
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

		// infoPublishStatus=表示待发布的 infoPublishStatus=1表示已经发布
		// infoPublishStatus=2表示其他意思，未定义。
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
		// System.out.println("从数据库读取"+infos.length+"信息完整记录，耗费"+timeCostInSecond+"秒。平均每秒读取条"+((infos.length*1.0)/timeCostInSecond)+"记录.");
		return infos;
	}

	/**
	 * 从网站添加的信息中获取最受欢迎的信息---访问量最大的信息
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
	 * 从网站添加的信息中获取最新添加的信息,根据添加的时间按最近--》最早排序
	 * 
	 * @param ctv
	 *            信息量很少，ID，标题，Html链接地址
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
	 * 从数据库中获取未分类的信息,根据添加的时间按最近--》最早排序
	 * 
	 * @param ctv
	 *            信息量很少，ID，标题，Html链接地址
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
		Vector<Info> tmpV = new Vector<Info>();
		String sql = " select InfoCatalogTreeHTMLWebPath.virtualWebPath,Info.infoID,Info.title,Info.publishedDate from InfoCatalogTreeHTMLWebPath,Info where InfoCatalogTreeHTMLWebPath.catalogTreeID= "
				+ ctv.getCatalogTreeID()
				+ " and InfoCatalogTreeHTMLWebPath.infoID = Info.infoID and Info.title like '%"
				+ keyword
				+ "%'  order by InfoID desc limit "
				+ returnCount
				+ ";";

		System.out.println("keyword=" + keyword);
		// latestInfo 是特殊关键词.
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
	 * 检查目录和信息文件是否完整
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

			int howMany = 0; // 具体要更新的数量
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

				// 更新信息
				if (updateInfo) {
					infoToHTMLForUserView(currentInfoCatalogTree, ctv,
							whichInfoID.intValue(), model);
					System.out.println("信息ID:" + whichInfoID + "已经成功更新. 目录:"
							+ currentInfoCatalogTree.getFullCatalogPath());
				}
				// 更新目录.
				if (updateDir) {
					onlyDirToHTMLForUserView(currentInfoCatalogTree, ctv,
							model, dirModel);
				}
			}

			if (howMany > 0) {
				System.out.println("已经成功发布" + howMany + "条新信息.");
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
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
				// 2.更新目录列表
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
					.println("目录ID:"
							+ currentCatalogTree.getID()
							+ " 目录名称路径:"
							+ currentCatalogTree.getFullCatalogPath()
							+ " 目录路径:"
							+ ctv.getURI()
							+ currentCatalogTree.getVirtualFullWebDirPath()
							+ "  已经更新完毕. 共有"
							+ infoCount
							+ "个信息, 目录"
							+ dirSize
							+ "个. 总共花费时间:"
							+ timeCostInSecond
							+ "秒"
							+ (timeCostInSecond > 0 ? "平均每秒更新记录"
									+ ((infoCount * 1.0) / (timeCostInSecond + 0.00001))
									+ "个"
									: ""));
		}
		long totalEndTime = System.currentTimeMillis();
		long totalTimeCostInSecond = totalEndTime - totalStartTime;
		System.out.println("\n恭喜了。本次信息更新计划已经成功进行. 共有" + totalCount
				+ "个信息被更新 花费时间:" + totalTimeCostInSecond + "秒 平均每秒更新记录"
				+ ((totalCount * 1.0) / totalTimeCostInSecond) + "个");
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