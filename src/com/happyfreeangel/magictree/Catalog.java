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

//Ŀ¼��.
public class Catalog implements Serializable, Comparable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long id; // Ŀ¼ID
	private String name; // Ŀ¼����
	private String folder; // �ļ���Ŀ¼Ӣ������
	private long fatherID; // ��Ŀ¼ID
	private long attribute; // Ŀ¼����.

	// long ��64λ����ͬʱ����64������.
	private final static long HIDDEN = 1; // 0000000000000001 //��Ŀ¼����ʾ
	private final static long READONLY = 2; // 0000000000000010 //��Ŀ¼�����޸�
	private final static long UPDATE = 4; // 0000000000000100 //�������ϸ���.

	// �ø�32λ��ʾ�ļ��е���ʾ˳��, ��32λ��ʾ�ļ��ĸ����Զ�������.

	public Catalog() {
	}

	/**
	 * ����Ŀ¼������
	 * 
	 * @param attribute
	 */
	public void setAttribute(long attribute) {
		this.attribute = attribute;
	}

	/**
	 * ��ȡĿ¼������
	 * 
	 * @return
	 */
	public long getAttribute() {
		return this.attribute;
	}

	// �ж��Ƿ������ص�Ŀ¼
	public boolean isHidden() {
		return (this.attribute & Catalog.HIDDEN) != 0;
	}

	// �ж��Ƿ���ֻ��,ֻ���Ľ��������������Ŀ¼������.
	public boolean isReadOnly() {
		return (this.attribute & Catalog.READONLY) != 0;
	}

	public int compareTo(Object other) {
		Catalog tmp = (Catalog) other;
		return (int) (this.getID() - tmp.getID());
	}

	public void setFatherID(long fatherID) {
		this.fatherID = fatherID;
	}

	public long getFatherID() {
		return fatherID;
	}

	// �����root,fatherID Լ��Ϊ-1.
	protected Catalog(long fatherID, long id, String name, String folder) {
		this.fatherID = fatherID;
		this.id = id;
		this.name = name;
		this.folder = folder;
		this.attribute = 0;
	}

	// �����root,fatherID Լ��Ϊ-1.
	protected Catalog(long fatherID, long id, String name, String folder,
			long attribute) {
		this.fatherID = fatherID;
		this.id = id;
		this.name = name;
		this.folder = folder;
		this.attribute = attribute;
	}

	/**
	 * ����һ��Ŀ¼.
	 * 
	 * @param fatherID
	 * @param id
	 * @param name
	 * @param folder
	 * @return
	 */
	public static Catalog makeCatalog(long fatherID, long id, String name,
			String folder) {
		long attribute = 0;
		return makeCatalog(fatherID, id, name, folder, attribute);
	}

	/**
	 * ����һ��Ŀ¼.
	 * 
	 * @param fatherID
	 * @param id
	 * @param name
	 * @param folder
	 * @param attribute
	 * @return
	 */
	public static Catalog makeCatalog(long fatherID, long id, String name,
			String folder, long attribute) {
		if (name != null) {
			name = name.trim();
		}
		if (folder != null) {
			folder = folder.trim();
		}

		if (name == null) // || folder == null
		{
			return null;
		}
		return new Catalog(fatherID, id, name, folder, attribute);
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	}

	public String getFolder() {
		return folder;
	}

	public long getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public void setID(long id) {
		this.id = id;
	}

	public String toString() {
		String info = "name=" + name + "  id=" + id + " folder=" + folder;
		if (this.isHidden()) {
			info += " attribute:hidden";
		}

		if (this.isReadOnly()) {
			info += " attribute:readOnly";
		}
		return info;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * ����һ���µ�Ŀ¼ catalogID Ŀ¼����ID
	 * 
	 * @param fatherID
	 *            ��Ŀ¼ID
	 * @param name
	 *            ��Ŀ¼���� Ŀ¼��һ����
	 * @param folder
	 *            �ļ������� Ŀ¼�ļ���Ҳ��һ��.
	 * @param attribute
	 *            Ŀ¼����
	 * @return ���ó�Ϊ����͵� �ɹ�����Ŀ¼ID��ʧ�ܷ���-1;
	 */
	public long save(CatalogTreeView catalogTreeView) {
		int failed = -1;

		if (catalogTreeView == null) {
			System.out.println(" catalogTreeView is null, ��վĿ¼�޷�����!");
			return failed;
		}

		// �ж���ͬĿ¼���Ƿ������ͬ������,��������������.
		String tableName = catalogTreeView.getCatalogTreeDefinitionTableName();

		String testIfHasExitsSQL = " select id,name  from " + tableName
				+ " where (name='" + name + "' or folder='" + folder
				+ "') and fatherID=" + fatherID + " and catalogTreeID ="
				+ catalogTreeView.getCatalogTreeID();

		if (folder == null) {
			testIfHasExitsSQL = " select id,name  from " + tableName
					+ " where name='" + name + "' and fatherID=" + fatherID
					+ " and catalogTreeID ="
					+ catalogTreeView.getCatalogTreeID();
		}

		Statement stmt = null;
		boolean hasExits = false;
		ResultSet rs = null;
		try {
			stmt = catalogTreeView.getPool().createStatement();
			rs = stmt.executeQuery(testIfHasExitsSQL);
			if (rs.next()) {
				hasExits = true;
				System.out.println("<br> �Բ���,��Ҫ��ӵ���Ŀ¼:" + name + " �Ѿ�������. id="
						+ rs.getInt("id"));
				failed = -1;
			}
			failed = 0;
		} catch (SQLException e) {
			System.out.println(e);
		} finally {
			if (stmt != null) {
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

		if (failed < 0) {
			return failed;
		}

		long sonCatalogID = -1;
		long maxID = -1;

		// id ʹ�õ�����˳��,ֻ��һ��Ψһ�Ա�־,���ٸ����κ�������,

		String testSQL = " select max(id) as id from " + tableName
				+ "  where catalogTreeID = "
				+ catalogTreeView.getCatalogTreeID();

		try {
			stmt = catalogTreeView.getPool().createStatement();
			rs = stmt.executeQuery(testSQL);
			if (rs.next()) {
				int tt = rs.getInt(1);
				if (tt >= 0) {
					maxID = rs.getInt(1);
				}
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

		sonCatalogID = maxID + 1;

		if (fatherID < 0) {
			sonCatalogID = 0; // ������Ŀ¼.
		}

		if (folder == null) // folder==null��ʾҪʹ��ȱʡĿ¼,Ŀ¼ID���� folderID
		{
			folder = sonCatalogID + "";
		}

		// ����Ŀ¼�ĸ�32Ϊ��ǰ��ʱ���ȥ�˶��ٷ���.
		if (this.attribute == 0) {
			long timeNow = System.currentTimeMillis();
			int k = (int) (timeNow / 1000 / 60);
			attribute = attribute ^ (k << 32);
		}

		PreparedStatement pstmt = null;
		String insertSQL = "insert into "
				+ tableName
				+ "(catalogTreeID,fatherID,id,name,folder,attribute)values(?,?,?,?,?,?)";

		try {
			pstmt = catalogTreeView.getPool().prepareStatement(insertSQL);
			pstmt.setLong(1, catalogTreeView.getCatalogTreeID());
			pstmt.setLong(2, fatherID);
			// ����ID.
			pstmt.setLong(3, sonCatalogID);
			pstmt.setString(4, name);
			pstmt.setString(5, folder);
			pstmt.setLong(6, attribute);
			int affectedRecordCount = pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			System.out.println(e + " sonCatalogID=" + sonCatalogID
					+ " fatherID=" + fatherID);
			return failed;
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
					System.out.println(e);
				}
			}
		}
		return sonCatalogID;
	}

	/**
	 * ����һ��Ŀ¼, ID ���Զ����ɵ�.
	 * 
	 * @param fatherID
	 * @param name
	 * @param folder
	 * @param attribute
	 * @param webCatalogView
	 * @return
	 */
	public static boolean createCatalog(long fatherID, String name,
			String folder, long attribute, CatalogTreeView catalogTreeView) {
		Catalog catalog = Catalog.makeCatalog(fatherID, -1, name, folder,
				attribute);
		if (catalog == null) {
			return false;
		}
		return catalog.save(catalogTreeView) >= 0;
	}

	public static void main(String[] args) {

	}
}
