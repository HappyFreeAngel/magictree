package com.happyfreeangel.magictree;

/**
 * <p>Title: ħ��</p>
 * <p>Description: ħ��</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author happy
 * @version 0.1
 */

import java.util.Collection;

public interface Tree {
	public Tree getFather();

	public void setFather(Tree father);

	public Collection getSons();

	public boolean isRoot(); // �ж��Ƿ�������

	public boolean isLeaf(); // �ж��Ƿ�����Ҷ

	public boolean removeSon(Tree son);

	public boolean addSon(Tree son);

	public boolean isOffspring(Tree who); // �ж��Ƿ���who�ĺ��,���ӣ����ӣ����ӵĶ��ӣ����ӵ�����,..
}
