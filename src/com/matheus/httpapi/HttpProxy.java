package com.matheus.httpapi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matheus.httpapi.request.RequestMethod;

public class HttpProxy {

	private InetSocketAddress address;
	private HttpProxyType type = HttpProxyType.HTTP;
	private String proxyIP;

	public HttpProxy() {
		HttpURLConnection connection = null;

		try {

			URL url = new URL("https://gimmeproxy.com/api/getProxy?post=true&supportsHttps=true&maxCheckPeriod=3600");
			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod(RequestMethod.GET.name());
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			connection.setRequestProperty("Content-Language", "en-US");
			connection.setRequestProperty("Accept-Encoding", "identity");
			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
			}
			rd.close();

			JsonObject res = new JsonParser().parse(response.toString()).getAsJsonObject();
			proxyIP = res.get("ip").getAsString();
			int port = Integer.parseInt(res.get("port").getAsString());
			System.out.println("USING PROXY IP> " + proxyIP);
			System.out.println("USING PROXY PORT> " + port);

			address = new InetSocketAddress(proxyIP, port);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public HttpProxy(String ip, int port) {
		proxyIP = ip;
		address = new InetSocketAddress(proxyIP, port);
	}

	public void setProxyType(HttpProxyType type) {
		this.type = type;
	}

	public boolean isProxyValid() {
		HttpRequest req = new HttpRequest("http://api.ipify.org");
		req.useProxy(this);
		req.showStackTrace(false);
		String res = req.execute();
		return res != null && res.equalsIgnoreCase(proxyIP);
	}

	public Proxy getProxy() {
		return new Proxy(type.getType(), address);
	}

}
