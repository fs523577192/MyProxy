package org.firas;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TrustAnyTrustManager implements X509TrustManager {
	
    static TrustManager[] MANAGER_ARRAY = new TrustManager[] {
            new TrustAnyTrustManager()
    };

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		
	}
	
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		
	}
	
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[] {};
	}
}
