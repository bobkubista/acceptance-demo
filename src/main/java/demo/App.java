package demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * The smallest app that exercises the delivery loop: a page that says End of the internet (public
 * on bobkubista.com, ADR-0030 in kvm-server), and {@code /version} says which release runs.
 */
public final class App {

  private static final String PAGE =
      """
      <!doctype html>
      <html lang="en">
      <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>bobkubista.com</title>
      </head>
      <body>
      <h1>End of the internet</h1>
      </body>
      </html>
      """;

  private final HttpServer server;

  private App(HttpServer server) {
    this.server = server;
  }

  /** Starts the app on {@code port} (0 = any free port), reporting {@code version}. */
  public static App start(int port, String version) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", exchange -> handle(exchange, version));
    server.start();
    return new App(server);
  }

  private static void handle(HttpExchange exchange, String version) throws IOException {
    String path = exchange.getRequestURI().getPath();
    switch (path) {
      case "/" -> send(exchange, 200, "text/html; charset=utf-8", PAGE);
      case "/version" ->
          send(exchange, 200, "text/plain; charset=utf-8", "acceptance-demo " + version + "\n");
      case "/actuator/health/readiness", "/actuator/health/liveness" ->
          send(exchange, 200, "application/json", "{\"status\":\"UP\"}");
      default -> send(exchange, 404, "text/plain", "not found\n");
    }
  }

  private static void send(HttpExchange exchange, int status, String type, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  public int port() {
    return server.getAddress().getPort();
  }

  public void stop() {
    server.stop(0);
  }

  public static void main(String[] args) throws IOException {
    String version = System.getenv().getOrDefault("APP_VERSION", "unknown");
    start(8080, version);
  }
}
