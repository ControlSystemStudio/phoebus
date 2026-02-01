/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import org.phoebus.core.websocket.WebSocketMessage;


import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class WebSocketService {

    @SuppressWarnings("unused")
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @SuppressWarnings("unused")
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @SuppressWarnings("unused")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private String contextPath;

    private static final Logger logger = Logger.getLogger(WebSocketService.class.getName());

    /**
     * @param webSocketMessage Non-null {@link WebSocketMessage}, will be converted to a JSON string before
     *                        it is dispatched to clients.
     */
    public void sendMessageToClients(@NonNull WebSocketMessage<?> webSocketMessage) {
        try {
            String message = objectMapper.writeValueAsString(webSocketMessage);
            String messageEndpoint = contextPath.length() > 0 ? contextPath : "";
            simpMessagingTemplate.convertAndSend(messageEndpoint + "/web-socket/messages", message);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Failed to write web socket message to json string", e);
        }
    }
}
