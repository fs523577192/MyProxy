package org.firas;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

class ProxyHandler {

	private static SSLContext sslContext = null;
	private HeaderMap headers;
	private boolean getFont = false;
	private String method, host, path;
	private int port = 80;
	private URI uri;
	
	ProxyHandler(HttpServerExchange exchange) {
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
		debug("--== End ==--");
	}
	
	private static synchronized SSLContext getSslContext() {
		if (null == sslContext) {
			try {
				sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, TrustAnyTrustManager.MANAGER_ARRAY,
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
		for (HeaderValues values : headers) {
			String name = values.getHeaderName().toString();
			String line = String.join(",", values);
			if (getFont && name.equalsIgnoreCase("accept-encoding")) {
				line = "identity";
			}
			debug("[IN] " + name + ": " + line);
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
	
	private static final SimpleDateFormat formatter =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private void debug(String str) {
		System.out.println('[' + formatter.format(new Date()) + "] " + str);
	}

	private void debug() {
		System.out.println(host + ":" + port);
		System.out.println(method + "  " + path);
	}
	
	private boolean filterGoogleApis() {
        debug("Host: " + host);
        boolean changeHost = "true".equals(
                System.getProperty("change_host"));
		if ((changeHost && path.indexOf("/ajax/libs") == 0) ||
                "ajax.googleapis.com".equals(host) ||
				"cdnjs.cloudflare.com".equals(host)) {
			host = "lib.baomitu.com";
			port = 80;
			path = path.replaceFirst("^/ajax/libs", "");
			return true;
		} else if (changeHost || "fonts.googleapis.com".equals(host)) {
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
		}
		return false;
	}
	
	
}
