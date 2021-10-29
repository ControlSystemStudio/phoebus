/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Demo of basic TCP and UDP server supporting IPv6
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IPv6Demo
{
    private final ServerSocketChannel tcp_socket;
    private final DatagramChannel udp_socket;
    private volatile boolean running = true;

    private IPv6Demo(final String host, final int port) throws Exception
    {
        final InetAddress addr = InetAddress.getByName(host);

        tcp_socket = ServerSocketChannel.open();
        tcp_socket.configureBlocking(true);
        tcp_socket.socket().setReuseAddress(true);
        tcp_socket.bind(new InetSocketAddress(addr, port));

        udp_socket = DatagramChannel.open();
        udp_socket.configureBlocking(true);
        udp_socket.socket().setReuseAddress(true);
        udp_socket.bind(new InetSocketAddress(addr, port));
    }

    private void listenTCP()
    {
        System.out.println("Listening on TCP, access via 'nc -6 " + tcp_socket.socket().getInetAddress().getHostAddress() +
                           " " + tcp_socket.socket().getLocalPort() + "'");

        try
        {
            while (running )
            {
                final SocketChannel client = tcp_socket.accept();

                System.out.println("Got connection from " + client.socket().getInetAddress() +
                                   ", port " + client.socket().getPort());
                final ByteBuffer message = ByteBuffer.allocate(500);
                message.put(("Hello, client at " + client.socket().getInetAddress() +
                             ", port " + client.socket().getPort() + "\n").getBytes());
                message.flip();
                client.write(message);
                client.close();
            }

            tcp_socket.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void listenUDP()
    {
        try
        {
            System.out.println("Listening on UDP, access via 'nc -6 -u " + ((InetSocketAddress) udp_socket.getLocalAddress()).getHostString() +
                               " " + udp_socket.socket().getLocalPort() + "'");
            while (running )
            {
                final ByteBuffer message = ByteBuffer.allocate(500);

                final InetSocketAddress client = (InetSocketAddress) udp_socket.receive(message);

                System.out.println("Got UDP message from " + client.getAddress() +
                                   ", port " + client.getPort());

                message.flip();

                final byte[] data = new byte[message.limit()];
                message.get(data);
                message.clear();
                message.put(("Hello, client at " + client.getAddress() +
                             ", port " + client.getPort() + "\n").getBytes());
                message.put("I got your message:\n> ".getBytes());
                message.put(data);
                message.put(" <\nBye!\n".getBytes());
                message.flip();
                udp_socket.send(message, client);
            }

            udp_socket.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }



    private static void analyze(final String host) throws Exception
    {
        final InetAddress addr = InetAddress.getByName(host);
        System.out.print("'" + host + "' -> ");

        analyze(addr);
    }

    private static void analyze(final InetAddress addr)
    {
        try
        {
            // There is no is...() method for detecting IPv4/6
            // https://docs.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/index.html
            // "IPv4 and IPv6 can be distinguished by the Java type Inet4Address and Inet6Address"
            if (addr instanceof Inet6Address)
            {
                System.out.print("IPv6 " + addr);
                final byte[] segments = addr.getAddress();
                for (int i=0; i<segments.length; i+=2)
                    if (i==0)
                        System.out.printf(", %02x%02x", Byte.toUnsignedInt(segments[0]), Byte.toUnsignedInt(segments[1]));
                    else
                        System.out.printf(":%02x%02x", Byte.toUnsignedInt(segments[i]), Byte.toUnsignedInt(segments[i+1]));

                final Inet6Address addr6 = (Inet6Address) addr;
                final NetworkInterface iface = addr6.getScopedInterface();
                if (iface != null)
                    System.out.print(", interface " + iface.getName());
            }
            else
            {
                System.out.print("IPv4 " + addr);
                final byte[] segments = addr.getAddress();
                for (int i=0; i<segments.length; ++i)
                    if (i == 0)
                        System.out.print(", " + Byte.toUnsignedInt(segments[i]));
                    else
                        System.out.print("." + Byte.toUnsignedInt(segments[i]));
            }
            if (addr.isLoopbackAddress())
                System.out.print(" (loopback)");
            if (addr.isMulticastAddress())
                System.out.print(" (multicast)");
            if (addr.isLinkLocalAddress())
                System.out.print(" (link local)");
            if (addr.isSiteLocalAddress())
                System.out.print(" (site local)");
            System.out.println();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception
    {
        // Should "localhost" turn into 127.0.0.1 or ::1?
        // System.setProperty("java.net.preferIPv6Addresses", Boolean.TRUE.toString());

        System.out.println("* Some Examples");
        for (String addr : List.of("127.0.0.1", "",
                                   "::1", "localhost",
                                   "fe80::ce6:7daf:3025:7cdf%en0",
                                   "1.2.3.4", "::ffff:1.2.3.4"))
            IPv6Demo.analyze(addr);

        System.out.println("* Local interfaces");
        NetworkInterface.networkInterfaces()
                        .flatMap(NetworkInterface::inetAddresses)
                        .forEach(IPv6Demo::analyze);

        final IPv6Demo server = new IPv6Demo("::1", 9876);
        final ExecutorService threads = Executors.newCachedThreadPool();
        threads.submit(server::listenUDP);
        TimeUnit.SECONDS.sleep(2);
        threads.submit(server::listenTCP);

        threads.awaitTermination(1, TimeUnit.DAYS);
    }
}
