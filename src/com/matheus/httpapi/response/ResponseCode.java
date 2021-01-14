package com.matheus.httpapi.response;

public enum ResponseCode {

	NOT_FOUND(404), SUCCESS(200), BAD_REQUEST(400), REDIRECTED(302);

	int code;

	public int getCode() {
		return code;
	}

	ResponseCode(int code) {
		this.code = code;
	}

}
