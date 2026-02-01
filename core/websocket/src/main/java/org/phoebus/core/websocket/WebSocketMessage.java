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

package org.phoebus.core.websocket;

/**
 * Record encapsulating a {@link MessageType} and a payload of arbitrary type.
 * <p>
 *     The deserialization process of a web socket message into a concrete {@link WebSocketMessage} must be
 *     delegated to a custom deserializer.
 * </p>
 * @param messageType The {@link MessageType} of a web socket message. Apps can implement as needed.
 * @param payload The payload like a {@link String}, or something more specific for the actual use case, e.g. a
 *                logbook entry or save-and-restore object.
 */
public record WebSocketMessage<T>(MessageType messageType, T payload) {
}
