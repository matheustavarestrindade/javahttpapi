package com.matheus.httpapi.request;

public enum RequestMethod {

	POST("POST"), GET("GET"), PUT("PUT"), DELETE("DELETE"), UPDATE("UPDATE");

	private final String name;

	private RequestMethod(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}

	@Override
	public String toString() {
		return this.name;
	}

}
