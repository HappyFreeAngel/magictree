package com.happyfreeangel.magictree;

/**
 * <p>Title: 魔树</p>
 * <p>Description: 魔树</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author happy
 * @version 0.1
 */

import java.util.*;
import java.io.*;
import java.sql.*;
import net.snapbug.util.dbtool.*;
import java.net.*;

public class CatalogTree extends Catalog implements Tree, Serializable,
		Comparable {
	private static final long serialVersionUID = 1L;
	private CatalogTree father;
	private Collection<CatalogTree> sons;

	// 2008-06-19 开心添加, 这样就形成了一棵有果实的信息树。
	private Collection<Object> infos;// 目录下存放的信息

	public Collection<Object> getInfo() {
		return infos;
	}

	public CatalogTree() {

	}

	public int compareTo(Object o) {
		if (o == null) {
			return -1;
		}
		CatalogTree tmp = (CatalogTree) o;

		return (int) (this.getID() - tmp.getID());

	}

	public Tree getFather() {
		return father;
	}

	public void setFather(Tree father) {
		this.father = (CatalogTree) father;
	}

	public Collection<CatalogTree> getSons() {
		return sons;
	}

	public void setSons(Collection<CatalogTree> sons) {
		this.sons = sons;
	}

	public boolean isRoot() {
		// 判断是否是树根
		if (this.getFather() == null && this.getID() == 0) {
			return true;
		}
		return false;
	}

	public boolean isLeaf() {
		if (this.sons == null) {
			return true;
		}
		if (this.sons.size() <= 0) {
			return true;
		}
		return false;
	}

	// 2009-10-23 开心添加 于上海。
	public boolean isOffspring(Tree who) {
		Stack<CatalogTree> stack = new Stack<CatalogTree>();

		stack.push((CatalogTree) who);
		while (stack.size() > 0) {
			CatalogTree temp = stack.pop();
			if (temp.equals(this)) // ??
			{
				return true;
			}
			Iterator<CatalogTree> itor = temp.getSons().iterator();
			while (itor.hasNext()) {
				CatalogTree tempSon = itor.next();
				stack.push(tempSon);
			}
		}
		return false;
	}

	public boolean removeSon(Tree son) {
		return this.sons.remove(son);
	}

	/**
	 * 添加成功返回true.
	 * 
	 * @param son
	 * @return
	 */
	public boolean addSon(Tree son) {
		boolean hasExits = false;
		Iterator<CatalogTree> itor = sons.iterator();
		CatalogTree tmp = null;
		CatalogTree tmpSon = (CatalogTree) son;
		while (itor.hasNext()) {
			tmp = (CatalogTree) itor.next();
			if ((tmpSon.getID() == tmp.getID())
					|| ((tmpSon.getName() != null && tmp.getName() != null) && (tmpSon
							.getName() == tmp.getName()))) {
				hasExits = true;
				break;
			}
		}
		if (!hasExits) {
			this.sons.add((CatalogTree) son); // ??
			son.setFather(this);
		}

		if (!hasExits) {
			return true;
		}
		return false;
	}

	/**
	 * 获取当前位置相对根目录所在的目录层次深度 根目录为level 0, 根目录下的子目录为level 为 1
	 * 
	 * @return
	 */
	public int getLevel() {
		int level = 0;
		CatalogTree tmp = this;
		while (!tmp.isRoot()) {
			level++;
			tmp = (CatalogTree) tmp.getFather();
			if (tmp == null) {
				System.out
						.println("tree error. int getLevel(). the father tree is null.");
				break;
			}
		}
		return level;
	}

	/**
	 * 找到当前的根目录.
	 * 
	 * @return
	 */
	public CatalogTree getRoot() {
		CatalogTree tmp = null;
		int count = 0, maxCount = 1000;

		// 自己就是Root
		if (this.isRoot()) {
			return this;
		}

		tmp = (CatalogTree) this.getFather();

		if (tmp == null) {
			System.out.println(" error. has not found father.");
		}

		while (tmp != null) {
			if (tmp.isRoot()) {
				return tmp;
			}
			tmp = (CatalogTree) tmp.getFather();
			count++;

			if (count > maxCount) {
				System.out
						.println("fital error. root and son is the same one.");
				System.out.println(this);
				System.out.println("\n\n me =" + this + " father=" + tmp
						+ "\n\n");
				break;
			}
		}
		return tmp;
	}

	/**
	 * 
	 * @param me
	 *            当前的树节点
	 * @param fromWhere
	 *            开始位置
	 * @param folderIDs
	 *            目录IDs
	 * @param ht
	 *            HashTable, 可以很快的找到目标.
	 * @return
	 */

	public static CatalogTree findMyAncestor(CatalogTree me, int fromWhere,
			Vector<Long> folderIDs, Hashtable ht) {
		// 从最深层次的树开始.
		CatalogTree tmpCatalogTree = null;
		for (int i = fromWhere; i >= 0; i--) {
			tmpCatalogTree = (CatalogTree) ht.get((Long) folderIDs.get(i));

			if (me.getID() == tmpCatalogTree.getID()) // 是同一个对象.
			{
				continue;
			}

			if (me.getFatherID() == tmpCatalogTree.getID()) {
				tmpCatalogTree.addSon(me); // 父亲认识我
				me.setFather(tmpCatalogTree); // 我认识父亲
				if (!tmpCatalogTree.isRoot()) // 还不是祖先,继续找.
				{
					tmpCatalogTree = findMyAncestor(tmpCatalogTree,
							fromWhere - 1, folderIDs, ht);
				}
				break;
			}
		}
		return tmpCatalogTree;
	}

	/**
	 * 修改目录属性 或名称 或文件夹
	 * 
	 * @param id
	 * @param newCatalog
	 *            id 不能改
	 * @param CatalogTreeView
	 * @return
	 */
	public boolean updateCatalog(long id, Catalog newCatalog,
			CatalogTreeView catalogTreeView) {
		CatalogTree obj = this.findByID(id);
		if (obj == null) {
			return false;
		}

		if (obj.isReadOnly()) {
			System.out.println("这个目录是只读的,不能修改,要修改必须先去掉只读属性.");

			// 抛出异常.
			// ......

			return false;
		}
		// 修改内存中的对象.
		obj.setName(newCatalog.getName());
		obj.setFolder(newCatalog.getFolder());
		obj.setAttribute(newCatalog.getAttribute());

		try {
			String updateSQL = "update "
					+ catalogTreeView.getCatalogTreeDefinitionTableName()
					+ " set name = ?, folder = ?, attribute = ? where catalogTreeID = ? and id = ?";

			PreparedStatement pstmt = catalogTreeView.getPool()
					.prepareStatement(updateSQL);

			pstmt.setString(1, obj.getName());
			pstmt.setString(2, obj.getFolder());
			pstmt.setLong(3, obj.getAttribute());
			pstmt.setLong(4, catalogTreeView.getCatalogTreeID());
			pstmt.setLong(5, obj.getID());

			int affectedRowCount = pstmt.executeUpdate();
			if (affectedRowCount != 1) {
				System.out.println("更新子树失败. 目录树ID="
						+ catalogTreeView.getCatalogTreeID() + " folderID = "
						+ obj.getID() + "  name = " + obj.getName()
						+ " folder=" + obj.getFolder());
			}
		} catch (SQLException e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

	/**
	 * 根据目录ID 删除目录 只从内存中删除.
	 * 
	 * @param id
	 * @return 删除成功返回 true
	 */
	public boolean deleteByID(long id) {
		CatalogTree tmp = this.findByID(id);
		if (tmp == null) {
			System.out.println("找不到ID=" + id);
			return false;
		}

		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(tmp);

		while (stack.size() > 0) {
			// 删除所有的子目录,如果有的话.
			CatalogTree t = (CatalogTree) stack.pop();
			if (t.isLeaf()) {
				if (t.getFather() != null) {
					t.getFather().getSons().remove(t);
				} else // this is root
				{
					t.setName(null);
					t.setID(-1);
					t.setFolder(null);
					t = null;
				}
				continue;
			} else // 展开后放回去,这样才能顺利删除,因为删除是从底层往上层来删除的.
			{
				stack.push(t);
				Iterator<CatalogTree> itor = t.getSons().iterator();
				while (itor.hasNext()) {
					stack.push(itor.next());
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * @param sonCatalogName
	 *            子树名称,从数据库删除.
	 * @param deleteFromDatabase
	 * @return
	 */
	public boolean deleteCatalog(String sonCatalogName,
			boolean deleteFromDatabase, CatalogTreeView catalogTreeView) {
		if (sonCatalogName == null || sonCatalogName.length() < 1) {
			return false;
		}

		Iterator<CatalogTree> itor = this.getSons().iterator();
		CatalogTree tmp = null, obj = null;

		while (itor.hasNext()) {
			tmp = (CatalogTree) itor.next();
			// 忽略目录名称的大小写.
			if (sonCatalogName.equalsIgnoreCase(tmp.getName())) {
				// 找到了目标.
				obj = tmp;
				break;
			}
		}

		if (obj == null) {
			return false; // 没有找到.
		}

		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(obj);
		CatalogTree tmpCatalog = null;

		while (stack.size() > 0) {
			tmpCatalog = (CatalogTree) stack.pop();
			if (!tmpCatalog.isLeaf()) {
				// 不是子树,就弹出,把弹出的目录获取子树，一个个压入堆栈,如此反复，直到遇到子树.
				Iterator<CatalogTree> itor3 = tmpCatalog.sons.iterator();
				while (itor3.hasNext()) {
					stack.push(itor3.next());
				}
				continue;
			}

			if (deleteFromDatabase) {
				try {
					String deleteSQL = "delete from "
							+ catalogTreeView
									.getCatalogTreeDefinitionTableName()
							+ " where catalogTreeID = ? and id = ?";
					PreparedStatement pstmt = catalogTreeView.getPool()
							.prepareStatement(deleteSQL);

					pstmt.setLong(1, catalogTreeView.getCatalogTreeID());
					pstmt.setLong(2, tmpCatalog.getID());

					int affectedRowCount = pstmt.executeUpdate();
					if (affectedRowCount != 1) {
						System.out.println("删除子树失败." + tmpCatalog.toString());
					}
				} catch (SQLException e) {
					System.out.println(e);
					return false;
				}
			}

			// delete from memory. ???
			if (!tmpCatalog.isRoot()) {
				tmpCatalog.getFather().removeSon(tmpCatalog);
			}
			// 要不要保存到XML, javabean 中???
		}
		return true;
	}

	/**
	 * 
	 * @param pool
	 * @param poolName
	 * @return
	 */
	public static CatalogTree loadFromDatabase(CatalogTreeView catalogTreeView) {

		// long catalogTreeID = catalogTreeView.getCatalogTreeID();
		String tableName = catalogTreeView.getCatalogTreeDefinitionTableName();
		CatalogTree root = null;
		Hashtable<Long, CatalogTree> ht = new Hashtable<Long, CatalogTree>(); // 为了能够快速找到.
		Vector<Long> folderIDs = new Vector<Long>(100, 100); // 用来记录目录ID的值和大小的顺序.

		// 注意必须按照升序排序,否则结果会错掉.
		String sql = "select fatherID,id,name,folder,attribute from "
				+ tableName + " where catalogTreeID="
				+ catalogTreeView.getCatalogTreeID()
				+ " order by fatherID asc,id asc ";

		// 把网页目录结构加载到hashtable 中.
		try {
			Catalog tmpCatalog = null;
			CatalogTree tmpCalogTree = null;
			long fatherID = 0, id = 0;
			String name = null, folder = null;
			long attribute = 0;
			Statement stmt = catalogTreeView.getPool().createStatement();

			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				fatherID = rs.getLong("fatherID");
				id = rs.getLong("id");
				name = rs.getString("name");
				folder = rs.getString("folder");
				attribute = rs.getLong("attribute");

				tmpCatalog = Catalog.makeCatalog(fatherID, id, name, folder,
						attribute);
				tmpCalogTree = CatalogTree.makeCatalogTree(null, tmpCatalog);
				ht.put(new Long(id), tmpCalogTree);
				folderIDs.add(new Long(id)); // 记住这个位置.
			}
			rs.close();

			CatalogTree me = null;
			Long folderID = null;

			// 从最深层次的树开始,儿子找父亲,父亲找爷爷,爷爷找爷爷的爸爸,
			// 直到找到祖先,早到后就让他们互相认识,把它们关联起来.逐渐构造出一棵树
			for (int i = folderIDs.size() - 1; i >= 0; i--) {
				folderID = (Long) folderIDs.get(i);
				me = (CatalogTree) ht.get(folderID);
				CatalogTree tmp = findMyAncestor(me, i, folderIDs, ht);// ??
			}

			// 最上面的就是root.
			if (folderIDs.size() > 0) {
				root = (CatalogTree) ht.get((Long) folderIDs.get(0));
			}
			// root.getID()>0 就不是根目录,可能没有设置根目录的名称.
		} catch (SQLException e) {
			System.out.println(e);
		}

		return root;
	}

	/**
	 * 
	 * @param father
	 *            父目录
	 * @param name
	 *            当前目录名称
	 * @param dirName
	 *            当前目录对应的文件目录名称
	 * @param fileName
	 *            当前目录对应的文件名称.
	 */

	private CatalogTree(CatalogTree father, Catalog catalog) {
		this.father = father;
		this.setID(catalog.getID());
		this.setName(catalog.getName());
		this.setFatherID(catalog.getFatherID());
		this.setFolder(catalog.getFolder());
		this.setAttribute(catalog.getAttribute());
		this.sons = Collections.synchronizedSet(new TreeSet<CatalogTree>()); // 确保同步操作;
																				// 用TreeSet
																				// 可以让子树有顺序的排序,按升序顺序.
		if (father != null) {
			father.addSon(this);
		}
	}

	/**
	 * 在内存中构造一棵树.
	 * 
	 * @param father
	 * @param catalog
	 * @return
	 */
	public static CatalogTree makeCatalogTree(CatalogTree father,
			Catalog catalog) {
		if (catalog == null) {
			return null;
		}
		return new CatalogTree(father, catalog);
	}

	/**
	 * 在指定的父目录下,创建一个子目录,在数据库中创建. 可以使用事件监听器. 在创建目录的时候; 在删除目录的时候.
	 * 
	 * @param father
	 * @param name
	 * @param folder
	 * @param attribute
	 * @param webCatalogView
	 * @return
	 */
	public static CatalogTree createCatalog(CatalogTree father, String name,
			String folder, long attribute, CatalogTreeView catalogTreeView) {
		long fatherID = -1;
		if (father != null) {
			fatherID = father.getID();
		}
		Catalog catalog = Catalog.makeCatalog(fatherID, -1, name, folder,
				attribute);

		if (catalog == null) {
			return null;
		}

		long newCatalogID = catalog.save(catalogTreeView);

		if (newCatalogID < 0) {
			return null;
		}
		catalog.setID(newCatalogID);

		return CatalogTree.makeCatalogTree(father, catalog);
	}

	/**
	 * 通过当前的整个目录树来查找.
	 * 
	 * @param id
	 * @return
	 */
	public CatalogTree findByID(long id) {
		CatalogTree obj = null;
		if (this.getID() == id) {
			obj = this;
		} else {
			Iterator<CatalogTree> itor = this.getSons().iterator();
			while (itor.hasNext()) {
				CatalogTree tmpSon = (CatalogTree) itor.next();
				if (tmpSon == null) {
					System.out.println("error !!!!!!!!!! son is null son id = "
							+ id + " WebCatalog findByID(long id)");
					continue;
				}
				obj = tmpSon.findByID(id);
				if (obj != null) // 终止条件. 只有一个不再找了.
				{
					break;
				}
			}
		}

		return obj;
	}

	// 获取当前目录的所在虚拟目录全路径. 最后不含有/
	public String getVirtualFullDirPath() {
		String virtualFullDirPath = "";
		CatalogTree tmp = this;
		while (true) {
			virtualFullDirPath = java.io.File.separator + tmp.getFolder()
					+ virtualFullDirPath;
			if (tmp.isRoot()) {
				break;
			}
			tmp = (CatalogTree) tmp.getFather();
		}
		return virtualFullDirPath;
	}

	public String getVirtualFullWebDirPath() {
		String virtualFullDirPath = "";
		CatalogTree tmp = this;
		while (true) {
			virtualFullDirPath = "/" + tmp.getFolder() + virtualFullDirPath;
			if (tmp.isRoot()) {
				break;
			}
			tmp = (CatalogTree) tmp.getFather();
		}
		return virtualFullDirPath;
	}

	// 获取当前目录的所在目录全路径.
	public String getSystemFullDirPath(String systemDirOfRoot) {
		if (systemDirOfRoot == null) {
			System.out.println(" systemFullDirPath is null.");
			return null;
		}
		String virtualFullDirPath = "";
		CatalogTree tmp = this;
		while (true) {
			// 不同的操作系统返回的结果是不一样的.
			virtualFullDirPath = java.io.File.separator + tmp.getFolder()
					+ virtualFullDirPath;
			if (tmp.isRoot()) {
				break;
			}
			tmp = (CatalogTree) tmp.getFather();
		}
		return systemDirOfRoot + virtualFullDirPath;
	}

	public String getFullCatalogPath() {
		String catalogPath = this.getName();
		if (this.isRoot()) {
			return catalogPath;
		}

		CatalogTree tmp = (CatalogTree) this.getFather();
		if (tmp == null) {
			return catalogPath; // this is that case :this is root.
		}

		while (true) {
			catalogPath = tmp.getName() + "->" + catalogPath;
			tmp = (CatalogTree) tmp.getFather();
			if (tmp == null) {
				break;
			}
			if (tmp.isRoot()) {
				catalogPath = tmp.getName() + "->" + catalogPath;
				break;
			}
		}
		return catalogPath;
	}
	
	public String getOptionCatalogPath() {
		String catalogPath = this.getName();
		if (this.isRoot()) {
			return catalogPath;
		}

		CatalogTree tmp = (CatalogTree) this.getFather();
		if (tmp == null) {
			return catalogPath; // this is that case :this is root.
		}

		while (true) {
			catalogPath = tmp.getName() + "->" + catalogPath;
			tmp = (CatalogTree) tmp.getFather();
			if (tmp == null) {
				break;
			}
			if (tmp.isRoot()) {
				//catalogPath = tmp.getName() + "->" + catalogPath;
				break;
			}
		}
		return catalogPath;
	}

	/**
	 * 转化为HTML OPTION
	 * 
	 * @param selectedID
	 *            int
	 * @return String
	 */
	public String toOptions(int selectedID) {
		StringBuffer info = new StringBuffer();

		if (this.getID() == selectedID) {
			info.append("<option value='" + this.getID() + "' selected>"
					+ this.getFullCatalogPath() + "</option>\n");
		} else {
			info.append("<option value='" + this.getID() + "'>"
					+ this.getFullCatalogPath() + "</option>\n");
		}

		CatalogTree tmp = null;
		Iterator<CatalogTree> itor = this.getSons().iterator();
		while (itor.hasNext()) {
			tmp = (CatalogTree) itor.next();
			info.append(tmp.toOptions(selectedID));
		}
		return info.toString();
	}
	
	/**
	 * 转化为HTML OPTION
	 * 
	 * @param selectedID
	 *            int
	 * @return String
	 */
	public String toJsonArray(int selectedID) {
		StringBuffer info = new StringBuffer();
		String selectedValue= (this.getID() == selectedID) ? "1" : "0";
		info.append("{\"tid\":\"" + this.getID() + "\",\"tn\":"
					+ "\""+ this.getOptionCatalogPath() + "\",\"ts\":\""+selectedValue+"\"},\n");//1表示选择了.
		CatalogTree tmp = null;
		Iterator<CatalogTree> itor = this.getSons().iterator();
		while (itor.hasNext()) {
			tmp = (CatalogTree) itor.next();
			info.append(tmp.toJsonArray(selectedID));
		}
		return info.toString();
	}

	/**
	 * //对目录进行重新命名.
	 * 
	 * @param ctv
	 * @param catalogTreeID
	 *            目录树ID
	 * @param newName
	 *            目录名称
	 * @param NewFolder
	 *            目录代表的文件夹
	 * @return
	 */
	public boolean rename(CatalogTreeView ctv, long catalogTreeID,
			String newName, String newFolder) {

		if (ctv == null || newName == null || newFolder == null) {
			return false;
		}
		String updateSQL = " update " + ctv.getCatalogTreeDefinitionTableName()
				+ " set name = ?, folder = ? where "
				+ ctv.getCatalogTreeDefinitionTableName() + ".catalogTreeID="
				+ catalogTreeID + " and "
				+ ctv.getCatalogTreeDefinitionTableName() + ".id="
				+ this.getID();

		boolean isOK = false;
		try {
			PreparedStatement pstmt = ctv.getPool().prepareStatement(updateSQL);
			pstmt.setString(1, newName);
			pstmt.setString(2, newFolder);
			int affectedRows = pstmt.executeUpdate();
			isOK = (affectedRows == 1);
			pstmt.close();
		} catch (SQLException e) {
			System.out.println(e);
		}
		// 不要忘记更新内存对象.
		if (isOK) {
			this.setName(newName);
			this.setFolder(newFolder);
		}
		return isOK;
	}

	// 对目录进行重新命名.
	public boolean rename(CatalogTreeView ctv, long catalogTreeID,
			String newName) {
		return rename(ctv, catalogTreeID, newName, this.getFolder());
	}
}
