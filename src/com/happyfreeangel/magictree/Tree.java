package com.happyfreeangel.magictree;

/**
 * <p>Title: 魔树</p>
 * <p>Description: 魔树</p>
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

	public boolean isRoot(); // 判断是否是树根

	public boolean isLeaf(); // 判断是否是树叶

	public boolean removeSon(Tree son);

	public boolean addSon(Tree son);

	public boolean isOffspring(Tree who); // 判断是否是who的后代,儿子，孙子，孙子的儿子，孙子的孙子,..
}
