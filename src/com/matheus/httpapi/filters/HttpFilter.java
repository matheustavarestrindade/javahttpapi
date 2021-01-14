package com.matheus.httpapi.filters;

import com.matheus.httpapi.request.Request;
import com.matheus.httpapi.response.Response;

public abstract class HttpFilter {

	private String context;

	public void Filter(String context) {
		this.context = context;
	}

	public abstract boolean doFilter(Request req, Response res);

	public String getContext() {
		return context;
	}
}
