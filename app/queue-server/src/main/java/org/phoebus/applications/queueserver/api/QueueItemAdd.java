// src/main/java/com/jbi/api/AddQueueItemRequest.java
package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueItemAdd(
        @JsonProperty("item") Item item,
        String user,
        @JsonProperty("user_group") String userGroup) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            @JsonProperty("item_type") String itemType,
            String name,
            List<Object> args,
            Map<String, Object> kwargs
    ) {

        public static Item from(QueueItem qi) {
            return new Item(
                    qi.itemType(),
                    qi.name(),
                    qi.args(),
                    qi.kwargs());
        }
    }
}
