package com.happyfreeangel.application;

/**
 * <p>Title: Ä§Ê÷</p>
 * <p>Description: Ä§Ê÷</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author not attributable
 * @version 0.1
 */
import com.happyfreeangel.magictree.*;

public interface HTMLMaker {

	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, int infoID, StringBuffer model);

	public String dirToHTMLForUserView(CatalogTree t, CatalogTreeView ctv,
			StringBuffer model, StringBuffer dirModel);

	public String safeUpdateTotalInfoToHTMLForUserView(CatalogTree ct,
			CatalogTreeView ctv, StringBuffer model, StringBuffer dirModel,
			boolean updateInfo, boolean updateDir);

	public void infoToHTMLForUserView(CatalogTree currentInfoCatalogTree,
			CatalogTreeView ctv, Info info, StringBuffer model);

}
