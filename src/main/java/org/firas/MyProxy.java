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

    private short httpPort;
    private MyProxy(short httpPort) {
        this.httpPort = httpPort;
    }

	public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("  java -jar MyProxy.jar 80 443 cert.pfx");
            return;
        }
        try {
            short httpPort = Short.parseShort(args[0]);
            short httpsPort = Short.parseShort(args[1]);

            new Thread(new MyProxy(httpPort)).start();
            
            SSLContext sslContext = MySslContext.getSslContext(args[2],
                    "PKCS12", "");
            Undertow server = Undertow.builder()
                    .addHttpsListener(httpsPort, "0.0.0.0", sslContext)
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(final HttpServerExchange exchange) throws Exception {
                            new ProxyHandler(exchange);
                        }
                    }).build();
            server.start();
        } catch (NumberFormatException ex) {
            System.out.println("Usage:");
            System.out.println("  java -jar MyProxy.jar 80 443 cert.pfx");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	
    public void run() {
		Undertow server = Undertow.builder()
                .addHttpListener(httpPort, "0.0.0.0")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        new ProxyHandler(exchange);
                    }
                }).build();
        server.start();
	}

}
