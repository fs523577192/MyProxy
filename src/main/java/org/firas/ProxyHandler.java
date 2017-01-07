package org.firas;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

public class ProxyHandler {

	private static SSLContext sslContext = null;
	private HeaderMap headers;
	private boolean getFont = false;
	private String method, host, path;
	private int port = 80;
	private URI uri;
	
	public ProxyHandler(HttpServerExchange exchange) throws IOException {
		parseUrl(exchange);
		debug();
		filterGoogleApis();
		debug();
		try {
			HttpURLConnection connection = getConnection();
			Receiver fromClient = exchange.getRequestReceiver();
			fromClient.receiveFullBytes(new OutPipe(connection, getFont));
		} catch (SocketTimeoutException ex) {
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.setStatusCode(404)
				.getResponseSender()
				.send("The requested resource is not available");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("--== End ==--");
	}
	
	private static synchronized SSLContext getSslContext() {
		if (null == sslContext) {
			try {
				sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new javax.net.ssl.TrustManager[]
						{ new TrustAnyTrustManager() },
						new java.security.SecureRandom());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return sslContext;
	}
	
	private HttpURLConnection getConnection() throws Exception {
		int connectTimeout = 2000, readTimeout = 10000;
		URL url = null;
		HttpURLConnection connection = null;
		if (port == 443) {
			url = new URL("https", host, 443, path);
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			conn.setSSLSocketFactory(getSslContext().getSocketFactory());
			conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
			connection = (HttpURLConnection)conn;
		} else {
			url = new URL("http", host, port, path);
			connection = (HttpURLConnection)url.openConnection();
		}
		Iterator<HeaderValues> iterator = headers.iterator();
		while (iterator.hasNext()) {
			HeaderValues values = iterator.next();
			String name = values.getHeaderName().toString();
			String line = String.join(",", values);
			if (getFont && name.equalsIgnoreCase("accept-encoding")) {
				line = "identity";
			}
			System.out.println("[IN] " + name + ": " + line);
			connection.setRequestProperty(name, line);
		}
		connection.setDoInput(true);
		connection.setDoOutput(!method.equalsIgnoreCase("GET"));
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		connection.setUseCaches(false);
		connection.connect();
		return connection;
	}
	
	private void parseUrl(HttpServerExchange exchange) {
		headers = exchange.getRequestHeaders();
		method = exchange.getRequestMethod().toString();
		host = exchange.getHostName();
		port = exchange.getHostPort();
		path = exchange.getRequestPath();
		String query = exchange.getQueryString();
		if (null != query && query.length() > 0) {
			path = path + "?" + query;
		}
	}
	
	private void debug() {
		System.out.println(host + ":" + port);
		System.out.println(method + "  " + path);
	}
	
	private boolean filterGoogleApis() {
		if (host.equals("fonts.googleapis.com")) {
			host = "cdn.baomitu.com";
			port = 443;
			try {
				path = "/index/font_generator?url=" + URLEncoder.encode(
						"http://fonts.googleapis.com" + path, "UTF-8");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			getFont = true;
			return true;
		} else if (host.equals("ajax.googleapis.com") ||
				host.equals("cdnjs.cloudflare.com")) {
			host = "lib.baomitu.com";
			port = 80;
			path = path.replaceFirst("^/ajax/libs", "");
			return true;
		}
		return false;
	}
	
	
}