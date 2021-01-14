package com.matheus.httpapi;

import java.security.Key;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.JoseException;

import com.google.gson.JsonObject;

public class HttpJWTContent {

	private String JWTEncoded;
	private JsonObject JWTContent;
	private boolean validSession = false;
	private String jwtKey;
	private int jwtTime;
	
	public HttpJWTContent(String sessionJWT, String jwtKey, int jwtTime) {
		this.JWTEncoded = sessionJWT;
		this.jwtKey = jwtKey;
		this.jwtTime = jwtTime;
		if (sessionJWT.length() == 0 || sessionJWT.split(" ").length < 2) {
			return;
		}
		// Decode session info from JWT

	}

	public String createJWTToken() {
		try {
			Key key = new AesKey(jwtKey.getBytes());
			JsonWebEncryption jwe = new JsonWebEncryption();
			jwe.setPayload(JWTContent.toString());
			jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
			jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
			jwe.setKey(key);
			String serializedJwe = jwe.getCompactSerialization();
			System.out.println("Serialized Encrypted JWE: " + serializedJwe);
			jwe = new JsonWebEncryption();
			jwe.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.PERMIT, KeyManagementAlgorithmIdentifiers.A128KW));
			jwe.setContentEncryptionAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.PERMIT, ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
			jwe.setKey(key);
			jwe.setCompactSerialization(serializedJwe);
			System.out.println("Payload: " + jwe.getPayload());
		} catch (JoseException e) {
			e.printStackTrace();
		}

		return "";
	}

	public boolean isJWTValid() {
		return validSession;
	}

	public void setJWTInfo(JsonObject sessionContent) {
		// Encode header

	}

}
