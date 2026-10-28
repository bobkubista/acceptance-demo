package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppTest {

  private App app;
  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void start() throws Exception {
    app = App.start(0, "1.2.3");
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  private HttpResponse<String> get(String path) throws Exception {
    URI uri = URI.create("http://localhost:" + app.port() + path);
    return client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void rootIsAnEndOfTheInternetPage() throws Exception {
    HttpResponse<String> response = get("/");

    assertEquals(200, response.statusCode());
    assertEquals(
        "text/html; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
    assertTrue(response.body().startsWith("<!doctype html>"), response.body());
    assertTrue(response.body().contains("<h1>End of the internet</h1>"), response.body());
  }

  @Test
  void versionTellsWhichReleaseRuns() throws Exception {
    HttpResponse<String> response = get("/version");

    assertEquals(200, response.statusCode());
    assertEquals("acceptance-demo 1.2.3\n", response.body());
  }

  @Test
  void healthEndpointsAnswerUp() throws Exception {
    for (String path : new String[] {"/actuator/health/readiness", "/actuator/health/liveness"}) {
      HttpResponse<String> response = get(path);
      assertEquals(200, response.statusCode(), path);
      assertTrue(response.body().contains("\"UP\""), path);
    }
  }

  @Test
  void unknownPathIsNotFound() throws Exception {
    assertEquals(404, get("/nope").statusCode());
  }
}
