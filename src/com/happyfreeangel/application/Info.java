package com.happyfreeangel.application;

/**
 * <p>Title: ħ��</p>
 * <p>Description: ħ��</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: happyfreeangel</p>
 * @author not attributable
 * @version 0.1
 */
import java.sql.*;

public class Info {
	private int infoID; // int not null primary key,#��ϢID
	private String title; // varchar(255) not null,#��Ϣ����
	private int infoType; // int not null,#��Ϣ����
	private String content; // text, #��Ϣ����
	private String author; // varchar(255),#����
	private int format; // int ,#��Ϣ�ĵ����͸�ʽ
	private int languageType; // int, #��Ϣ��������
	private Timestamp publishedDate; // datetime not null,#��������
	private Timestamp expiredDate; // datetime ;//not null,#��������
	private String keywords; // varchar(255),#�ؼ���
	private String derivation; // varchar(255),#����
	private String sourceURI; // varchar(255),#������������
	private String whoCollectIt; // varchar(255) not null,#˭�ռ���
	private Timestamp latestUpdateTime; // datetime ;// not null, #���һ�θ���ʱ��
	private String insertedTime; // datetime ;// not null, #����ʱ��
	private int viewedCount; // int, #���鿴����
	private int latestModifiedMemeberID; // varchar(255),#���һ���޸ĵĻ�ԱID
	private int hasVerify; // int, #�Ƿ�ͨ����֤
	private int activeStatus; // varchar(255), #�״̬
	private int securityLevel; // int;//, #��ȫ����
	private int isAuthorship; // int;// #�Ƿ���ԭ��
	private String virtualWebPath;
	private int elite;

	public int getElite() {
		return elite;
	}

	public void setElite(int elite) {
		this.elite = elite;
	}

	public String getVirtualWebPath() {
		return virtualWebPath;
	}

	public void setVirtualWebPath(String virtualWebPath) {
		this.virtualWebPath = virtualWebPath;
	}

	public Info() {
		super();
	}

	public static Info findByID(int infoID) {
		return null;
	}

	public int getActiveStatus() {
		return activeStatus;
	}

	public String getAuthor() {
		return author;
	}

	public String getContent() {
		return content;
	}

	public String getDerivation() {
		return derivation;
	}

	public Timestamp getExpiredDate() {
		return expiredDate;
	}

	public int getFormat() {
		return format;
	}

	public int getHasVerify() {
		return hasVerify;
	}

	public int getInfoID() {
		return infoID;
	}

	public int getInfoType() {
		return infoType;
	}

	public String getInsertedTime() {
		return insertedTime;
	}

	public int getIsAuthorship() {
		return isAuthorship;
	}

	public String getKeywords() {
		return keywords;
	}

	public int getLanguageType() {
		return languageType;
	}

	public int getLatestModifiedMemeberID() {
		return latestModifiedMemeberID;
	}

	public Timestamp getLatestUpdateTime() {
		return latestUpdateTime;
	}

	public Timestamp getPublishedDate() {
		return publishedDate;
	}

	public int getSecurityLevel() {
		return securityLevel;
	}

	public String getSourceURI() {
		return sourceURI;
	}

	public String getTitle() {
		return title;
	}

	public int getViewedCount() {
		return viewedCount;
	}

	public String getWhoCollectIt() {
		return whoCollectIt;
	}

	public void setActiveStatus(int activeStatus) {
		this.activeStatus = activeStatus;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setDerivation(String derivation) {
		this.derivation = derivation;
	}

	public void setExpiredDate(Timestamp expiredDate) {
		this.expiredDate = expiredDate;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public void setHasVerify(int hasVerify) {
		this.hasVerify = hasVerify;
	}

	public void setInfoID(int infoID) {
		this.infoID = infoID;
	}

	public void setInfoType(int infoType) {
		this.infoType = infoType;
	}

	public void setInsertedTime(String insertedTime) {
		this.insertedTime = insertedTime;
	}

	public void setIsAuthorship(int isAuthorship) {
		this.isAuthorship = isAuthorship;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public void setLanguageType(int languageType) {
		this.languageType = languageType;
	}

	public void setLatestModifiedMemeberID(int latestModifiedMemeberID) {
		this.latestModifiedMemeberID = latestModifiedMemeberID;
	}

	public void setLatestUpdateTime(Timestamp latestUpdateTime) {
		this.latestUpdateTime = latestUpdateTime;
	}

	public void setPublishedDate(Timestamp publishedDate) {
		this.publishedDate = publishedDate;
	}

	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}

	public void setSourceURI(String sourceURI) {
		this.sourceURI = sourceURI;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setViewedCount(int viewedCount) {
		this.viewedCount = viewedCount;
	}

	public void setWhoCollectIt(String whoCollectIt) {
		this.whoCollectIt = whoCollectIt;
	}
}
