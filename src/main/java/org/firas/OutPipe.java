package org.firas;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import io.undertow.io.Receiver;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

public class OutPipe implements Receiver.FullBytesCallback {
	private static Charset charset = Charset.forName("UTF-8");
	private boolean getFont = false;
	private HttpURLConnection connection;
	
	public OutPipe(HttpURLConnection connection, boolean getFont) {
		this.connection = connection;
		this.getFont = getFont;
	}
	
	@Override
	public void handle(HttpServerExchange exchange, byte[] message) {
		try {
			if (connection.getDoOutput()) {
				OutputStream toServer = connection.getOutputStream();
				toServer.write(message);
				toServer.close();
			}
			pipe(exchange);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void pipe(HttpServerExchange exchange) throws IOException {
		int c;
		List<Byte> bytes = new ArrayList<Byte>();
		try {
			InputStream fromServer = connection.getInputStream();
			while (true) {
				c = fromServer.read();
				if (c < 0) break;
				bytes.add((byte)c);
			}
			fromServer.close();
			c = bytes.size();
			debug("Length: " + c);
			
			getHeaders(connection, exchange);
			
			int code = connection.getResponseCode();
			debug("Status: " + code); // For Debug
			
			exchange.setStatusCode(code);
			Sender toClient = exchange.getResponseSender();
			if (getFont) {
				String json = fromByteList(bytes);
				toClient.send(getFontData(json));
			} else {
				ByteBuffer buffer = ByteBuffer.allocate(c);
				for (Byte b : bytes) {
					buffer.put(b);
				}
				buffer.position(0);
				toClient.send(buffer);
			}
		} catch (FileNotFoundException ex) {
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.setStatusCode(404)
				.getResponseSender()
				.send("The requested resource is not available");
		}
	}

	private static final SimpleDateFormat formatter =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private void debug(String str) {
		System.out.println('[' + formatter.format(new Date()) + "] " + str);
	}

	private void getHeaders(HttpURLConnection connection,
			HttpServerExchange exchange) {
		HeaderMap clientHeaders = exchange.getResponseHeaders();
		String key = connection.getHeaderFieldKey(0);
		String value;
		if (null != key) {
			value = connection.getHeaderField(0);
			debug("[O] " + key + ": " + value);
			if (getFont && key.equalsIgnoreCase("content-type")) {
				value = "text/css";
			}
			clientHeaders.add(new HttpString(key), value);
		}
		int i = 0;
		while (true) {
			key = connection.getHeaderFieldKey(++i);
			if (null == key) break;
			value = connection.getHeaderField(i);
			debug("[O] " + key + ": " + value);
			if (getFont && key.equalsIgnoreCase("content-type")) {
				value = "text/css";
			}
			clientHeaders.add(new HttpString(key), value);
		}
		if (getFont) {
			clientHeaders.put(Headers.CACHE_CONTROL, "private, max-age=86400");
		}
	}
	
	private static String fromByteList(List<Byte> bytes) {
		int c = bytes.size();
		byte[] response = new byte[c];
		for (int i = c; i > 0; ) {
			--i;
			response[i] = bytes.get(i);
		}
		return new String(response, charset);
	}
	
	private static String getFontData(String json) {
		String pattern = "\"data\":\"";
		int index = json.indexOf(pattern);
		json = json.substring(index + pattern.length());
		for (index = 0; index >= 0; ) {
			index = json.indexOf('"', index + 1);
			if (json.charAt(index - 1) != '\\') {
				return json.substring(0, index).replace("\\n", "\n");
			}
		}
		return "";
	}

}
