/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.nio.ByteBuffer;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAString;

/** Encode CMD_MESSAGE from server to client */
public class MessageRequest implements RequestEncoder
{
	private final int request_id;
	private final PVAStatus.Type type;
	private final String message;
	
	/** @param request_id ID of client request to which this message applies
	 *  @param type Message type
	 *  @param message Message text
	 */
	public MessageRequest(final int request_id, final PVAStatus.Type type, final String message)
	{
		this.request_id = request_id;
		this.type = type;
		this.message = message;
	}
	
	@Override
	public void encodeRequest(byte version, ByteBuffer buffer) throws Exception
	{
		final int payload = 4 + 1 + PVAString.getEncodedSize(message);
		PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_MESSAGE, payload);
		buffer.putInt(request_id);
		buffer.put((byte) type.ordinal());
		PVAString.encodeString(message, buffer);
	}
}
