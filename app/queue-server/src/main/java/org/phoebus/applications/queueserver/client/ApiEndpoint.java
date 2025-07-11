package org.phoebus.applications.queueserver.client;

import static org.phoebus.applications.queueserver.client.HttpMethod.*;

public enum ApiEndpoint {

    PING              (GET , "/api/ping"),
    STATUS            (GET , "/api/status"),
    CONFIG_GET        (GET , "/api/config/get"),
    QUEUE_START       (POST, "/api/queue/start"),
    QUEUE_STOP        (POST, "/api/queue/stop"),
    QUEUE_STOP_CANCEL (POST, "/api/queue/stop/cancel"),
    QUEUE_GET         (GET , "/api/queue/get"),
    QUEUE_CLEAR       (POST, "/api/queue/clear"),
    QUEUE_AUTOSTART   (POST, "/api/queue/autostart"),
    QUEUE_MODE_SET    (POST, "/api/queue/mode/set"),
    QUEUE_ITEM_ADD    (POST, "/api/queue/item/add"),
    QUEUE_ITEM_ADD_BATCH(POST,"/api/queue/item/add/batch"),
    QUEUE_ITEM_GET    (GET , "/api/queue/item/get"),
    QUEUE_ITEM_UPDATE (POST, "/api/queue/item/update"),
    QUEUE_ITEM_REMOVE (POST, "/api/queue/item/remove"),
    QUEUE_ITEM_REMOVE_BATCH(POST,"/api/queue/item/remove/batch"),
    QUEUE_ITEM_MOVE   (POST, "/api/queue/item/move"),
    QUEUE_ITEM_MOVE_BATCH(POST,"/api/queue/item/move/batch"),
    QUEUE_ITEM_EXECUTE(POST, "/api/queue/item/execute"),
    HISTORY_GET       (GET , "/api/history/get"),
    HISTORY_CLEAR     (POST, "/api/history/clear"),
    STREAM_CONSOLE_OUTPUT (GET , "/api/stream_console_output"),
    CONSOLE_OUTPUT        (GET , "/api/console_output"),
    CONSOLE_OUTPUT_UID    (GET , "/api/console_output/uid"),
    CONSOLE_OUTPUT_UPDATE (GET , "/api/console_output_update"),
    ENVIRONMENT_OPEN  (POST, "/api/environment/open"),
    ENVIRONMENT_CLOSE (POST, "/api/environment/close"),
    ENVIRONMENT_DESTROY(POST,"/api/environment/destroy"),
    ENVIRONMENT_UPDATE(POST, "/api/environment/update"),
    RE_PAUSE          (POST, "/api/re/pause"),
    RE_RESUME         (POST, "/api/re/resume"),
    RE_STOP           (POST, "/api/re/stop"),
    RE_ABORT          (POST, "/api/re/abort"),
    RE_HALT           (POST, "/api/re/halt"),
    RE_RUNS           (POST, "/api/re/runs"),
    PLANS_ALLOWED     (GET , "/api/plans/allowed"),
    DEVICES_ALLOWED   (GET , "/api/devices/allowed"),
    PLANS_EXISTING    (GET , "/api/plans/existing"),
    DEVICES_EXISTING  (GET , "/api/devices/existing"),
    PERMISSIONS_RELOAD(POST, "/api/permissions/reload"),
    PERMISSIONS_GET   (GET , "/api/permissions/get"),
    PERMISSIONS_SET   (POST, "/api/permissions/set"),
    SCRIPT_UPLOAD     (POST, "/api/script/upload"),
    FUNCTION_EXECUTE  (POST, "/api/function/execute"),
    TASK_STATUS       (GET , "/api/task/status"),
    TASK_RESULT       (GET , "/api/task/result"),
    LOCK              (POST, "/api/lock"),
    UNLOCK            (POST, "/api/unlock"),
    LOCK_INFO         (GET , "/api/lock/info"),
    KERNEL_INTERRUPT  (POST, "/api/kernel/interrupt"),
    MANAGER_STOP      (POST, "/api/manager/stop"),
    MANAGER_KILL      (POST, "/api/test/manager/kill"),
    // --- Auth ---
    SESSION_REFRESH   (POST, "/api/auth/session/refresh"),
    APIKEY_NEW        (POST, "/api/auth/apikey"),
    APIKEY_INFO       (GET , "/api/auth/apikey"),
    APIKEY_DELETE     (DELETE,"/api/auth/apikey"),
    WHOAMI            (GET , "/api/auth/whoami"),
    API_SCOPES        (GET , "/api/auth/scopes"),
    LOGOUT            (POST, "/api/auth/logout");

    private final Endpoint endpoint;
    ApiEndpoint(HttpMethod method, String path) {
        this.endpoint = new Endpoint(method, path);
    }
    public Endpoint endpoint() { return endpoint; }
}
