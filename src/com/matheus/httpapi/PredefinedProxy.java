package com.matheus.httpapi;

public class PredefinedProxy {

	private String ip;
	private int port;

	public int getPort() {
		return port;
	}

	public String getIp() {
		return ip;
	}

	public PredefinedProxy(String ip, int port) {
		this.port = port;
		this.ip = ip;
	}

}