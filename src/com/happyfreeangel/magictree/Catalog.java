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

//目录树.
public class Catalog implements Serializable, Comparable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long id; // 目录ID
	private String name; // 目录名称
	private String folder; // 文件夹目录英文名称
	private long fatherID; // 父目录ID
	private long attribute; // 目录属性.

	// long 有64位可以同时设置64种属性.
	private final static long HIDDEN = 1; // 0000000000000001 //该目录不显示
	private final static long READONLY = 2; // 0000000000000010 //该目录不能修改
	private final static long UPDATE = 4; // 0000000000000100 //必须马上更新.

	// 用高32位表示文件夹的显示顺序, 低32位表示文件的各种自定义属性.

	public Catalog() {
	}

	/**
	 * 设置目录的属性
	 * 
	 * @param attribute
	 */
	public void setAttribute(long attribute) {
		this.attribute = attribute;
	}

	/**
	 * 获取目录的属性
	 * 
	 * @return
	 */
	public long getAttribute() {
		return this.attribute;
	}

	// 判断是否是隐藏的目录
	public boolean isHidden() {
		return (this.attribute & Catalog.HIDDEN) != 0;
	}

	// 判断是否是只读,只读的将不能向里面添加目录和文章.
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

	// 如果是root,fatherID 约定为-1.
	protected Catalog(long fatherID, long id, String name, String folder) {
		this.fatherID = fatherID;
		this.id = id;
		this.name = name;
		this.folder = folder;
		this.attribute = 0;
	}

	// 如果是root,fatherID 约定为-1.
	protected Catalog(long fatherID, long id, String name, String folder,
			long attribute) {
		this.fatherID = fatherID;
		this.id = id;
		this.name = name;
		this.folder = folder;
		this.attribute = attribute;
	}

	/**
	 * 创建一个目录.
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
	 * 创建一个目录.
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
	 * 创建一个新的目录 catalogID 目录名称ID
	 * 
	 * @param fatherID
	 *            父目录ID
	 * @param name
	 *            父目录名称 目录不一样，
	 * @param folder
	 *            文件夹名称 目录文件夹也不一样.
	 * @param attribute
	 *            目录属性
	 * @return 设置成为轻便型的 成功返回目录ID，失败返回-1;
	 */
	public long save(CatalogTreeView catalogTreeView) {
		int failed = -1;

		if (catalogTreeView == null) {
			System.out.println(" catalogTreeView is null, 网站目录无法创建!");
			return failed;
		}

		// 判断相同目录下是否存在相同的名称,如果存在则不能添加.
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
				System.out.println("<br> 对不起,您要添加的子目录:" + name + " 已经存在了. id="
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

		// id 使用递增的顺序,只是一种唯一性标志,不再赋予任何意义了,

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
			sonCatalogID = 0; // 创建根目录.
		}

		if (folder == null) // folder==null表示要使用缺省目录,目录ID就是 folderID
		{
			folder = sonCatalogID + "";
		}

		// 设置目录的高32为当前的时间过去了多少分钟.
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
			// 生成ID.
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
	 * 创建一个目录, ID 是自动生成的.
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
