package org.firas;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLContext;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class MyProxy implements Runnable {

	public static void main(String[] args) {
        new Thread(new MyProxy()).start();
        try {
            Undertow server = Undertow.builder()
                    .addHttpsListener(65533, "0.0.0.0", getSslContext())
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(final HttpServerExchange exchange) throws Exception {
                            new ProxyHandler(exchange);
                        }
                    }).build();
            server.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	
    public void run() {
		Undertow server = Undertow.builder()
                .addHttpListener(65532, "0.0.0.0")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        new ProxyHandler(exchange);
                    }
                }).build();
        server.start();
	}

    private static SSLContext getSslContext() throws IOException {
        return MySslContext.getSslContext(System.getProperty("cert_file"),
                "PKCS12", "");
    }
}
