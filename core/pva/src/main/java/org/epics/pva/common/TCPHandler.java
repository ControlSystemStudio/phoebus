/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.data.Hexdump;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAString;

/** Read and write TCP messages
 *
 *  <p>Waits for messages,
 *  performs basic message header check
 *  and dispatches to derived class.
 *
 *  <p>Maintains send queue.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class TCPHandler
{
    /** Protocol version used by the PVA server
     *
     *  <p>That's either 'us' when used in the server,
     *  or the server to which we're connected when
     *  used in the client
     */
    protected volatile byte server_version = PVAHeader.PVA_PROTOCOL_REVISION;

    /** Buffer size on server.
     *
     *  <p>XXX Client reduces this from the default if server
     *  reports a smaller value during connection validation?
     */
    protected volatile int server_buffer_size = PVASettings.TCP_BUFFER_SIZE;

    /** Is this the client, expecting received messages to be marked as server messages? */
    private final boolean client_mode;

    /** TCP socket to PVA peer */
    private final SocketChannel socket;

    /** Flag to indicate that 'close' was called to close the 'socket' */
    protected volatile boolean running = true;

    /** Buffer used to receive data via {@link TCPHandler#receive_thread} */
    protected ByteBuffer receive_buffer = ByteBuffer.allocate(PVASettings.EPICS_PVA_RECEIVE_BUFFER_SIZE);

    /** Buffer for assembling parts of segmented message
     *
     *  <p>Created and then grown as needed
     */
    private ByteBuffer segments = null;

    /** Buffer used to send data via {@link TCPHandler#send_thread} */
    protected final ByteBuffer send_buffer = ByteBuffer.allocate(PVASettings.EPICS_PVA_SEND_BUFFER_SIZE);

    /** Queue of items to send to peer */
    private final BlockingQueue<RequestEncoder> send_items = new LinkedBlockingQueue<>();

    /** Magic `send_items` value that asks send thread to exit */
    private static final RequestEncoder END_REQUEST = new RequestEncoder()
    {
        @Override
        public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
        {
            throw new IllegalStateException("END_REQUEST not meant to be encoded");
        }
    };

    /** Pool for sender and receiver threads */
    private static final ExecutorService thread_pool = Executors.newCachedThreadPool(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    /** Thread that runs {@link TCPHandler#receiver()} */
    private final Future<Void> receive_thread;

    /** Thread that runs {@link TCPHandler#sender()} */
    private volatile Future<Void> send_thread;

    /** Start receiving messages
     *
     *  <p>Will accept messages to be sent,
     *  but will only start sending them when the
     *  send thread is running
     *
     *  @param socket Socket to read/write
     *  @param client_mode Is this the client, expecting to receive messages from server?
     *  @see #startSender()
     */
    public TCPHandler(final SocketChannel socket, final boolean client_mode)
    {
        this.socket = socket;
        this.client_mode = client_mode;

        // Start receiving data
        receive_thread = thread_pool.submit(this::receiver);
    }

    /** Start send thread
     *
     *  <p>Must be called from just one thread,
     *  might otherwise start multiple send threads.
     *
     * @throws Exception on error
     */
    protected void startSender() throws Exception
    {
        if (send_thread == null)
            send_thread = thread_pool.submit(this::sender);
        else
            throw new Exception("Send thread already running");
    }

    /** @return Remote address of this end of the TCP socket */
    public InetSocketAddress getRemoteAddress()
    {
        return new InetSocketAddress(socket.socket().getInetAddress(), socket.socket().getPort());
    }

    /** @return Is the send queue idle/empty? */
    protected boolean isSendQueueIdle()
    {
        return send_items.isEmpty();
    }

    /** Submit item to be sent to peer
     *  @param item {@link RequestEncoder}
     *  @return <code>true</code> on success,
     *          <code>false</code> if send queue is full
     */
    public boolean submit(final RequestEncoder item)
    {
        if (send_items.offer(item))
            return true;
        logger.log(Level.WARNING, this + " send queue full");
        return false;
    }

    /** Send {@link RequestEncoder} entries off queue */
    private Void sender()
    {
        try
        {
            Thread.currentThread().setName("TCP sender " + socket.getRemoteAddress());
            logger.log(Level.FINER, Thread.currentThread().getName() + " started");
            while (true)
            {
                send_buffer.clear();
                final RequestEncoder to_send = send_items.take();
                if (to_send == END_REQUEST)
                    break;
                try
                {
                    to_send.encodeRequest(server_version, send_buffer);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, Thread.currentThread().getName() + " request encoding error", ex);
                    continue;
                }
                send_buffer.flip();
                send(send_buffer);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, Thread.currentThread().getName() + " exits because of error", ex);
        }
        logger.log(Level.FINER, Thread.currentThread().getName() + " done.");
        return null;
    }

    /** Send message
     *
     *  <p>Must only be called by outside code before
     *  the sender has been started
     *
     *  @param buffer Buffer to send
     *  @throws Exception on error
     */
    protected void send(final ByteBuffer buffer) throws Exception
    {
        logger.log(Level.FINER, () -> Thread.currentThread().getName() + ":\n" + Hexdump.toHexdump(buffer));

        // Original AbstractCodec.send() mentions
        // Microsoft KB article KB823764:
        // Limiting buffer size increases performance.
        final int batch_limit = server_buffer_size / 2;
        final int total = buffer.limit();
        int batch = total - buffer.position();
        if (batch > batch_limit)
        {
            batch = batch_limit;
            buffer.limit(buffer.position() + batch);
        }

        int tries = 0;
        while (batch > 0)
        {
            final int sent = socket.write(buffer);
            if (sent < 0)
                throw new Exception("Connection closed");
            else if (sent == 0)
            {
                logger.log(Level.FINER, "Send buffer full after " + buffer.position() + " of " + total + " bytes.");
                Thread.sleep(Math.max(++tries * 100, 1000));
            }
            else
            {
                // Wrote _something_
                tries = 0;
                // Determine next batch
                batch = total - buffer.position();
                if (batch > batch_limit)
                    batch = batch_limit;
                // In case batch > 0, move limit to write that batch
                buffer.limit(buffer.position() + batch);
            }
        }
    }

    /** Receiver */
    private Void receiver()
    {
        try
        {
            Thread.currentThread().setName("TCP receiver " + socket.getRemoteAddress());
            logger.log(Level.FINER, Thread.currentThread().getName() + " started");
            logger.log(Level.FINER, "Native byte order " + receive_buffer.order());
            receive_buffer.clear();
            while (true)
            {
                // Read at least one complete message,
                // which requires the header..
                int message_size = PVAHeader.checkMessageAndGetSize(receive_buffer, client_mode);
                while (receive_buffer.position() < message_size)
                {
                    receive_buffer = assertBufferSize(receive_buffer, message_size);
                    final int read = socket.read(receive_buffer);
                    if (read < 0)
                    {
                        logger.log(Level.FINER, () -> Thread.currentThread().getName() + ": socket closed");
                        return null;
                    }
                    if (read > 0)
                        logger.log(Level.FINER, () -> Thread.currentThread().getName() + ": " + read + " bytes");
                    // and once we get the header, it will tell
                    // us how large the message actually is
                    message_size = PVAHeader.checkMessageAndGetSize(receive_buffer, client_mode);
                }
                // .. then decode
                receive_buffer.flip();
                logger.log(Level.FINER, () -> Thread.currentThread().getName() + ":\n" + Hexdump.toHexdump(receive_buffer));

                // While buffer may contain more data,
                // limit it to the end of this message to prevent
                // message handler from reading beyond message boundary.
                final int actual_limit = receive_buffer.limit();
                receive_buffer.limit(message_size);
                try
                {
                    handleMessage(receive_buffer);
                }
                catch (Exception ex)
                {
                    // Once we fail to decode and handle a message,
                    // it is likely that the server/client protocol gets
                    // out of step and never recovers.
                    // Still, log error and keep reading in case
                    // the issue is limited to just this one message.
                    logger.log(Level.WARNING, Thread.currentThread().getName() + " message error. Protocol might be broken from here on.", ex);
                }

                receive_buffer.limit(actual_limit);
                // No matter if message handler read the complete message,
                // position at end of handled message
                receive_buffer.position(message_size);

                // Shift rest to start of buffer and handle next message
                receive_buffer.compact();
            }
        }
        catch (Exception ex)
        {
            if (running)
                logger.log(Level.WARNING, Thread.currentThread().getName() + " exits because of error", ex);
        }
        finally
        {
            onReceiverExited(running);
            logger.log(Level.FINER, Thread.currentThread().getName() + " done.");
        }
        return null;
    }

    /** Invoked when the receiver thread exits because socket has been closed.
     *
     *  <p>Derived class may override to perform cleanup
     *  when socket has been closed
     *
     *  @param running Was TCP connection still running, i.e. unexpected shutdown?
     */
    protected void onReceiverExited(final boolean running)
    {
        // NOP
    }

    /** Check buffer size, grow if needed
     *
     *  <p>When necessary, a new buffer is allocated,
     *  existing data copied.
     *
     *  @param buffer Original buffer
     *  @param message_size Required receive buffer size
     *  @return Original buffer, or larger buffer with copied data
     */
    private ByteBuffer assertBufferSize(final ByteBuffer buffer, final int size)
    {
        if (buffer.capacity() >= size)
            return buffer;

        final ByteBuffer new_buffer = ByteBuffer.allocate(size);
        new_buffer.order(buffer.order());
        buffer.flip();
        new_buffer.put(buffer);

        logger.log(Level.INFO,
                   Thread.currentThread().getName() + " extends buffer from " +
                   buffer.capacity() + " to " + new_buffer.capacity() +
                   ", copied " + new_buffer.position() + " bytes to new buffer");
        return new_buffer;
    }

    /** Handle a received message
     *
     *  <p>Called after the protocol header was found
     *  to contain a valid protocol version and a complete
     *  message has been received.
     *
     *  @param buffer Buffer positioned at start of header, limit set to size of message
     *  @throws Exception on error
     */
    private void handleMessage(final ByteBuffer buffer) throws Exception
    {
        final byte flags = buffer.get(2);
        final byte segemented = (byte) (flags & PVAHeader.FLAG_SEGMENT_MASK);
        if (segemented != 0)
            handleSegmentedMessage(segemented, buffer);
        else
        {
            final boolean control = (flags & PVAHeader.FLAG_CONTROL) != 0;
            final byte command = buffer.get(3);
            // Move to start of potential payload
            buffer.position(8);
            if (control)
                handleControlMessage(command, buffer);
            else
                handleApplicationMessage(command, buffer);
        }
    }

    /** Handle a segmented message
     *
     *  <p>Assembles parts of a segmented message,
     *  then handles it when last part has been received.
     *
     *  @param segmented {@link PVAHeader#FLAG_SEGMENT_MASK} bits of the message
     *  @param buffer Buffer set to one part of a segmented message
     *  @throws Exception on error
     */
    private void handleSegmentedMessage(final byte segmented, final ByteBuffer buffer) throws Exception
    {
        // This implementation does copy data from the original receive buffer
        // into the 'segmented' buffer where they are combined.
        // Original Java implementation also copied received bytes,
        // albeit within the same socketBuffer.
        if (segmented == PVAHeader.FLAG_FIRST)
        {
            if (segments == null)
            {
                logger.log(Level.INFO,
                           () -> Thread.currentThread().getName() + " allocates segmented message accumulator buffer for " + buffer.limit() + " bytes");
                segments = ByteBuffer.allocate(buffer.limit());
                segments.order(buffer.order());
            }
            else if (segments.position() > 0)
                throw new Exception("Received new first message segment while still handling previous one");

            segments = assertBufferSize(segments, buffer.limit());
            segments.put(buffer);
            // Clear the 'segmented' flags in the accumulator buffer
            segments.put(2, (byte) (buffer.get(2) & 0b11001111));

            if (logger.isLoggable(Level.FINER))
            {
                final int pos = segments.position();
                segments.flip();
                logger.log(Level.FINER, "First message segment:\n" + Hexdump.toHexdump(segments));
                segments.position(pos);
            }
        }
        else
        {
            final boolean last = segmented == PVAHeader.FLAG_LAST;

            if (segments == null  ||  segments.position() <= 0)
                throw new Exception("Received " + (last ? "last" : "middle") + " message segment without first segment");
            // Check if command matches the one in first segment
            final byte seg_command = segments.get(3);
            if (seg_command != buffer.get(3))
                throw new Exception(String.format("Received " + (last ? "last" : "middle") +
                                                  " message segment for command 0x%02X after first segment for command 0x%02X",
                                                  buffer.get(3), seg_command));

            // Size of segments accumulated so far..
            final int seg_size = segments.getInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE);
            // Payload of segment to add
            final int payload = buffer.getInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE);
            final int total = seg_size + payload;
            segments = assertBufferSize(segments, PVAHeader.HEADER_SIZE + total);
            // Skip header, add payload to segments
            buffer.position(PVAHeader.HEADER_SIZE);
            segments.put(buffer);
            // Update total size
            segments.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, total);

            if (logger.isLoggable(Level.FINER))
            {
                final int pos = segments.position();
                segments.flip();
                logger.log(Level.FINER, (last ? "Last" : "Middle") + " message segment:\n" + Hexdump.toHexdump(segments));
                segments.position(pos);
            }

            if (last)
            {
                try
                {   // Handle the merged message
                    segments.flip();
                    handleMessage(segments);
                }
                catch (Exception ex)
                {
                    throw new Exception("Error handling assembled segmented message", ex);
                }
                finally
                {   // Reset segments buffer to allow starting with another 'first' message
                    segments.clear();
                }
            }
        }
    }

    /** Handle a received control message
     *
     *  @param command Control command
     *  @param buffer Buffer with complete message, positioned at start of payload
     *  @throws Exception on error
     */
    protected void handleControlMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        logger.log(Level.WARNING, String.format("Cannot handle control command 0x%02x", command));
    }

    /** Handle a received application message
     *
     *  @param command Application command
     *  @param buffer Buffer with complete message, positioned at start of payload
     *  @throws Exception on error
     */
    protected void handleApplicationMessage(final byte command, final ByteBuffer buffer) throws Exception
    {
        if (command == PVAHeader.CMD_MESSAGE)
        {
            final int req = buffer.getInt();
            final byte b = buffer.get();
            if (b < 0  ||  b > 3)
                throw new Exception("Message with invalid type " + b);
            final PVAStatus.Type type = PVAStatus.Type.values()[b];
            final String message = PVAString.decodeString(buffer);

            switch (type)
            {
            case WARNING:
                logger.log(Level.WARNING, "Warning for request #" + req + ": " + message);
                break;
            case ERROR:
                logger.log(Level.SEVERE, "Error for request #" + req + ": " + message);
                break;
            case FATAL:
                logger.log(Level.SEVERE, "Fatal Error for request #" + req + ": " + message);
                break;
            case OK:
            default:
                logger.log(Level.INFO, "Message for request #" + req + ": " + message);
            }
        }
        else
            logger.log(Level.WARNING, String.format("Cannot handle application command 0x%02x", command));
    }

    /** Close network socket and threads
     *  @param wait Wait for threads to end?
     */
    public void close(final boolean wait)
    {
        logger.log(Level.FINE, "Closing " + this);

        // Wait until all requests are sent out
        submit(END_REQUEST);
        try
        {
            if (send_thread != null  &&  wait)
                send_thread.get(5, TimeUnit.SECONDS);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot stop send thread", ex);
        }

        try
        {
            running = false;
            socket.close();
            if (wait)
                receive_thread.get(5, TimeUnit.SECONDS);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot stop receive thread", ex);
        }
        logger.log(Level.FINE, () -> this + " closed  ============================");
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("TCPHandler");
        try
        {
            final SocketAddress server = socket.getRemoteAddress();
            buf.append(" ").append(server);
        }
        catch (Exception ex)
        {
            // Ignore
        }
        return buf.toString();
    }
}
