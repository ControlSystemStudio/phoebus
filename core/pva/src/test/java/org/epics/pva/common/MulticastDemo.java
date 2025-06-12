/*******************************************************************************
 * Copyright (c) 2021-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Overall demo of using sockets for UDP with both IPv4 and IPv6
 *
 *  Loopback (may be called 'lo' or 'lo0') doesn't always support MULTICAST.
 *  If there are multiple network interfaces,
 *  need to tell the program via cmd line which one to use.
 *
 *  To receive, one UDP socket can join both an IPv4 and IPv6 multicast group.
 *  It can receive from both
 *      nc -u 224.0.0.129 9876
 *  and nc -6 -u 'ff02::42:1%en0' 9876
 *  (This might not work for lo/lo0, where it may only receive IPv6).
 *  Maintaining a single receive socket for both protocol families
 *  simplifies the receive thread.
 *
 *  To send, only a StandardProtocolFamily.INET port can send to the IPv4 multicast group,
 *  and that in turn cannot send to the IPv6 multicast group,
 *  so need one socket per protocol type for sending.
 *
 *  Both sending and receiving are specific to an interface.
 *  Sending socket uses IP_MULTICAST_IF option to configure via which interface to send.
 *  Receiving socket joins MC group on a specific interface.
 *
 *  IPv6 Multicast address format: ffFS:xxxxxxxx
 *  ff - Multicast marker
 *  F  - Flag bits 0, Rendezvous point, Prefix, Transient
 *       If not 'transient', address is _predefined_,
 *       assigned/registered by IANA
 *  S  - Scope
 *       1 Interface-local
 *       2 Link local
 *       4 Admin local
 *       5 Site local
 *       8 Org local
 *       E Global
 *
 *  Well known addresses:
 *  ff02::1    All IPv6 devices on link, "broadcast"
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MulticastDemo
{
    // Same as defaults in PVASettings.EPICS_PVAS_INTF_ADDR_LIST
    private static final String MCAST_GROUP4 = "224.0.0.128";
    private static final String MCAST_GROUP6 = "ff02::42:1";
    // Non-default port
    private static int PORT = 9876;
    private static boolean use_v6 = false;

    private static DatagramChannel createUDPChannel(final ProtocolFamily family, final int port) throws Exception
    {
        final DatagramChannel udp = DatagramChannel.open(family);
        udp.configureBlocking(true);
        udp.socket().setReuseAddress(true);
        udp.bind(new InetSocketAddress(port));
        return udp;
    }

    private static void receive(final NetworkInterface iface)
    {
        try
        {
            final InetAddress group4 = InetAddress.getByName(MCAST_GROUP4);
            final InetAddress group6 = InetAddress.getByName(MCAST_GROUP6);

            final DatagramChannel udp = createUDPChannel(StandardProtocolFamily.INET6, PORT);
            udp.join(group4, iface);
            udp.join(group6, iface);

            System.out.println("Listening on " + group4 + ", port " + PORT + " via " + iface.getDisplayName());
            System.out.println("Listening on " + group6 + ", port " + PORT + " via " + iface.getDisplayName());

            final ByteBuffer buf = ByteBuffer.allocate(500);
            int awaiting = 2;
            while (awaiting > 0)
            {
                buf.clear();
                final InetSocketAddress client = (InetSocketAddress) udp.receive(buf);
                buf.flip();
                final byte[] data = new byte[buf.limit()];
                buf.get(data);
                final String received = new String(data);
                System.out.println("Received: '" + received + "' from " + client);
                if ("end".equals(received))
                    --awaiting;
            }
            udp.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.out.println("Exiting receiver");
    }

    private static void send(final DatagramChannel udp, final InetAddress target, final String text) throws Exception
    {
        final InetSocketAddress addr = new InetSocketAddress(target, PORT);
        System.out.println("Sending '" + text + "' to " + addr);
        final ByteBuffer buf = ByteBuffer.allocate(500);
        buf.put(text.getBytes());
        buf.flip();
        udp.send(buf, addr);
    }

    public static void main(String[] args) throws Exception
    {
        NetworkInterface iface = null;
        boolean do_send = false, do_receive = false;
        boolean help = false;
        for (int i=0; i<args.length; ++i)
        {
            final String arg = args[i];
            if ("-s".equals(arg))
                do_send = true;
            else if ("-r".equals(arg))
                do_receive = true;
            else if ("-6".equals(arg))
                use_v6 = true;
            else if ("-p".equals(arg))
            {
                if (i < args.length - 1)
                    PORT = Integer.valueOf(args[++i].strip());
                else
                {
                    System.out.println("Missing port");
                    help = true;
                }
            }
            else if ("-h".equals(arg))
                help = true;
            else
            {
                iface = NetworkInterface.getByName(arg);
                if (iface == null)
                    System.out.println("Cannot get network interface '" + arg + "'");
            }
        }

        if (help  ||  iface == null)
        {
            System.out.println("USAGE: MulticastDemo [options] interface");
            System.out.println("Options:");
            System.out.println(" -s      Run sender");
            System.out.println(" -r      Run receiver (may run both sender and receiver)");
            System.out.println(" -6      Send also via IPv6");
            System.out.println(" -p " + PORT + " Set port");
            System.out.println(" interface: 'lo' or 'lo0' depending on OS");
            System.out.println();
            System.out.println(" Example:   -r -s lo0");
            return;
        }

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
        {
            final DatagramChannel udp4 = createUDPChannel(StandardProtocolFamily.INET, 0);
            udp4.setOption(StandardSocketOptions.IP_MULTICAST_IF, iface);
            udp4.socket().setOption(StandardSocketOptions.IP_MULTICAST_TTL, 1);

            final DatagramChannel udp6 = createUDPChannel(StandardProtocolFamily.INET6, 0);
            udp6.setOption(StandardSocketOptions.IP_MULTICAST_IF, iface);
            udp6.socket().setOption(StandardSocketOptions.IP_MULTICAST_TTL, 1);

            final InetAddress group4 = InetAddress.getByName(MCAST_GROUP4);
            final InetAddress group6 = InetAddress.getByName(MCAST_GROUP6);

            while (! receiver.isDone())
                for (String text : new String[] {"Hello", "bye!", "end"})
                {
                    TimeUnit.SECONDS.sleep(2);
                    if (receiver.isDone())
                        break;
                    send(udp4, group4, text);
                    if (use_v6)
                        send(udp6, group6, text);
                }

            udp6.close();
            udp4.close();
        }

        pool.shutdown();
        System.out.println("Done.");
    }
}
