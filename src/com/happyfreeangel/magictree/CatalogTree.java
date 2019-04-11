package com.happyfreeangel.magictree;

/**
 * <p>Title: ħ��</p>
 * <p>Description: ħ��</p>
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

	// 2008-06-19 �������, �������γ���һ���й�ʵ����Ϣ����
	private Collection<Object> infos;// Ŀ¼�´�ŵ���Ϣ

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
		// �ж��Ƿ�������
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

	// 2009-10-23 ������� ���Ϻ���
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
	 * ��ӳɹ�����true.
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
	 * ��ȡ��ǰλ����Ը�Ŀ¼���ڵ�Ŀ¼������ ��Ŀ¼Ϊlevel 0, ��Ŀ¼�µ���Ŀ¼Ϊlevel Ϊ 1
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
	 * �ҵ���ǰ�ĸ�Ŀ¼.
	 * 
	 * @return
	 */
	public CatalogTree getRoot() {
		CatalogTree tmp = null;
		int count = 0, maxCount = 1000;

		// �Լ�����Root
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
	 *            ��ǰ�����ڵ�
	 * @param fromWhere
	 *            ��ʼλ��
	 * @param folderIDs
	 *            Ŀ¼IDs
	 * @param ht
	 *            HashTable, ���Ժܿ���ҵ�Ŀ��.
	 * @return
	 */

	public static CatalogTree findMyAncestor(CatalogTree me, int fromWhere,
			Vector<Long> folderIDs, Hashtable ht) {
		// �������ε�����ʼ.
		CatalogTree tmpCatalogTree = null;
		for (int i = fromWhere; i >= 0; i--) {
			tmpCatalogTree = (CatalogTree) ht.get((Long) folderIDs.get(i));

			if (me.getID() == tmpCatalogTree.getID()) // ��ͬһ������.
			{
				continue;
			}

			if (me.getFatherID() == tmpCatalogTree.getID()) {
				tmpCatalogTree.addSon(me); // ������ʶ��
				me.setFather(tmpCatalogTree); // ����ʶ����
				if (!tmpCatalogTree.isRoot()) // ����������,������.
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
	 * �޸�Ŀ¼���� ������ ���ļ���
	 * 
	 * @param id
	 * @param newCatalog
	 *            id ���ܸ�
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
			System.out.println("���Ŀ¼��ֻ����,�����޸�,Ҫ�޸ı�����ȥ��ֻ������.");

			// �׳��쳣.
			// ......

			return false;
		}
		// �޸��ڴ��еĶ���.
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
				System.out.println("��������ʧ��. Ŀ¼��ID="
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
	 * ����Ŀ¼ID ɾ��Ŀ¼ ֻ���ڴ���ɾ��.
	 * 
	 * @param id
	 * @return ɾ���ɹ����� true
	 */
	public boolean deleteByID(long id) {
		CatalogTree tmp = this.findByID(id);
		if (tmp == null) {
			System.out.println("�Ҳ���ID=" + id);
			return false;
		}

		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(tmp);

		while (stack.size() > 0) {
			// ɾ�����е���Ŀ¼,����еĻ�.
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
			} else // չ����Ż�ȥ,��������˳��ɾ��,��Ϊɾ���Ǵӵײ����ϲ���ɾ����.
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
	 *            ��������,�����ݿ�ɾ��.
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
			// ����Ŀ¼���ƵĴ�Сд.
			if (sonCatalogName.equalsIgnoreCase(tmp.getName())) {
				// �ҵ���Ŀ��.
				obj = tmp;
				break;
			}
		}

		if (obj == null) {
			return false; // û���ҵ�.
		}

		Stack<CatalogTree> stack = new Stack<CatalogTree>();
		stack.push(obj);
		CatalogTree tmpCatalog = null;

		while (stack.size() > 0) {
			tmpCatalog = (CatalogTree) stack.pop();
			if (!tmpCatalog.isLeaf()) {
				// ��������,�͵���,�ѵ�����Ŀ¼��ȡ������һ����ѹ���ջ,��˷�����ֱ����������.
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
						System.out.println("ɾ������ʧ��." + tmpCatalog.toString());
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
			// Ҫ��Ҫ���浽XML, javabean ��???
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
		Hashtable<Long, CatalogTree> ht = new Hashtable<Long, CatalogTree>(); // Ϊ���ܹ������ҵ�.
		Vector<Long> folderIDs = new Vector<Long>(100, 100); // ������¼Ŀ¼ID��ֵ�ʹ�С��˳��.

		// ע����밴����������,����������.
		String sql = "select fatherID,id,name,folder,attribute from "
				+ tableName + " where catalogTreeID="
				+ catalogTreeView.getCatalogTreeID()
				+ " order by fatherID asc,id asc ";

		// ����ҳĿ¼�ṹ���ص�hashtable ��.
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
				folderIDs.add(new Long(id)); // ��ס���λ��.
			}
			rs.close();

			CatalogTree me = null;
			Long folderID = null;

			// �������ε�����ʼ,�����Ҹ���,������үү,үү��үү�İְ�,
			// ֱ���ҵ�����,�絽��������ǻ�����ʶ,�����ǹ�������.�𽥹����һ����
			for (int i = folderIDs.size() - 1; i >= 0; i--) {
				folderID = (Long) folderIDs.get(i);
				me = (CatalogTree) ht.get(folderID);
				CatalogTree tmp = findMyAncestor(me, i, folderIDs, ht);// ??
			}

			// ������ľ���root.
			if (folderIDs.size() > 0) {
				root = (CatalogTree) ht.get((Long) folderIDs.get(0));
			}
			// root.getID()>0 �Ͳ��Ǹ�Ŀ¼,����û�����ø�Ŀ¼������.
		} catch (SQLException e) {
			System.out.println(e);
		}

		return root;
	}

	/**
	 * 
	 * @param father
	 *            ��Ŀ¼
	 * @param name
	 *            ��ǰĿ¼����
	 * @param dirName
	 *            ��ǰĿ¼��Ӧ���ļ�Ŀ¼����
	 * @param fileName
	 *            ��ǰĿ¼��Ӧ���ļ�����.
	 */

	private CatalogTree(CatalogTree father, Catalog catalog) {
		this.father = father;
		this.setID(catalog.getID());
		this.setName(catalog.getName());
		this.setFatherID(catalog.getFatherID());
		this.setFolder(catalog.getFolder());
		this.setAttribute(catalog.getAttribute());
		this.sons = Collections.synchronizedSet(new TreeSet<CatalogTree>()); // ȷ��ͬ������;
																				// ��TreeSet
																				// ������������˳�������,������˳��.
		if (father != null) {
			father.addSon(this);
		}
	}

	/**
	 * ���ڴ��й���һ����.
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
	 * ��ָ���ĸ�Ŀ¼��,����һ����Ŀ¼,�����ݿ��д���. ����ʹ���¼�������. �ڴ���Ŀ¼��ʱ��; ��ɾ��Ŀ¼��ʱ��.
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
	 * ͨ����ǰ������Ŀ¼��������.
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
				if (obj != null) // ��ֹ����. ֻ��һ����������.
				{
					break;
				}
			}
		}

		return obj;
	}

	// ��ȡ��ǰĿ¼����������Ŀ¼ȫ·��. ��󲻺���/
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

	// ��ȡ��ǰĿ¼������Ŀ¼ȫ·��.
	public String getSystemFullDirPath(String systemDirOfRoot) {
		if (systemDirOfRoot == null) {
			System.out.println(" systemFullDirPath is null.");
			return null;
		}
		String virtualFullDirPath = "";
		CatalogTree tmp = this;
		while (true) {
			// ��ͬ�Ĳ���ϵͳ���صĽ���ǲ�һ����.
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
	 * ת��ΪHTML OPTION
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
	 * ת��ΪHTML OPTION
	 * 
	 * @param selectedID
	 *            int
	 * @return String
	 */
	public String toJsonArray(int selectedID) {
		StringBuffer info = new StringBuffer();
		String selectedValue= (this.getID() == selectedID) ? "1" : "0";
		info.append("{\"tid\":\"" + this.getID() + "\",\"tn\":"
					+ "\""+ this.getOptionCatalogPath() + "\",\"ts\":\""+selectedValue+"\"},\n");//1��ʾѡ����.
		CatalogTree tmp = null;
		Iterator<CatalogTree> itor = this.getSons().iterator();
		while (itor.hasNext()) {
			tmp = (CatalogTree) itor.next();
			info.append(tmp.toJsonArray(selectedID));
		}
		return info.toString();
	}

	/**
	 * //��Ŀ¼������������.
	 * 
	 * @param ctv
	 * @param catalogTreeID
	 *            Ŀ¼��ID
	 * @param newName
	 *            Ŀ¼����
	 * @param NewFolder
	 *            Ŀ¼������ļ���
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
		// ��Ҫ���Ǹ����ڴ����.
		if (isOK) {
			this.setName(newName);
			this.setFolder(newFolder);
		}
		return isOK;
	}

	// ��Ŀ¼������������.
	public boolean rename(CatalogTreeView ctv, long catalogTreeID,
			String newName) {
		return rename(ctv, catalogTreeID, newName, this.getFolder());
	}
}
