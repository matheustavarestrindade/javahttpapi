package com.matheus.httpapi.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.matheus.httpapi.HttpJWTContent;
import com.matheus.httpapi.HttpMultipart;
import com.matheus.httpapi.HttpMultipartType;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class Request {

	private HttpJWTContent session;
	private HashMap<String, String> cookies;
	private Map<String, List<String>> queryParameters = new HashMap<String, List<String>>();
	private Map<String, HttpMultipart> multipartParameters = new HashMap<String, HttpMultipart>();
	private HttpExchange exchange;
	private HashMap<String, Integer> params;
	private String jwtKey;
	private int jwtTime;

	public Request(String jwtKey, int jwtTime, HttpExchange exchange, HashMap<String, String> cookies,
																HashMap<String, Integer> params) {
		this.jwtKey = jwtKey;
		this.jwtTime = jwtTime;
		String query = null;
		this.exchange = exchange;
		this.params = params;
		if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
			if (exchange.getRequestURI().getRawQuery() != null) {
				query = exchange.getRequestURI().getRawQuery();
			}
		} else if (exchange.getRequestBody() != null) {
			Headers headers = exchange.getRequestHeaders();
			String contentType = headers.getFirst("Content-Type");
			if (contentType.startsWith("multipart/form-data")) {
				// found form data
				String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
				// as of rfc7578 - prepend "\r\n--"
				byte[] boundaryBytes = ("\r\n--" + boundary).getBytes(Charset.forName("UTF-8"));
				byte[] payload = getInputAsBinary(exchange.getRequestBody());

				List<Integer> offsets = searchBytes(payload, boundaryBytes, 0, payload.length - 1);
				for (int idx = 0; idx < offsets.size(); idx++) {
					int startPart = offsets.get(idx);
					int endPart = payload.length;
					if (idx < offsets.size() - 1) {
						endPart = offsets.get(idx + 1);
					}
					byte[] part = Arrays.copyOfRange(payload, startPart, endPart);
					// look for header
					int headerEnd = indexOf(part, "\r\n\r\n".getBytes(Charset.forName("UTF-8")), 0, part.length - 1);
					if (headerEnd > 0) {

						HttpMultipartType type;
						String name = "";
						String filename = "";
						String multiPartContentType = "";
						String value = "";
						byte[] bytes = null;

						byte[] head = Arrays.copyOfRange(part, 0, headerEnd);
						String header = new String(head);
						// extract name from header
						int nameIndex = header.indexOf("\r\nContent-Disposition: form-data; name=");
						if (nameIndex >= 0) {
							int startMarker = nameIndex + 39;
							// check for extra filename field
							int fileNameStart = header.indexOf("; filename=");
							if (fileNameStart >= 0) {
								filename = header.substring(fileNameStart + 11, header.indexOf("\r\n", fileNameStart));
								filename = filename.replace('"', ' ').replace('\'', ' ').trim();
								name = header.substring(startMarker, fileNameStart).replace('"', ' ').replace('\'', ' ').trim();
								type = HttpMultipartType.FILE;
							} else {
								int endMarker = header.indexOf("\r\n", startMarker);
								if (endMarker == -1) endMarker = header.length();
								name = header.substring(startMarker, endMarker).replace('"', ' ').replace('\'', ' ').trim();
								type = HttpMultipartType.TEXT;
							}
						} else {
							// skip entry if no name is found
							continue;
						}
						// extract content type from header
						int typeIndex = header.indexOf("\r\nContent-Type:");
						if (typeIndex >= 0) {
							int startMarker = typeIndex + 15;
							int endMarker = header.indexOf("\r\n", startMarker);
							if (endMarker == -1) endMarker = header.length();
							multiPartContentType = header.substring(startMarker, endMarker).trim();
						}

						// handle content
						if (type == HttpMultipartType.TEXT) {
							// extract text value
							byte[] body = Arrays.copyOfRange(part, headerEnd + 4, part.length);
							value = new String(body);
						} else {
							// must be a file upload
							bytes = Arrays.copyOfRange(part, headerEnd + 4, part.length);
						}
						multipartParameters.put(name, new HttpMultipart(type, multiPartContentType, name, filename, value, bytes));
					}
				}
			} else {
				StringBuilder sb = new StringBuilder();
				InputStream ios = exchange.getRequestBody();
				int i;
				try {
					while ((i = ios.read()) != -1) {
						sb.append((char) i);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				query = sb.toString();
			}
		}
		Map<String, List<String>> splited = splitQuery(query);
		if (splited != null) {
			queryParameters.putAll(splited);
		}
		this.cookies = cookies;
	}

	public HttpJWTContent getJWTAuth() {
		String jwtTOKEN = exchange.getRequestHeaders().containsKey("Authorization") ? exchange.getRequestHeaders().getFirst("Authorization") : "";
		session = new HttpJWTContent(jwtTOKEN, jwtKey, jwtTime);
		return session;
	}

	public String getCookie(String key) {
		if (!cookies.containsKey(key)) {
			return null;
		}
		return cookies.get(key);
	}

	public void enableCORS() {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept");
	}

	public boolean existURLParameter(String key) {
		if (params.containsKey(key)) {
			int index = params.get(key);
			if (exchange.getRequestURI().toString().split("/").length > index) {
				return true;
			}
		}
		return false;
	}

	public String getRequestURI() {
		return exchange.getRequestURI().toString();
	}

	public String getURLParameter(String key) {
		if (params.containsKey(key)) {
			int index = params.get(key);
			if (exchange.getRequestURI().toString().split("/").length > (index + 1)) {
				return exchange.getRequestURI().toString().split("/")[index + 1];
			}
		}
		return "";
	}

	public boolean existMultipartParameter(String key) {
		return multipartParameters.containsKey(key);
	}

	public HttpMultipart getMultipartParameter(String key) {
		return multipartParameters.get(key);
	}

	public String getQueryParameter(String key) {
		return queryParameters.get(key).get(0);
	}

	public boolean existQueryParameter(String key) {
		return queryParameters.containsKey(key) && queryParameters.get(key).size() > 0;
	}

	private static Map<String, List<String>> splitQuery(String query) {
		try {
			if (query == null) {
				return new HashMap<String, List<String>>();
			}
			final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
			final String[] pairs = query.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
				query_pairs.get(key).add(value);
			}
			return query_pairs;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] getInputAsBinary(InputStream requestStream) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[100000];
			int bytesRead = 0;
			while ((bytesRead = requestStream.read(buf)) != -1) {
				// while (requestStream.available() > 0) {
				// int i = requestStream.read(buf);
				bos.write(buf, 0, bytesRead);
			}
			requestStream.close();
			bos.close();
		} catch (IOException e) {
			System.out.println("error while decoding http input stream");
			e.printStackTrace();
		}
		return bos.toByteArray();
	}

	/**
	 * Search bytes in byte array returns indexes within this byte-array of all
	 * occurrences of the specified(search bytes) byte array in the specified range
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes
	 * @param searchBytes
	 * @param searchStartIndex
	 * @param searchEndIndex
	 * @return result index list
	 */
	public static List<Integer> searchBytes(byte[] srcBytes, byte[] searchBytes, int searchStartIndex, int searchEndIndex) {
		final int destSize = searchBytes.length;
		final List<Integer> positionIndexList = new ArrayList<Integer>();
		int cursor = searchStartIndex;
		while (cursor < searchEndIndex + 1) {
			int index = indexOf(srcBytes, searchBytes, cursor, searchEndIndex);
			if (index >= 0) {
				positionIndexList.add(index);
				cursor = index + destSize;
			} else {
				cursor++;
			}
		}
		return positionIndexList;
	}

	/**
	 * Returns the index within this byte-array of the first occurrence of the
	 * specified(search bytes) byte array.<br>
	 * Starting the search at the specified index, and end at the specified index.
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes
	 * @param searchBytes
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public static int indexOf(byte[] srcBytes, byte[] searchBytes, int startIndex, int endIndex) {
		if (searchBytes.length == 0 || (endIndex - startIndex + 1) < searchBytes.length) {
			return -1;
		}
		int maxScanStartPosIdx = srcBytes.length - searchBytes.length;
		final int loopEndIdx;
		if (endIndex < maxScanStartPosIdx) {
			loopEndIdx = endIndex;
		} else {
			loopEndIdx = maxScanStartPosIdx;
		}
		int lastScanIdx = -1;
		label: // goto label
		for (int i = startIndex; i <= loopEndIdx; i++) {
			for (int j = 0; j < searchBytes.length; j++) {
				if (srcBytes[i + j] != searchBytes[j]) {
					continue label;
				}
				lastScanIdx = i + j;
			}
			if (endIndex < lastScanIdx || lastScanIdx - i + 1 < searchBytes.length) {
				// it becomes more than the last index
				// or less than the number of search bytes
				return -1;
			}
			return i;
		}
		return -1;
	}

}
