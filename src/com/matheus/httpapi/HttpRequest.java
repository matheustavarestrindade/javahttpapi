package com.matheus.httpapi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;

import com.matheus.httpapi.request.RequestMethod;

public class HttpRequest {

	private String link;
	private String method;

	boolean hasParameters = false;
	private String requestParameters;
	private Proxy proxy;
	private boolean useProxy;

	private boolean showStackTrace = true;

	private HashMap<String, String> headers = new HashMap<String, String>();

	public HttpRequest(String url) {
		this.link = url;
		setMethod(RequestMethod.GET);
	}

	public void useProxy(HttpProxy p) {
		useProxy = true;
		proxy = p.getProxy();
	}

	public void setMethod(RequestMethod method) {
		this.method = method.name();
	}

	public void setRequestPrameters(String parameters) {
		this.requestParameters = parameters;
		this.hasParameters = true;
	}

	public void addHeader(String header, String value) {
		this.headers.put(header, value);
	}

	public void setJsonRequestParameters(String json) {
		this.requestParameters = json;
		this.hasParameters = true;
		addHeader("Content-Type", "application/json; charset=UTF-8");
	}

	public void showStackTrace(boolean show) {
		showStackTrace = show;
	}

	public String execute() {
		HttpURLConnection connection = getConnection();
		if (connection == null) {
			System.out.println("Cannot open connection with: " + link);
			return null;
		}
		try {
			connection.setRequestMethod(method);
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);

			if (!headers.isEmpty()) {

				for (Entry<String, String> entry : headers.entrySet()) {
					connection.setRequestProperty(entry.getKey(), entry.getValue());
				}

			}
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Content-Language", "en-US");

			byte[] requestBytes = null;

			if (hasParameters) {
				requestBytes = requestParameters.getBytes(StandardCharsets.UTF_8);
				connection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
			}
			connection.setUseCaches(false);

			// Send request
			if (hasParameters) {
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.write(requestBytes);
				wr.close();
			}

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
			}
			rd.close();

			return response.toString();
		} catch (Exception e) {
			if (showStackTrace) {
				e.printStackTrace();
			}
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private HttpURLConnection getConnection() {
		URL url;
		try {
			url = new URL(link);
			if (useProxy) {
				return (HttpURLConnection) url.openConnection(proxy);
			} else {
				return (HttpURLConnection) url.openConnection();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
