package org.phoebus.applications.queueserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.Preferences;
import org.phoebus.applications.queueserver.client.*;
import org.phoebus.applications.queueserver.client.ApiEndpoint;
import org.phoebus.applications.queueserver.client.RunEngineHttpClient;
import org.phoebus.applications.queueserver.client.RunEngineHttpClient.*;

        import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public final class RunEngineRepl {

    private static final String PROMPT   = "> ";
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        /* ---- bootstrap client ---- */
        RunEngineHttpClient.initialize(Preferences.queue_server_url, Preferences.api_key);
        var client  = RunEngineHttpClient.get();
        var console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("""
                REPL – type an endpoint (HELP for list), optional JSON body.
                Type EXIT or QUIT to leave. Press Enter on an empty line to re-prompt.
                """);

        while (true) {
            System.out.print(PROMPT);
            String line = console.readLine();

            if (line == null) break;                                   // EOF (Ctrl-D)
            if (line.isBlank()) continue;                              // re-prompt
            if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) break;

            /* ---- HELP ---- */
            if ("help".equalsIgnoreCase(line)) {
                Arrays.stream(ApiEndpoint.values())
                        .map(Enum::name)
                        .sorted()
                        .forEach(System.out::println);
                continue;
            }

            /* ---- endpoint + optional JSON ---- */
            String[] parts = line.split("\\s+", 2);
            ApiEndpoint ep;
            try {
                ep = ApiEndpoint.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException badName) {
                System.out.println("❌ Unknown endpoint. Type HELP for the list.");
                continue;
            }

            Object body = null;
            if (parts.length == 2) {
                try {
                    body = JSON.readValue(parts[1], Object.class);
                } catch (JsonProcessingException badJson) {
                    System.out.println("❌ Invalid JSON: " + badJson.getOriginalMessage());
                    continue;
                }
            }

            try {
                Map<String,Object> out = client.send(ep, body);
                System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(out));
            } catch (BlueskyException be) {
                System.out.println("❌ " + be.getMessage());
            } catch (Exception ex) {
                System.out.println("❌ Unexpected error: " + ex);
            }
        }
        System.out.println("Bye.");
    }
}
