package org.phoebus.applications.queueserver.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.api.*;
import org.phoebus.applications.queueserver.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exhaustive façade: one method per QueueServer REST endpoint.
 *
 * <p>Each method blocks, calls {@link RunEngineHttpClient#call(ApiEndpoint, Object)},
 * and returns the raw {@link Envelope}.  Pass {@code null} or {@link NoBody#INSTANCE}
 * when no JSON request body is needed.  The envelope’s {@code payload()} is
 * usually a {@code Map&lt;String,Object&gt;} (typed DTOs exist only for STATUS
 * and QUEUE_GET).</p>
 *
 * <p>All errors propagate as {@link RunEngineHttpClient.BlueskyException} or
 * {@link java.io.IOException} / {@link java.net.http.HttpTimeoutException}.</p>
 */
public final class RunEngineService {

    private final RunEngineHttpClient http = RunEngineHttpClient.get();
    private static final Logger logger = Logger.getLogger(RunEngineService.class.getPackageName());

    /* ---- Ping & status --------------------------------------------------- */

    public Envelope<?>          ping()                       throws Exception { return http.call(ApiEndpoint.PING,            NoBody.INSTANCE); }
    public StatusResponse status() throws Exception {
        logger.log(Level.FINEST, "Fetching status");
        return http.send(ApiEndpoint.STATUS, NoBody.INSTANCE, StatusResponse.class);
    }
    public Envelope<?>          configGet()                  throws Exception { return http.call(ApiEndpoint.CONFIG_GET,      NoBody.INSTANCE); }

    /* ───────── Queue – typed helpers ───────── */

    public QueueGetPayload queueGetTyped() throws Exception {
        return http.send(ApiEndpoint.QUEUE_GET, NoBody.INSTANCE, QueueGetPayload.class);
    }

    public HistoryGetPayload historyGetTyped() throws Exception {
        return http.send(ApiEndpoint.HISTORY_GET, NoBody.INSTANCE, HistoryGetPayload.class);
    }

    /* ---- Queue control --------------------------------------------------- */

    public Envelope<?> queueStart()          throws Exception { return http.call(ApiEndpoint.QUEUE_START,      NoBody.INSTANCE); }
    public Envelope<?> queueStop()           throws Exception { return http.call(ApiEndpoint.QUEUE_STOP,       NoBody.INSTANCE); }
    public Envelope<?> queueStopCancel()     throws Exception { return http.call(ApiEndpoint.QUEUE_STOP_CANCEL,NoBody.INSTANCE); }
    public Envelope<?> queueGet()            throws Exception { return http.call(ApiEndpoint.QUEUE_GET,        NoBody.INSTANCE); }
    public Envelope<?> queueClear()          throws Exception { return http.call(ApiEndpoint.QUEUE_CLEAR,      NoBody.INSTANCE); }
    public Envelope<?> queueAutostart(Object body) throws Exception { return http.call(ApiEndpoint.QUEUE_AUTOSTART, body); }
    public Envelope<?> queueAutostart(boolean enable) throws Exception { return queueAutostart(Map.of("enable", enable)); }
    public Envelope<?> queueModeSet(Object body)   throws Exception { return http.call(ApiEndpoint.QUEUE_MODE_SET,  body); }

    /* ---- single-item add ------------------------------------------------ */

    public Envelope<?> queueItemAdd(Object body)   throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_ADD, body); }

    public Envelope<?> queueItemAdd(QueueItem item,
                                    String   user,
                                    String   group) throws Exception {

        QueueItemAdd req = new QueueItemAdd(QueueItemAdd.Item.from(item), user, group);
        return queueItemAdd(req);
    }

    public Envelope<?> queueItemAdd(QueueItem item) throws Exception {
        return queueItemAdd(item, "GUI Client", "primary");
    }

    /* ---- move helpers --------------------------------------------------- */

    public void moveSingle(QueueItemMove dto)       throws Exception {
        http.call(ApiEndpoint.QUEUE_ITEM_MOVE, dto);
    }
    public void moveSingle(String uid, String ref, boolean before) throws Exception {
        moveSingle(before ? QueueItemMove.before(uid, ref)
                : QueueItemMove.after (uid, ref));
    }

    public void moveBatch(QueueItemMoveBatch dto)   throws Exception {
        http.call(ApiEndpoint.QUEUE_ITEM_MOVE_BATCH, dto);
    }
    public void moveBatch(List<String> uids, String ref, boolean before) throws Exception {
        moveBatch(before ? QueueItemMoveBatch.before(uids, ref)
                : QueueItemMoveBatch.after (uids, ref));
    }

    /* ---- duplicate helper ---------------------------------------------- */

    public String addAfter(QueueItem item, String afterUid) throws Exception {
        QueueItemAdd req = new QueueItemAdd(QueueItemAdd.Item.from(item),
                "GUI Client", "primary");
        Map<String,Object> body = Map.of(
                "item",       req.item(),
                "after_uid",  afterUid,
                "user",       req.user(),
                "user_group", req.userGroup());

        Envelope<?> env = queueItemAdd(body);

        Object payload = env.payload();
        if (payload instanceof Map<?,?> p &&
                p.get("item") instanceof Map<?,?> m &&
                m.get("item_uid") != null) {
            return m.get("item_uid").toString();
        }
        return null;
    }

    /* ---- batch-add helpers --------------------------------------------- */

    public Envelope<?> queueItemAddBatch(Object body) throws Exception {
        return http.call(ApiEndpoint.QUEUE_ITEM_ADD_BATCH, body);
    }

    public Envelope<?> queueItemAddBatch(QueueItemAddBatch req) throws Exception {
        return http.call(ApiEndpoint.QUEUE_ITEM_ADD_BATCH, req);
    }

    public Envelope<?> queueItemAddBatch(List<QueueItem> items,
                                         String user,
                                         String group) throws Exception {
        return queueItemAddBatch(new QueueItemAddBatch(items, user, group));
    }

    public Envelope<?>          queueItemAddBatch(QueueItemMoveBatch body ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_ADD_BATCH, body); }
    public Envelope<?>          queueItemGet(Object params ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_GET,  params); }
    public Envelope<?>          queueItemUpdate(Object b   ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_UPDATE, b); }

    public Envelope<?>          queueItemUpdate(QueueItem item) throws Exception {
        Map<String, Object> updateRequest = Map.of(
                "item", Map.of(
                        "item_type", item.itemType(),
                        "name", item.name(),
                        "args", item.args(),
                        "kwargs", item.kwargs(),
                        "item_uid", item.itemUid()
                ),
                "user", item.user() != null ? item.user() : "GUI Client",
                "user_group", item.userGroup() != null ? item.userGroup() : "primary",
                "replace", true
        );
        return queueItemUpdate(updateRequest);
    }
    public Envelope<?>          queueItemRemove(Object b   ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_REMOVE, b); }
    public Envelope<?>          queueItemRemoveBatch(Object b) throws Exception {return http.call(ApiEndpoint.QUEUE_ITEM_REMOVE_BATCH,b); }
    public Envelope<?>          queueItemMove(Object b     ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_MOVE,   b); }
    public Envelope<?>          queueItemMove(QueueItemMove body) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_MOVE,   body); }
    public Envelope<?>          queueItemMoveBatch(Object b) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_MOVE_BATCH,b);}
    public Envelope<?>          queueItemExecute(Object b  ) throws Exception { return http.call(ApiEndpoint.QUEUE_ITEM_EXECUTE,b); }

    /* ---- History --------------------------------------------------------- */

    public Envelope<?>          historyGet()                 throws Exception { return http.call(ApiEndpoint.HISTORY_GET,      NoBody.INSTANCE); }
    public Envelope<?>          historyClear()               throws Exception { return http.call(ApiEndpoint.HISTORY_CLEAR,    NoBody.INSTANCE); }


    /* ───────── Console monitor ───────── */

    public InputStream streamConsoleOutput() throws Exception {
        logger.log(Level.FINE, "Opening console output stream");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(http.getBaseUrl() + ApiEndpoint.STREAM_CONSOLE_OUTPUT.endpoint().path()))
                .header("Authorization", "ApiKey " + http.getApiKey())
                .GET()
                .build();
        // no retry/limiting – you open it once and keep reading
        HttpResponse<InputStream> rsp =
                http.httpClient().send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (rsp.statusCode() < 200 || rsp.statusCode() >= 300) {
            logger.log(Level.WARNING, "Console stream failed with HTTP " + rsp.statusCode());
            throw new IOException("console stream - HTTP " + rsp.statusCode());
        }
        logger.log(Level.FINE, "Console output stream opened successfully");
        return rsp.body();
    }

    public ConsoleOutputText consoleOutput(int nLines) throws Exception {
        String q = "?nlines=" + nLines;
        return http.send(ApiEndpoint.CONSOLE_OUTPUT, null,
                ConsoleOutputText.class, q);
    }

    public ConsoleOutputUid consoleOutputUid() throws Exception {
        return http.send(ApiEndpoint.CONSOLE_OUTPUT_UID, null, ConsoleOutputUid.class);
    }

    private static final ObjectMapper JSON = new ObjectMapper();


    public ConsoleOutputUpdate consoleOutputUpdate(String lastUid) throws Exception {
        String bodyJson = JSON.writeValueAsString(Map.of("last_msg_uid", lastUid));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        http.getBaseUrl() +
                                ApiEndpoint.CONSOLE_OUTPUT_UPDATE
                                        .endpoint()
                                        .path()))
                .header("Authorization", "ApiKey " + http.getApiKey())
                .header("Content-Type",  "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> rsp =
                http.httpClient().send(req, HttpResponse.BodyHandlers.ofString());

        if (rsp.statusCode() < 200 || rsp.statusCode() >= 300) {
            throw new IOException("console_output_update – HTTP "
                    + rsp.statusCode() + " – " + rsp.body());
        }

        return JSON.readValue(rsp.body(), ConsoleOutputUpdate.class);
    }



    /* ---- Environment ----------------------------------------------------- */

    public Envelope<?>          environmentOpen()            throws Exception { return http.call(ApiEndpoint.ENVIRONMENT_OPEN,   NoBody.INSTANCE); }
    public Envelope<?>          environmentClose()           throws Exception { return http.call(ApiEndpoint.ENVIRONMENT_CLOSE,  NoBody.INSTANCE); }
    public Envelope<?>          environmentDestroy()         throws Exception { return http.call(ApiEndpoint.ENVIRONMENT_DESTROY,NoBody.INSTANCE); }
    public Envelope<?>          environmentUpdate(Object b)  throws Exception { return http.call(ApiEndpoint.ENVIRONMENT_UPDATE, b); }

    /* ---- Run Engine control --------------------------------------------- */

    public Envelope<?>          rePause()                    throws Exception { return http.call(ApiEndpoint.RE_PAUSE,  NoBody.INSTANCE); }
    public Envelope<?> rePause(String option) throws Exception {
        if (option == null || option.isBlank())
            option = "deferred";                       // QS default
        return http.call(ApiEndpoint.RE_PAUSE, Map.of("option", option));
    }

    public Envelope<?>          reResume()                   throws Exception { return http.call(ApiEndpoint.RE_RESUME, NoBody.INSTANCE); }
    public Envelope<?>          reStop()                     throws Exception { return http.call(ApiEndpoint.RE_STOP,   NoBody.INSTANCE); }
    public Envelope<?>          reAbort()                    throws Exception { return http.call(ApiEndpoint.RE_ABORT,  NoBody.INSTANCE); }
    public Envelope<?>          reHalt()                     throws Exception { return http.call(ApiEndpoint.RE_HALT,   NoBody.INSTANCE); }
    public Envelope<?>          reRuns(Object body)          throws Exception { return http.call(ApiEndpoint.RE_RUNS,   body); }

    /* ---- Permissions & allowed lists ------------------------------------ */

    public Envelope<?>          plansAllowed()               throws Exception { return http.call(ApiEndpoint.PLANS_ALLOWED,   NoBody.INSTANCE); }
    public Map<String, Object>  plansAllowedRaw()           throws Exception {
        logger.log(Level.FINE, "Fetching plans allowed (raw)");
        return http.send(ApiEndpoint.PLANS_ALLOWED,   NoBody.INSTANCE);
    }
    public Envelope<?>          devicesAllowed()             throws Exception { return http.call(ApiEndpoint.DEVICES_ALLOWED, NoBody.INSTANCE); }
    public Envelope<?>          plansExisting()              throws Exception { return http.call(ApiEndpoint.PLANS_EXISTING,  NoBody.INSTANCE); }
    public Envelope<?>          devicesExisting()            throws Exception { return http.call(ApiEndpoint.DEVICES_EXISTING,NoBody.INSTANCE); }
    public Envelope<?>          permissionsReload()          throws Exception { return http.call(ApiEndpoint.PERMISSIONS_RELOAD,NoBody.INSTANCE); }
    public Envelope<?>          permissionsGet()             throws Exception { return http.call(ApiEndpoint.PERMISSIONS_GET,   NoBody.INSTANCE); }
    public Envelope<?>          permissionsSet(Object body ) throws Exception { return http.call(ApiEndpoint.PERMISSIONS_SET,   body); }

    /* ---- Script / function ---------------------------------------------- */

    public Envelope<?>          scriptUpload(Object body  )  throws Exception { return http.call(ApiEndpoint.SCRIPT_UPLOAD,     body); }
    public Envelope<?>          functionExecute(Object b )   throws Exception { return http.call(ApiEndpoint.FUNCTION_EXECUTE,  b); }

    /* ---- Tasks ----------------------------------------------------------- */

    public Envelope<?>          taskStatus(Object params)    throws Exception { return http.call(ApiEndpoint.TASK_STATUS, params); }
    public Envelope<?>          taskResult(Object params)    throws Exception { return http.call(ApiEndpoint.TASK_RESULT, params); }

    /* ---- Locks ----------------------------------------------------------- */

    public Envelope<?>          lock(Object body   )         throws Exception { return http.call(ApiEndpoint.LOCK,   body); }
    public Envelope<?>          unlock()                     throws Exception { return http.call(ApiEndpoint.UNLOCK, NoBody.INSTANCE); }
    public Envelope<?>          lockInfo()                   throws Exception { return http.call(ApiEndpoint.LOCK_INFO, NoBody.INSTANCE); }

    /* ---- Kernel / Manager ------------------------------------------------ */

    public Envelope<?>          kernelInterrupt()            throws Exception { return http.call(ApiEndpoint.KERNEL_INTERRUPT, Map.of("interrupt_task", true)); }
    public Envelope<?>          managerStop()                throws Exception { return http.call(ApiEndpoint.MANAGER_STOP,     NoBody.INSTANCE); }
    public Envelope<?>          managerKill()                throws Exception { return http.call(ApiEndpoint.MANAGER_KILL,     NoBody.INSTANCE); }

    /* ---- Auth ------------------------------------------------------------ */

    public Envelope<?>          sessionRefresh(Object b )    throws Exception { return http.call(ApiEndpoint.SESSION_REFRESH, b); }
    public Envelope<?>          apikeyNew     (Object body ) throws Exception { return http.call(ApiEndpoint.APIKEY_NEW,     body); }
    public Envelope<?>          apikeyInfo()                  throws Exception { return http.call(ApiEndpoint.APIKEY_INFO,    NoBody.INSTANCE); }
    public Envelope<?>          apikeyDelete  (Object params) throws Exception { return http.call(ApiEndpoint.APIKEY_DELETE,  params); }
    public Envelope<?>          whoAmI()                      throws Exception { return http.call(ApiEndpoint.WHOAMI,        NoBody.INSTANCE); }
    public Envelope<?>          apiScopes()                   throws Exception { return http.call(ApiEndpoint.API_SCOPES,    NoBody.INSTANCE); }
    public Envelope<?>          logout()                      throws Exception { return http.call(ApiEndpoint.LOGOUT,       NoBody.INSTANCE); }

    /* ---- WebSockets ------------------------------------------------------ */

    /**
     * Create a WebSocket connection to the console output stream.
     * Messages are streamed in real-time as {"time": timestamp, "msg": text}.
     *
     * @return a WebSocket client that can be connected and listened to
     */
    public QueueServerWebSocket<ConsoleOutputWsMessage> createConsoleOutputWebSocket() {
        String wsUrl = http.getBaseUrl().replace("http://", "ws://").replace("https://", "wss://")
                + "/api/console_output/ws";
        return new QueueServerWebSocket<>(wsUrl, http.getApiKey(), ConsoleOutputWsMessage.class);
    }

    /**
     * Create a WebSocket connection to the status stream.
     * Status messages are sent each time status is updated at RE Manager or at least once per second.
     * Messages are formatted as {"time": timestamp, "msg": {"status": {...}}}.
     *
     * @return a WebSocket client that can be connected and listened to
     */
    public QueueServerWebSocket<StatusWsMessage> createStatusWebSocket() {
        String wsUrl = http.getBaseUrl().replace("http://", "ws://").replace("https://", "wss://")
                + "/api/status/ws";
        return new QueueServerWebSocket<>(wsUrl, http.getApiKey(), StatusWsMessage.class);
    }

    /**
     * Create a WebSocket connection to the system info stream.
     * Info stream includes status messages and potentially other system messages.
     * Messages are formatted as {"time": timestamp, "msg": {msg-class: msg-content}}.
     *
     * @return a WebSocket client that can be connected and listened to
     */
    public QueueServerWebSocket<SystemInfoWsMessage> createSystemInfoWebSocket() {
        String wsUrl = http.getBaseUrl().replace("http://", "ws://").replace("https://", "wss://")
                + "/api/info/ws";
        return new QueueServerWebSocket<>(wsUrl, http.getApiKey(), SystemInfoWsMessage.class);
    }
}