package com.matheus.httpapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.matheus.httpapi.filters.HttpFilter;
import com.matheus.httpapi.request.Request;
import com.matheus.httpapi.request.RequestMethod;
import com.matheus.httpapi.response.Response;
import com.matheus.httpapi.response.ResponseCode;

public abstract class HttpModule {

	private String context;
	private ArrayList<HttpFilter> filters = new ArrayList<HttpFilter>();
	private HashMap<RequestMethod, ArrayList<HttpFilter>> methodFilter = new HashMap<RequestMethod, ArrayList<HttpFilter>>();

	public HttpModule(String context) {
		this.context = context;
	}

	public void get(Request req, Response res) {
		res.sendCode(ResponseCode.NOT_FOUND);
	}

	public void post(Request req, Response res) {
		res.sendCode(ResponseCode.NOT_FOUND);
	}

	public void put(Request req, Response res) {
		res.sendCode(ResponseCode.NOT_FOUND);
	}

	public void update(Request req, Response res) {
		res.sendCode(ResponseCode.NOT_FOUND);
	}

	public void delete(Request req, Response res) {
		res.sendCode(ResponseCode.NOT_FOUND);
	}

	public void addMethodFilter(RequestMethod method, HttpFilter filter) {
		if (methodFilter.containsKey(method)) {
			methodFilter.get(method).add(filter);
			return;
		}
		methodFilter.put(method, new ArrayList<HttpFilter>(Arrays.asList(filter)));
	}

	public ArrayList<HttpFilter> getMethodFilters(RequestMethod method) {
		return methodFilter.containsKey(method) ? methodFilter.get(method) : new ArrayList<>();
	}

	public ArrayList<HttpFilter> getFilters() {
		return filters;
	}

	public void addFilter(HttpFilter filter) {
		filters.add(filter);
	}

	public void disable() {

	}

	public String getContext() {
		return context;
	}

}
