/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Server for an application instance
 *
 *  <p>Used to establish one Phoebus instance as a handler
 *  of all command line arguments.
 *
 *  <p>First phoebus instance creates this server,
 *  opens a UI, interacts with user, while listening
 *  for client connections.
 *
 *  Follow-up command line invocations will then pass
 *  command line arguments to the initial instance
 *  without opening yet another top-level UI.
 *
 *  <p>External tools can connect to the server
 *  via `telnet` or `nc`, simply sending each argument
 *  as a line separated by '\n'.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ApplicationServer
{
    private static volatile ApplicationServer instance = null;

    private final static int BUFFER_SIZE = 200;
    private final boolean is_server;

    private CompletionHandler<AsynchronousSocketChannel, Void> client_handler;

    private volatile Consumer<String> argument_handler = arg -> logger.log(Level.WARNING, "No argument handler installed to handle " + arg);

    /** Create the application server instance
     *  @param port TCP port where server will serve resp. where this client will connect to server
     *  @return ApplicationServer
     *  @throws Exception on error
     */
    public static ApplicationServer create(final int port) throws Exception
    {
        if (instance != null)
            throw new IllegalStateException();
        instance = new ApplicationServer(port);
        return instance;
    }

    /** @return ApplicationServer, if one was created, or <code>null</code> */
    public static ApplicationServer get()
    {
        return instance;
    }

    public static void setOnArgumentReceived(final Consumer<String> argument_handler)
    {
        if (instance != null)
            instance.argument_handler = argument_handler;
    }


    private ApplicationServer(final int port) throws Exception
    {
        is_server = startServer(port);
    }

    private boolean startServer(final int port) throws Exception
    {
        final AsynchronousServerSocketChannel server_channel = AsynchronousServerSocketChannel.open();
        server_channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
        try
        {
            server_channel.bind(new InetSocketAddress("localhost", port));
        }
        catch (BindException ex)
        {
            // Address in use, there is already a server
            return false;
        }

        client_handler = new CompletionHandler<>()
        {
            @Override
            public void completed(final AsynchronousSocketChannel client_channel, Void Null)
            {
                // Start thread to handle this client..
                handleClient(client_channel);

                // Accept another client
                server_channel.accept(null, client_handler);
            }

            @Override
            public void failed(final Throwable ex, Void Null)
            {
                logger.log(Level.WARNING, "Application server connection error", ex);
            }
        };

        // Accept initial client
        logger.log(Level.INFO, "Listening for arguments on TCP " + port);
        server_channel.accept(null, client_handler);

        return true;
    }

    public boolean isServer()
    {
        return is_server;
    }

    /** @param client_channel Client from which to read arguments */
    private void handleClient(final AsynchronousSocketChannel client_channel)
    {
        try
        {
            logger.log(Level.INFO, "Client connection " + client_channel.getRemoteAddress());
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            final StringBuilder text = new StringBuilder();
            int received;
            while (true)
            {
                buffer.clear();
                received = client_channel.read(buffer).get(60, TimeUnit.SECONDS);
                if (received < 0)
                    break;
                buffer.flip();
                decodeArguments(buffer, text);
            }
        }
        catch (TimeoutException ex)
        {
            logger.log(Level.WARNING, "Timeout for application client " + client_channel, ex);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Application client error", ex);
        }
        try
        {
            client_channel.close();
        }
        catch (IOException e)
        {
            // Ignore, closing anyway
        }
    }

    /** Decode arguments
     *  @param buffer Network buffer with newly received data
     *  @param text Text buffer where complete lines are assembled and handled
     */
    private void decodeArguments(final ByteBuffer buffer, final StringBuilder text)
    {
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        text.append(new String(bytes));

        int sep = text.indexOf("\n");
        while (sep >= 0)
        {
            final String line = text.substring(0,  sep);
            argument_handler.accept(line);
            text.delete(0, sep+1);
            sep = text.indexOf("\n");
        }
    }

    public void sendArguments(final List<String> args)
    {
        // TODO Auto-generated method stub
        for (String arg : args)
        {
            logger.info("Sending argument to server instance: " + arg);
        }
    }
}
