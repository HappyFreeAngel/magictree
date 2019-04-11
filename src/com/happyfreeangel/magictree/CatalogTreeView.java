package com.happyfreeangel.magictree;

/**
 * <p>Title: 魔树</p>
 * <p>Description: 魔树</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author happy
 * @version 0.1
 */

import java.io.*;

import java.sql.*;

import net.snapbug.util.dbtool.*;

public class CatalogTreeView implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CatalogTreeView() {
	}

	private long catalogTreeID; // 目录树的ID
	private String name; // 这个目录树的名字, 不是根的名字.
	private String catalogTreeDefinitionTableName; // 目录ID 对应的表名,假设在同一个数据库中.
	private String catalogTreeTableName; // web目录, 记录哪个网站放在哪个目录下.

	private String description; // 关于这个目录的描述.
	private String beanFileNameAndPath; // 保存为JavaBean时的路径和名字.
	private String XMLFileNameAndPath; // 保存为XML文件格式的文件路径和名字.
	private String javascriptFileNameAndPath; // JavaScript
	private String URI; // html文件夹对应的互联网路径 http://dir.541help.com/html
	private String catalogRootInSystemDir; // 在当前服务器上的目录的实际路径

	// 2009.07.02 happy add shanghai
	private StringBuffer infoModel; // 信息页面的HTML模版 StringBuffer 格式
	private StringBuffer dirModel;

	private String infoModelHTMLFilePath; //
	private String dirModelHTMLFilePath; //
	// ////////////////////////

	private ConnectionPool pool = null; // 数据库连接池.

	/**
	 * 在当前数据库创建3张表结构. 1. prefixCatalogTreeView, 2. prefixCatalogTree 3.
	 * prefixCatalogTreeDefinition
	 * 
	 * @param pool
	 * @param prefix
	 * @return
	 */
	public static boolean initCatalogTreeTables(ConnectionPool pool,
			String prefix) {
		if (pool == null) {
			System.out.println("数据库连接池是空的,无法连接.");
		}
		// test if tables has exits. if exits, then exit.
		String createTableSQLOfCatalogTreeView = "\n create table  "
				+ prefix
				+ "CatalogTreeView \n"
				+ " (    \n"
				+ "          catalogTreeID                   int not null primary key, #目录ID  \n"
				+ "          name                            varchar(255) not null,    #目录树名称 \n"
				+ "          catalogTreeDefinitionTableName  varchar(255) not null,    #目录定义表的名称\n"
				+ "          catalogTreeTableName            varchar(255) not null,    #目录存放表的名称\n"
				+ "          URI                             varchar(255),             #网站的网址\n"
				+ "          description                     text,                     #描述\n"
				+ "          beanFileNameAndPath             varchar(255),             #Java Bean 的文件全路径\n"
				+ "          XMLFileNameAndPath              varchar(255),             #XML 文件的全路径\n"
				+ "          javascriptFileNameAndPath       varchar(255),             #JavaScript 文件的全路径\n"
				+ "          catalogRootInSystemDir          varchar(255)              #静态网页在服务器上的路径\n"
				+ " ); \n";

		String createTableSQLofCatalogTree = "\n create table " + prefix
				+ "CatalogTree   \n" + "(  \n"
				+ "  catalogTreeID   int not null,#目录树ID  \n"
				+ "  folderID        int not null,            #目录ID   \n"
				+ "  infoID          int not null,             #信息ID   \n"
				+ "  primary key(catalogTreeID,folderID,infoID) #信息ID   \n"
				+ "); \n";

		String createTableSQLOfCatalogDefinition = "\n create table  "
				+ prefix
				+ "CatalogTreeDefinition "
				+ "(    \n"
				+ "  catalogTreeID  int not null,# 目录树的ID  \n"
				+ "  fatherID       int not null,# 当前目录的父亲的ID  \n"
				+ "  id             int not null,# 当前目录的ID   \n"
				+ "  name           varchar(255) not null,#目录名称  \n"
				+ "  folder         varchar(255) not null,#文件夹名称  \n"
				+ "  attribute      int          not null, #目录属性 只读,隐藏,特权位  \n"
				+ "  unique(catalogTreeID,fatherID,name),  #必须保证在同一棵树下,相同的父目录下没有相同的名称       \n"
				+ "  unique(catalogTreeID,fatherID,folder),#必须保证在同一棵树下,相同的父目录下没有相同的文件夹名称 \n"
				+ "  primary key(catalogTreeID,fatherID,id)   #定义主键  \n"
				+ ");\n";

		int failedCount = 0;
		int affectedRows = 0;
		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt
					.executeUpdate(createTableSQLOfCatalogDefinition);
			System.out.println(createTableSQLOfCatalogDefinition + "  创建成功.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLOfCatalogDefinition + "  创建失败.");
		}

		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt.executeUpdate(createTableSQLofCatalogTree);
			System.out.println(createTableSQLofCatalogTree + "  创建成功.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLofCatalogTree + "  创建失败.");
		}

		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt.executeUpdate(createTableSQLOfCatalogTreeView);
			System.out.println(createTableSQLOfCatalogTreeView + "  创建成功.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLOfCatalogTreeView + "  创建失败.");
		}

		if (failedCount == 0) {
			System.out.println("所有的表创建成功.");
		} else {
			System.out.println(failedCount + " 表创建失败.");
		}

		return (failedCount == 0);
	}

	public boolean initCatalogTreeView(String prefix) {
		String insertSQL = " insert into "
				+ prefix
				+ "CatalogTreeView(catalogTreeID,name,catalogTreeDefinitionTableName,catalogTreeTableName,URI,description,beanFileNameAndPath,XMLFileNameAndPath,javascriptFileNameAndPath,catalogRootInSystemDir) values(?,?,?,?,?,?,?,?,?,?) ";
		try {
			PreparedStatement pstmt = pool.prepareStatement(insertSQL);
			pstmt.setLong(1, catalogTreeID);
			pstmt.setString(2, name);
			pstmt.setString(3, prefix + "CatalogTreeDefinition");
			pstmt.setString(4, prefix + "CatalogTree");
			pstmt.setString(5, URI);
			pstmt.setString(6, description);
			pstmt.setString(7, beanFileNameAndPath);
			pstmt.setString(8, XMLFileNameAndPath);
			pstmt.setString(9, javascriptFileNameAndPath);
			pstmt.setString(10, catalogRootInSystemDir);
			pstmt.executeUpdate();
			pstmt.close();
			return true;
		} catch (SQLException e) {
			System.out.println(e);
		}
		return false;
	}

	/**
	 * 从数据库中加载.
	 * 
	 * @param pool
	 *            数据库连接池.
	 * @param catalogID
	 *            目录树的ID
	 * @param catalogTreeViewName
	 *            目录树视图的表名.
	 * @return
	 */
	public static CatalogTreeView loadFromDatabase(ConnectionPool pool,
			long catalogTreeID, String catalogTreeViewTableName) {
		CatalogTreeView catalogTreeView = null;

		String name = null;
		String catalogTreeDefinitionTableName = null;
		String catalogTreeTableName = null;
		String URI = null;
		String description = null;
		String beanFileNameAndPath = null;
		String XMLFileNameAndPath = null;
		String javascriptFileNameAndPath = null;
		String catalogRootInSystemDir = null;

		String querySQL = " select catalogTreeID,name,catalogTreeDefinitionTableName,catalogTreeTableName,URI,description,beanFileNameAndPath,XMLFileNameAndPath,javascriptFileNameAndPath,catalogRootInSystemDir from "
				+ catalogTreeViewTableName
				+ " where catalogTreeID = '"
				+ catalogTreeID + "' ";
		try {
			Statement stmt = pool.createStatement();
			ResultSet rs = stmt.executeQuery(querySQL);
			if (rs != null && rs.next()) {
				catalogTreeID = rs.getInt("catalogTreeID");
				name = rs.getString("name");
				catalogTreeDefinitionTableName = rs
						.getString("catalogTreeDefinitionTableName");
				catalogTreeTableName = rs.getString("catalogTreeTableName");
				URI = rs.getString("URI");
				description = rs.getString("description");
				beanFileNameAndPath = rs.getString("beanFileNameAndPath");
				XMLFileNameAndPath = rs.getString("XMLFileNameAndPath");
				javascriptFileNameAndPath = rs
						.getString("javascriptFileNameAndPath");
				catalogRootInSystemDir = rs.getString("catalogRootInSystemDir");

				catalogTreeView = new CatalogTreeView();
				catalogTreeView.setPool(pool); // 后面将直接访问这个数据库连接池

				catalogTreeView.setCatalogTreeID(catalogTreeID);
				catalogTreeView.setName(name);
				catalogTreeView
						.setCatalogTreeDefinitionTableName(catalogTreeDefinitionTableName);
				catalogTreeView.setCatalogTreeTableName(catalogTreeTableName);
				catalogTreeView.setURI(URI);
				catalogTreeView.setDescription(description);
				catalogTreeView.setBeanFileNameAndPath(beanFileNameAndPath);
				catalogTreeView.setXMLFileNameAndPath(XMLFileNameAndPath);
				// catalogTreeView.setJavascriptFileNameAndPath(javascriptFileNameAndPath);
				catalogTreeView
						.setCatalogRootInSystemDir(catalogRootInSystemDir);
			}
			if (rs != null) {
				rs.close();
			}
			stmt.close();
		} catch (SQLException e) {
			System.out.println(e);
		}

		if (catalogTreeView == null) {
			System.out.println("  加载:" + catalogTreeViewTableName + " 失败. ");
		}
		return catalogTreeView;
	}

	/**
	 * 从网页目录中删除这个制定的网站
	 * 
	 * @return
	 */
	public boolean deleteFromCatalogTree(int folderID, int webID) {
		return false;
	}

	/**
	 * 从网页目录中删除这个制定的网站
	 * 
	 * @param folderID
	 * @param webID
	 * @return
	 */
	public boolean insertIntoCatalogTree(int folderID, int webID) {
		// insert into Catalog Tree
		// insert into database
		// generate HTML in file System.
		return false;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	}

	public String getBeanFileNameAndPath() {
		return beanFileNameAndPath;
	}

	public void setBeanFileNameAndPath(String beanFileNameAndPath) {
		this.beanFileNameAndPath = beanFileNameAndPath;
	}

	public long getCatalogTreeID() {
		return catalogTreeID;
	}
 
	public void setCatalogTreeID(long catalogTreeID) {
		this.catalogTreeID = catalogTreeID;
	}

	public String getCatalogRootInSystemDir() {
		return catalogRootInSystemDir;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public ConnectionPool getPool() {
		return pool;
	}

	public String getURI() {
		return URI;
	}

	public String getXMLFileNameAndPath() {
		return XMLFileNameAndPath;
	}

	public void setXMLFileNameAndPath(String XMLFileNameAndPath) {
		this.XMLFileNameAndPath = XMLFileNameAndPath;
	}

	public void setURI(String URI) {
		this.URI = URI;
	}

	public void setPool(ConnectionPool pool) {
		this.pool = pool;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setCatalogRootInSystemDir(String catalogRootInSystemDir) {
		this.catalogRootInSystemDir = catalogRootInSystemDir;
	}

	public String getCatalogTreeDefinitionTableName() {
		return catalogTreeDefinitionTableName;
	}

	public void setCatalogTreeDefinitionTableName(
			String catalogTreeDefinitionTableName) {
		this.catalogTreeDefinitionTableName = catalogTreeDefinitionTableName;
	}

	public String getCatalogTreeTableName() {
		return catalogTreeTableName;
	}

	public void setCatalogTreeTableName(String catalogTreeTableName) {
		this.catalogTreeTableName = catalogTreeTableName;
	}

	public String getJavascriptFileNameAndPath() {
		return javascriptFileNameAndPath;
	}

	public void setJavascriptFileNameAndPath(String javascriptFileNameAndPath) {
		this.javascriptFileNameAndPath = javascriptFileNameAndPath;
	}

	// 待完善.
	public String toString() {
		return this.name + this.URI + this.catalogTreeID;
	}

	public static void init(String propertiesFilenameWithFullPath) {
		/**
	   * 
	   */
	}

	public static void main(String[] args) {

		CatalogTreeView catalogTreeView1 = new CatalogTreeView();
		CatalogTree root = null;

		// 从数据库中加载数据.
		ConnectionPool pool = null;
		String dbConf = "F:\\Tomcat5.5\\webapps\\www.541help.com\\WEB-INF\\DatabasePool.conf";
		String poolName = "info_541help_com";
		Statement stmt = null;
		Connection con = null;
		try {
			ConnectionManager.init(dbConf);

			pool = ConnectionManager.getConnectionPool(poolName);

			if (pool == null) {
				System.out.println("pool is null");
				return;
			}
			stmt = pool.createStatement();
		} catch (Exception e) {
			System.out.println(e);
		}

		// 创建一个新的目录类型及其相关的表格.
		String prefix = "Web";

		boolean ok = CatalogTreeView.initCatalogTreeTables(pool, prefix);
		CatalogTreeView ctv = new CatalogTreeView();
		ctv.setPool(pool);
		ctv.setCatalogTreeID(100);
		ctv.setName("全球网站分类目录");
		ctv.initCatalogTreeView(prefix);

		long dir_541help_com = 100;

		String catalogTreeViewName = prefix + "CatalogTreeView";

		ctv = CatalogTreeView.loadFromDatabase(pool, 100, catalogTreeViewName);

		CatalogTree ct = null;// ctv.getCatalogTree(dir_541help_com);

		if (ct == null) {
			ct = CatalogTree.createCatalog(null, "全球网站分类目录",
					"globalWebClassify", 0, ctv);
			System.out.println("根目录创建成功.");
		}

		if (ct != null) {
			System.out.println(ct);
		}
	}

}
