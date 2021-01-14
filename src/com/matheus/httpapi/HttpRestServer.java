package com.matheus.httpapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.matheus.httpapi.filters.HttpFilter;
import com.matheus.httpapi.request.Request;
import com.matheus.httpapi.request.RequestMethod;
import com.matheus.httpapi.response.Response;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HttpRestServer {

	private HttpServer httpServer;
	private HttpsServer httpsServer;
	private ArrayList<HttpModule> modules = new ArrayList<HttpModule>();
	private ArrayList<HttpFilter> filters = new ArrayList<HttpFilter>();

	private File folderPath;
	private String jwtKey;
	private int jwtValidTime;

	public HttpRestServer(int port) {
		try {
			System.out.println("Starting Server on port: " + port);
			httpServer = HttpServer.create(new InetSocketAddress(port), 0);
			httpServer.setExecutor(null);
			httpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HttpRestServer(int port, File jks, String JKSpassword) {
		try {
			System.out.println("Starting SSL Server on port: " + port);
			HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);
			System.out.println("Started SSL Server on port: " + port);
			SSLContext sslContext = SSLContext.getInstance("TLS");

			// initialise the keystore
			char[] password = JKSpassword.toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			FileInputStream fis = new FileInputStream(jks);
			ks.load(fis, password);

			// setup the key manager factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				public void configure(HttpsParameters params) {
					try {
						// initialise the SSL context
						SSLContext context = getSSLContext();
						SSLEngine engine = context.createSSLEngine();
						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// Set the SSL parameters
						SSLParameters sslParameters = context.getSupportedSSLParameters();
						params.setSSLParameters(sslParameters);

					} catch (Exception ex) {
						System.out.println("Failed to create HTTPS port");
					}
				}
			});
			httpsServer = server;
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}

	}

	public void setJWTKey(String key) {
		this.jwtKey = key;
	}

	public void setJWTValidTime(int minutes) {
		this.jwtValidTime = minutes;
	}

	public void addFilter(HttpFilter filter) {
		filters.add(filter);
	}

	public void addModule(HttpModule module) {
		System.out.println("Registering module on context: " + module.getContext());
		HashMap<String, Integer> params = new HashMap<String, Integer>();
		String finalContext = module.getContext();

		if (module.getContext().contains(":")) {
			String currentParam = "";
			boolean start = false;
			int index = 0;
			int charIndex = 1;
			char[] chars = module.getContext().toCharArray();
			for (char c : chars) {
				if (c == ':') {
					start = true;
				} else if (start && c == '/') {
					start = false;
					params.put(currentParam, index);
					finalContext = finalContext.replace(":" + currentParam + (c == '/' ? "/" : ""), "");
					currentParam = "";
					index++;
				} else if (start && charIndex == chars.length) {
					start = false;
					currentParam += c;
					params.put(currentParam, index);
					finalContext = finalContext.replace(":" + currentParam + (c == '/' ? "/" : ""), "");
					currentParam = "";
					index++;
				} else if (start) {
					currentParam += c;
				}
				charIndex++;
			}
		}

		createContext(finalContext, (exchange -> {
			try {

				HashMap<String, String> cookies = new HashMap<String, String>();
				if (exchange.getRequestHeaders().containsKey("Cookie")) {
					for (String cookie : exchange.getRequestHeaders().get("Cookie")) {
						if (cookie.split("=").length > 1) {
							String cookie_name = cookie.split("=", 2)[0];
							String cookie_value = cookie.split("=", 2)[1];
							cookies.put(cookie_name, cookie_value);
						}
					}
				}

				Request req = new Request(jwtKey, jwtValidTime, exchange, cookies, params);
				Response res = new Response(exchange, this.folderPath);

				for (HttpFilter filter : filters) {
					if (!filter.doFilter(req, res)) {
						return;
					}
				}

				for (HttpFilter filter : module.getFilters()) {
					if (!filter.doFilter(req, res)) {
						return;
					}
				}

				if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
					for (HttpFilter filter : module.getMethodFilters(RequestMethod.GET)) {
						if (!filter.doFilter(req, res)) {
							return;
						}
					}
					module.get(req, res);
				} else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
					for (HttpFilter filter : module.getMethodFilters(RequestMethod.POST)) {
						if (!filter.doFilter(req, res)) {
							return;
						}
					}
					module.post(req, res);
				} else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
					for (HttpFilter filter : module.getMethodFilters(RequestMethod.PUT)) {
						if (!filter.doFilter(req, res)) {
							return;
						}
					}
					module.put(req, res);
				} else if (exchange.getRequestMethod().equalsIgnoreCase("UPDATE")) {
					for (HttpFilter filter : module.getMethodFilters(RequestMethod.UPDATE)) {
						if (!filter.doFilter(req, res)) {
							return;
						}
					}
					module.update(req, res);
				} else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
					for (HttpFilter filter : module.getMethodFilters(RequestMethod.DELETE)) {
						if (!filter.doFilter(req, res)) {
							return;
						}
					}
					module.delete(req, res);
				}

			} catch (Exception e) {
				System.out.println("Error on module: " + module.getContext());
				e.printStackTrace();
			}
		}));
		modules.add(module);
	}

	// Will be registered to /public
	public void registerPublicFileDirectory(String basePath, File folderPath) {
		this.folderPath = folderPath;
		createContext(basePath, (exchange -> {
			String fileId = exchange.getRequestURI().getPath().replace("/public", "");
			File file = new File(folderPath.getAbsolutePath() + fileId);
			if (!file.exists()) {
				String response = "Error 404 File not found.";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream output = exchange.getResponseBody();
				output.write(response.getBytes());
				output.flush();
				output.close();
			} else {
				exchange.sendResponseHeaders(200, 0);
				OutputStream output = exchange.getResponseBody();
				FileInputStream fs = new FileInputStream(file);
				final byte[] buffer = new byte[0x10000];
				int count = 0;
				while ((count = fs.read(buffer)) >= 0) {
					output.write(buffer, 0, count);
				}
				output.flush();
				output.close();
				fs.close();
			}
		}));
	}

	private void createContext(String path, HttpHandler handler) {

		if (httpServer != null) {
			httpServer.createContext(path, handler);
		} else if (httpsServer != null) {
			httpsServer.createContext(path, handler);
		}

	}

	public void stopServer() {
		if (httpServer != null) {
			httpServer.stop(0);
		} else if (httpsServer != null) {
			httpsServer.stop(0);
		}
		for (HttpModule m : modules) {
			m.disable();
		}
	}

}
