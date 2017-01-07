package org.firas;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class MyProxy {

	public static void main(String[] args) {
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
	
}