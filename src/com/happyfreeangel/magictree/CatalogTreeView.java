package com.happyfreeangel.magictree;

/**
 * <p>Title: ħ��</p>
 * <p>Description: ħ��</p>
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

	private long catalogTreeID; // Ŀ¼����ID
	private String name; // ���Ŀ¼��������, ���Ǹ�������.
	private String catalogTreeDefinitionTableName; // Ŀ¼ID ��Ӧ�ı���,������ͬһ�����ݿ���.
	private String catalogTreeTableName; // webĿ¼, ��¼�ĸ���վ�����ĸ�Ŀ¼��.

	private String description; // �������Ŀ¼������.
	private String beanFileNameAndPath; // ����ΪJavaBeanʱ��·��������.
	private String XMLFileNameAndPath; // ����ΪXML�ļ���ʽ���ļ�·��������.
	private String javascriptFileNameAndPath; // JavaScript
	private String URI; // html�ļ��ж�Ӧ�Ļ�����·�� http://dir.541help.com/html
	private String catalogRootInSystemDir; // �ڵ�ǰ�������ϵ�Ŀ¼��ʵ��·��

	// 2009.07.02 happy add shanghai
	private StringBuffer infoModel; // ��Ϣҳ���HTMLģ�� StringBuffer ��ʽ
	private StringBuffer dirModel;

	private String infoModelHTMLFilePath; //
	private String dirModelHTMLFilePath; //
	// ////////////////////////

	private ConnectionPool pool = null; // ���ݿ����ӳ�.

	/**
	 * �ڵ�ǰ���ݿⴴ��3�ű�ṹ. 1. prefixCatalogTreeView, 2. prefixCatalogTree 3.
	 * prefixCatalogTreeDefinition
	 * 
	 * @param pool
	 * @param prefix
	 * @return
	 */
	public static boolean initCatalogTreeTables(ConnectionPool pool,
			String prefix) {
		if (pool == null) {
			System.out.println("���ݿ����ӳ��ǿյ�,�޷�����.");
		}
		// test if tables has exits. if exits, then exit.
		String createTableSQLOfCatalogTreeView = "\n create table  "
				+ prefix
				+ "CatalogTreeView \n"
				+ " (    \n"
				+ "          catalogTreeID                   int not null primary key, #Ŀ¼ID  \n"
				+ "          name                            varchar(255) not null,    #Ŀ¼������ \n"
				+ "          catalogTreeDefinitionTableName  varchar(255) not null,    #Ŀ¼����������\n"
				+ "          catalogTreeTableName            varchar(255) not null,    #Ŀ¼��ű������\n"
				+ "          URI                             varchar(255),             #��վ����ַ\n"
				+ "          description                     text,                     #����\n"
				+ "          beanFileNameAndPath             varchar(255),             #Java Bean ���ļ�ȫ·��\n"
				+ "          XMLFileNameAndPath              varchar(255),             #XML �ļ���ȫ·��\n"
				+ "          javascriptFileNameAndPath       varchar(255),             #JavaScript �ļ���ȫ·��\n"
				+ "          catalogRootInSystemDir          varchar(255)              #��̬��ҳ�ڷ������ϵ�·��\n"
				+ " ); \n";

		String createTableSQLofCatalogTree = "\n create table " + prefix
				+ "CatalogTree   \n" + "(  \n"
				+ "  catalogTreeID   int not null,#Ŀ¼��ID  \n"
				+ "  folderID        int not null,            #Ŀ¼ID   \n"
				+ "  infoID          int not null,             #��ϢID   \n"
				+ "  primary key(catalogTreeID,folderID,infoID) #��ϢID   \n"
				+ "); \n";

		String createTableSQLOfCatalogDefinition = "\n create table  "
				+ prefix
				+ "CatalogTreeDefinition "
				+ "(    \n"
				+ "  catalogTreeID  int not null,# Ŀ¼����ID  \n"
				+ "  fatherID       int not null,# ��ǰĿ¼�ĸ��׵�ID  \n"
				+ "  id             int not null,# ��ǰĿ¼��ID   \n"
				+ "  name           varchar(255) not null,#Ŀ¼����  \n"
				+ "  folder         varchar(255) not null,#�ļ�������  \n"
				+ "  attribute      int          not null, #Ŀ¼���� ֻ��,����,��Ȩλ  \n"
				+ "  unique(catalogTreeID,fatherID,name),  #���뱣֤��ͬһ������,��ͬ�ĸ�Ŀ¼��û����ͬ������       \n"
				+ "  unique(catalogTreeID,fatherID,folder),#���뱣֤��ͬһ������,��ͬ�ĸ�Ŀ¼��û����ͬ���ļ������� \n"
				+ "  primary key(catalogTreeID,fatherID,id)   #��������  \n"
				+ ");\n";

		int failedCount = 0;
		int affectedRows = 0;
		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt
					.executeUpdate(createTableSQLOfCatalogDefinition);
			System.out.println(createTableSQLOfCatalogDefinition + "  �����ɹ�.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLOfCatalogDefinition + "  ����ʧ��.");
		}

		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt.executeUpdate(createTableSQLofCatalogTree);
			System.out.println(createTableSQLofCatalogTree + "  �����ɹ�.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLofCatalogTree + "  ����ʧ��.");
		}

		try {
			Statement stmt = pool.createStatement();
			affectedRows = stmt.executeUpdate(createTableSQLOfCatalogTreeView);
			System.out.println(createTableSQLOfCatalogTreeView + "  �����ɹ�.");
		} catch (SQLException e) {
			failedCount++;
			System.out.println(createTableSQLOfCatalogTreeView + "  ����ʧ��.");
		}

		if (failedCount == 0) {
			System.out.println("���еı����ɹ�.");
		} else {
			System.out.println(failedCount + " ����ʧ��.");
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
	 * �����ݿ��м���.
	 * 
	 * @param pool
	 *            ���ݿ����ӳ�.
	 * @param catalogID
	 *            Ŀ¼����ID
	 * @param catalogTreeViewName
	 *            Ŀ¼����ͼ�ı���.
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
				catalogTreeView.setPool(pool); // ���潫ֱ�ӷ���������ݿ����ӳ�

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
			System.out.println("  ����:" + catalogTreeViewTableName + " ʧ��. ");
		}
		return catalogTreeView;
	}

	/**
	 * ����ҳĿ¼��ɾ������ƶ�����վ
	 * 
	 * @return
	 */
	public boolean deleteFromCatalogTree(int folderID, int webID) {
		return false;
	}

	/**
	 * ����ҳĿ¼��ɾ������ƶ�����վ
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

	// ������.
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

		// �����ݿ��м�������.
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

		// ����һ���µ�Ŀ¼���ͼ�����صı��.
		String prefix = "Web";

		boolean ok = CatalogTreeView.initCatalogTreeTables(pool, prefix);
		CatalogTreeView ctv = new CatalogTreeView();
		ctv.setPool(pool);
		ctv.setCatalogTreeID(100);
		ctv.setName("ȫ����վ����Ŀ¼");
		ctv.initCatalogTreeView(prefix);

		long dir_541help_com = 100;

		String catalogTreeViewName = prefix + "CatalogTreeView";

		ctv = CatalogTreeView.loadFromDatabase(pool, 100, catalogTreeViewName);

		CatalogTree ct = null;// ctv.getCatalogTree(dir_541help_com);

		if (ct == null) {
			ct = CatalogTree.createCatalog(null, "ȫ����վ����Ŀ¼",
					"globalWebClassify", 0, ctv);
			System.out.println("��Ŀ¼�����ɹ�.");
		}

		if (ct != null) {
			System.out.println(ct);
		}
	}

}
