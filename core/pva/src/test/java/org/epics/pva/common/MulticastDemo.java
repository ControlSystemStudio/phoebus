/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Demo of basic multicasting
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MulticastDemo
{
    // Loopback doesn't always support MULTICAST!
    // If there are multiple network interfaces,
    // need to tell the program via cmd line which one to use.

    // To connect as netcat 'sender', might have to add the network interface like this:
    //   nc -6 -u 'ff02::1%eno1' 9876

//    private static final String GROUP = "224.0.0.129";
    private static final String GROUP = "ff02::1";

    private static final int PORT = 9876;

    private static NetworkInterface getLoopback() throws Exception
    {
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements())
        {
            final NetworkInterface iface = interfaces.nextElement();
            if (iface.isUp()  &&
                iface.supportsMulticast()  &&
                iface.isLoopback())
                return iface;
        }
        System.err.println("Cannot determine loopback interface within " +
                           NetworkInterface.networkInterfaces()
                                           .map(NetworkInterface::getDisplayName)
                                           .collect(Collectors.joining(", ")));
        return null;
    }

    private static void send(final NetworkInterface iface, final String message) throws Exception
    {
        final InetSocketAddress target = new InetSocketAddress(GROUP, PORT);
        final DatagramChannel udp = target.getAddress() instanceof Inet4Address
                                  ? DatagramChannel.open(StandardProtocolFamily.INET)
                                  : DatagramChannel.open(StandardProtocolFamily.INET6);
        udp.configureBlocking(true);
        udp.socket().setReuseAddress(true);
        udp.bind(new InetSocketAddress(0));

        udp.setOption(StandardSocketOptions.IP_MULTICAST_IF, iface);

        System.out.println("Sending message to " + target.getHostString() +
                           " " + target.getPort());

        final ByteBuffer buf = ByteBuffer.allocate(500);
        buf.put(message.getBytes());
        buf.flip();
        udp.send(buf, target);
    }

    public static void receive(final NetworkInterface iface)
    {
        try
        {
            final InetAddress group = InetAddress.getByName(GROUP);

            final DatagramChannel udp = group instanceof Inet4Address
                                      ? DatagramChannel.open(StandardProtocolFamily.INET)
                                      : DatagramChannel.open(StandardProtocolFamily.INET6);
            udp.configureBlocking(true);
            udp.socket().setReuseAddress(true);
            udp.bind(new InetSocketAddress(PORT));
            udp.join(group, iface);

            System.out.println("Listening on " + group + ", port " + PORT + " via " + iface.getDisplayName());
            final ByteBuffer buf = ByteBuffer.allocate(500);
            while (true)
            {
                buf.clear();
                final InetSocketAddress client = (InetSocketAddress) udp.receive(buf);
                System.out.println("Got UDP message from " + client.getAddress() +
                                   ", port " + client.getPort());
                buf.flip();
                final byte[] data = new byte[buf.limit()];
                buf.get(data);
                final String received = new String(data);
                System.out.println("Received: " + received + " from " + client);
                if ("end".equals(received))
                    break;
            }
            udp.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.out.println("Exiting receiver");
    }

    public static void main(String[] args) throws Exception
    {
        NetworkInterface iface = null;
        boolean do_send = false, do_receive = false;
        for (String arg : args)
            if ("-s".equals(arg))
                do_send = true;
            else if ("-r".equals(arg))
                do_receive = true;
            else if ("-h".equals(arg))
            {
                System.out.println("USAGE: MulticastDemo [options] [interface]");
                System.out.println("Options:");
                System.out.println(" -s   Run sender");
                System.out.println(" -r   Run receiver (may run both)");
                System.out.println("Interface will default to one marked LOOPBACK and MULTICAST");
                return;
            }
            else
                iface = NetworkInterface.getByName(arg);

        if (iface == null)
            iface = getLoopback();

        final ExecutorService pool = Executors.newCachedThreadPool();
        final Future<?> receiver;

        if (do_receive)
        {
            final NetworkInterface the_iface = iface;
            receiver = pool.submit(() -> receive(the_iface));
        }
        else
            receiver = new CompletableFuture<Void>();

        if (do_send)
            while (! receiver.isDone())
                for (String text : new String[] {"Hello", "bye!", "end"})
                {
                    TimeUnit.SECONDS.sleep(2);
                    send(iface, text);
                }

        pool.shutdown();
        System.out.println("Done.");
    }
}
