package com.matheus.httpapi;

import java.net.Proxy;

public enum HttpProxyType {

	HTTP(Proxy.Type.HTTP), DIRECT(Proxy.Type.DIRECT), SOCKS(Proxy.Type.SOCKS);

	Proxy.Type type;

	HttpProxyType(Proxy.Type type) {
		this.type = type;
	}

	public Proxy.Type getType() {
		return type;
	}

}
