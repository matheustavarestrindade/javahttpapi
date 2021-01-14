package com.matheus.httpapi;

import java.util.HashMap;

public class HttpPageRender {

	private String filePath;
	private HashMap<String, String> content = new HashMap<String, String>();
	private HashMap<String, String> subPages = new HashMap<String, String>();

	public HttpPageRender(String filePath) {
		this.filePath = filePath;
	}

	public void addSubPageRender(String variable, String filePath) {
		subPages.put(variable, filePath);
	}

	public void setParameter(String name, String content) {
		this.content.put(name, content);
	}

	public HashMap<String, String> getSubPages() {
		return subPages;
	}

	public HashMap<String, String> getParameters() {
		return content;
	}

	public String getFilePath() {
		return filePath;
	}

}
