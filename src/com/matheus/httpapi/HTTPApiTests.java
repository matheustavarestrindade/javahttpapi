package com.matheus.httpapi;

import com.google.gson.JsonObject;

public class HTTPApiTests {

	public static void main(String[] args) {

		HttpJWTContent content = new HttpJWTContent("", "testeKey", 10);

		JsonObject obj = new JsonObject();
		obj.addProperty("teste", "testeValue");

		content.setJWTInfo(obj);
		content.createJWTToken();

	}

}
