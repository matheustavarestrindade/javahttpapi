package com.matheus.httpapi.response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.matheus.httpapi.HttpPageRender;
import com.sun.net.httpserver.HttpExchange;

public class Response {

	private HttpExchange exchange;
	private File basePath;
	private ResponseEncoding encoding;

	public Response(HttpExchange exchange, File basePath) {
		this.exchange = exchange;
		this.basePath = basePath;
	}

	public void redirect(String context) {
		exchange.getResponseHeaders().add("Location", context);
		sendCode(ResponseCode.REDIRECTED);
	}

	public void setEncoding(ResponseEncoding encoding) {
		this.encoding = encoding;
	}

	public void setJWTContent(String token) {
		exchange.getRequestHeaders().add("Authorization", "Bearer " + token);
	}

	public void sendJson(JsonObject json) {
		sendJson(json, ResponseCode.SUCCESS);
	}

	public void sendJson(JsonObject json, ResponseCode code) {
		String response = json.toString();
		try {
			byte[] bytes = null;
			if (encoding != null && encoding.equals(ResponseEncoding.JSON_UTF_8)) {
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
				bytes = response.getBytes(StandardCharsets.UTF_8);
			} else {
				bytes = response.getBytes();
			}
			exchange.sendResponseHeaders(code.getCode(), bytes.length);
			OutputStream output = exchange.getResponseBody();
			output.write(bytes);
			output.flush();
			exchange.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendText(String response) {
		sendText(response, ResponseCode.SUCCESS);
	}

	public void sendText(String response, ResponseCode code) {
		try {
			byte[] bytes = null;
			if (encoding != null && encoding.equals(ResponseEncoding.JSON_UTF_8)) {
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
				bytes = response.getBytes(StandardCharsets.UTF_8);
			} else {
				bytes = response.getBytes();
			}
			exchange.sendResponseHeaders(code.getCode(), bytes.length);
			OutputStream output = exchange.getResponseBody();
			output.write(bytes);
			output.flush();
			exchange.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void renderPage(HttpPageRender render) {
		if (basePath == null) {
			System.out.println("Cannot render page: " + render.getFilePath());
			System.out.println("Base path does not exist!");
			return;
		}
		File f = new File(basePath.getAbsolutePath() + File.separator + render.getFilePath());
		if (!f.exists()) {
			System.out.println("Cannot render page: " + render.getFilePath());
			System.out.println("File does not exist!");
			return;
		}

		HashMap<String, String> subPagesHtml = new HashMap<>();

		for (Entry<String, String> subPages : render.getSubPages().entrySet()) {
			File subPage = new File(basePath.getAbsolutePath() + File.separator + subPages.getValue());
			if (!subPage.exists()) {
				System.out.println("Cannot render SubPage: " + subPages.getValue());
				System.out.println("File does not exist!");
				return;
			}
			try {
				FileReader fr = new FileReader(subPage);
				BufferedReader br = new BufferedReader(fr);
				StringBuilder htmlBuilder = new StringBuilder(1024);
				String s = "";
				while ((s = br.readLine()) != null) {
					htmlBuilder.append(s);
				}
				br.close();
				String html = htmlBuilder.toString();
				subPagesHtml.put(subPages.getKey(), html);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		try {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			StringBuilder htmlBuilder = new StringBuilder(1024);
			String s = "";
			while ((s = br.readLine()) != null) {
				htmlBuilder.append(s);
			}
			br.close();
			String html = htmlBuilder.toString();

			for (Entry<String, String> subPageHtml : subPagesHtml.entrySet()) {
				html = html.replace("${" + subPageHtml.getKey() + "}", subPageHtml.getValue().toString());
			}

			for (Entry<String, String> content : render.getParameters().entrySet()) {
				html = html.replace("${" + content.getKey() + "}", content.getValue());
			}

			sendText(html, ResponseCode.SUCCESS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCode(ResponseCode code) {
		try {
			exchange.sendResponseHeaders(code.getCode(), 0);
			exchange.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
