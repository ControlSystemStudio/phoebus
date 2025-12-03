package org.phoebus.applications.queueserver.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.api.*;
import org.phoebus.applications.queueserver.api.Envelope;
import org.phoebus.applications.queueserver.api.EverythingElse.Arbitrary;
import org.phoebus.applications.queueserver.api.NoBody;
import org.phoebus.applications.queueserver.util.HttpSupport;
import org.phoebus.applications.queueserver.util.RateLimiter;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RunEngineHttpClient {

    private static volatile RunEngineHttpClient INSTANCE;

    // Default 10 req/sec
    public static void initialize(String baseUrl, String apiKey) {
        initialize(baseUrl, apiKey, 10.0);
    }
    public static void initialize(String baseUrl, String apiKey, double permitsPerSecond) {
        if (INSTANCE == null) {
            synchronized (RunEngineHttpClient.class) {
                if (INSTANCE == null)
                    INSTANCE = new RunEngineHttpClient(baseUrl, apiKey, permitsPerSecond);
            }
        }
    }
    public static RunEngineHttpClient get() {
        if (INSTANCE == null) throw new IllegalStateException("RunEngineHttpClient not initialised");
        return INSTANCE;
    }

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);;
    private final String base;
    private final String apiKey;
    private final RateLimiter limiter;
    private static final Logger logger = HttpSupport.logger;

    private RunEngineHttpClient(String baseUrl, String apiKey, double permitsPerSecond) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.base    = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey  = apiKey;
        this.limiter = new RateLimiter(permitsPerSecond);
    }

    // getters
    public String getBaseUrl() { return base; }
    public String getApiKey() { return apiKey; }
    public HttpClient httpClient() { return http; }

    public Envelope<?> call(ApiEndpoint ep, Object body) throws Exception {
        Object requestBody = (body == NoBody.INSTANCE) ? null : body;
        return send(ep, requestBody, new TypeReference<Envelope<Arbitrary>>() {});
    }

    // raw JSON (Map) for CLI / REPL
    public Map<String, Object> send(ApiEndpoint api, Object body) throws Exception {
        return send(api, body, new TypeReference<Map<String, Object>>() {});
    }

    public <T> T send(ApiEndpoint api, Object body, Class<T> type) throws Exception {
        HttpRequest req = build(api.endpoint(), body);
        return executeWithRetry(req, api, rsp ->
        {
            try {
                return (type == Void.class) ? null : mapper.readValue(rsp.body(), type);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> T send(ApiEndpoint api, Object body, TypeReference<T> ref) throws Exception {
        HttpRequest req = build(api.endpoint(), body);
        return executeWithRetry(req, api, rsp -> {
            try { return mapper.readValue(rsp.body(), ref); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    <T> T send(ApiEndpoint api, Object body, Class<T> type, String extraQuery) throws Exception {
        Endpoint ep = api.endpoint();
        String path = ep.path() + (extraQuery == null ? "" : extraQuery);
        HttpRequest req = build(new Endpoint(ep.method(), path), body);
        return executeWithRetry(req, api, rsp -> {
            try {
                return mapper.readValue(rsp.body(), type);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> T executeWithRetry(HttpRequest req, ApiEndpoint ep,
                                   Function<HttpResponse<String>, T> reader) throws Exception {
        int  attempt = 0;
        long back   = HttpSupport.INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            limiter.acquire();
            long t0 = System.nanoTime();
            try {
                logger.log(Level.FINEST, ep + " attempt " + attempt);
                HttpResponse<String> rsp = http.send(req, HttpResponse.BodyHandlers.ofString());
                logger.log(Level.FINEST, ep + " " + rsp.statusCode() + " " + HttpSupport.elapsed(t0) + " ms");
                check(rsp, ep);
                return reader.apply(rsp);
            } catch (java.io.IOException ex) {
                if (!HttpSupport.isRetryable(req) || attempt >= HttpSupport.MAX_RETRIES) throw ex;
                logger.log(Level.WARNING, ep + " transport error (" + ex.getClass().getSimpleName() +
                        "), retry in " + back + " ms (attempt " + attempt + ")");
                Thread.sleep(back);
                back = Math.round(back * HttpSupport.BACKOFF_MULTIPLIER);
            }
        }
    }

    private HttpRequest build(Endpoint ep, Object body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(base + ep.path()))
                .header("Authorization", "ApiKey " + apiKey)
                .header("Content-Type", "application/json");

        return switch (ep.method()) {
            case GET    -> b.GET().build();
            case DELETE -> b.DELETE().build();
            case POST, PUT -> b.method(ep.method().name(),
                            (body == null)
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
        };
    }

    private void check(HttpResponse<String> rsp, ApiEndpoint api) throws Exception {
        if (rsp.statusCode() >= 200 && rsp.statusCode() < 300) {
            try {
                Map<String,Object> map = mapper.readValue(
                        rsp.body(), new TypeReference<>() {});
                Object s = map.get("success");
                if (Boolean.FALSE.equals(s)) {
                    String msg = String.valueOf(map.getOrDefault("msg", "(no msg)"));
                    throw new RequestFailedException(api, msg);
                }
            } catch (JsonProcessingException ignore) {
                /* response isn't a generic object, that's fine (e.g. plain "OK") */
            }
        } else if (rsp.statusCode() < 500) {
            throw new ClientErrorException(api, rsp);
        } else {
            throw new ServerErrorException(api, rsp);
        }
    }


    public sealed static class BlueskyException extends RuntimeException
            permits ClientErrorException, ServerErrorException, RequestFailedException {
        BlueskyException(String m) { super(m); }
    }
    public static final class ClientErrorException extends BlueskyException {
        ClientErrorException(ApiEndpoint api, HttpResponse<?> rsp) {
            super(api + " → " + rsp.statusCode() + " " + rsp.body());
        }
    }
    public static final class ServerErrorException extends BlueskyException {
        ServerErrorException(ApiEndpoint api, HttpResponse<?> rsp) {
            super(api + " → server error " + rsp.statusCode());
        }
    }
    public static final class RequestFailedException extends BlueskyException {
        RequestFailedException(ApiEndpoint api, String msg) {
            super(api + " → " + msg);
        }
    }
}
