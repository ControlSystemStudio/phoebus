package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusResponse(

        String msg,

        @JsonProperty("items_in_queue")           int     itemsInQueue,
        @JsonProperty("items_in_history")         int     itemsInHistory,
        @JsonProperty("running_item_uid")         String  runningItemUid,
        @JsonProperty("manager_state")            String  managerState,
        @JsonProperty("queue_stop_pending")       boolean queueStopPending,
        @JsonProperty("queue_autostart_enabled")  boolean queueAutostartEnabled,
        @JsonProperty("worker_environment_exists") boolean workerEnvironmentExists,
        @JsonProperty("worker_environment_state") String  workerEnvironmentState,
        @JsonProperty("worker_background_tasks")  int     workerBackgroundTasks,
        @JsonProperty("re_state")                 String  reState,
        @JsonProperty("ip_kernel_state")          String  ipKernelState,
        @JsonProperty("ip_kernel_captured")       Boolean ipKernelCaptured,
        @JsonProperty("pause_pending")            boolean pausePending,
        @JsonProperty("run_list_uid")             String  runListUid,
        @JsonProperty("plan_queue_uid")           String  planQueueUid,
        @JsonProperty("plan_history_uid")         String  planHistoryUid,
        @JsonProperty("devices_existing_uid")     String  devicesExistingUid,
        @JsonProperty("plans_existing_uid")       String  plansExistingUid,
        @JsonProperty("devices_allowed_uid")      String  devicesAllowedUid,
        @JsonProperty("plans_allowed_uid")        String  plansAllowedUid,
        @JsonProperty("task_results_uid")         String  taskResultsUid,
        @JsonProperty("lock_info_uid")            String  lockInfoUid,
        @JsonProperty("plan_queue_mode")          PlanQueueMode planQueueMode,
        @JsonProperty("lock")                     LockInfo      lock
)
{
    public record LockInfo(
            boolean environment,
            boolean queue) {}

    public record PlanQueueMode(
            boolean loop,
            @JsonProperty("ignore_failures") boolean ignoreFailures) {}
}
