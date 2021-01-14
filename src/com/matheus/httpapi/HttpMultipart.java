package com.matheus.httpapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class HttpMultipart {
	private HttpMultipartType type;
	private String contentType;
	private String name;
	private String filename;
	private String value;
	private byte[] bytes;

	public HttpMultipart(HttpMultipartType type, String contentType, String name, String filename, String value,
																byte[] bytes) {
		this.type = type;
		this.contentType = contentType;
		this.name = name;
		this.filename = filename;
		this.value = value;
		this.bytes = bytes;
	}

	public String getContentType() {
		return contentType;
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		return filename;
	}

	public String getValue() {
		return value;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getFileExtension() {
		if (getFileName().length() < 1) {
			return "";
		}
		String extension = "";
		int i = getFileName().lastIndexOf('.');
		if (i > 0) {
			extension = getFileName().substring(i + 1);
		}
		return extension;
	}

	public HttpMultipartType getType() {
		return type;
	}

	public File getFile(Path destination) {
		InputStream is = new ByteArrayInputStream(bytes);
		try {
			Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new File(destination.toString());
	}

}
