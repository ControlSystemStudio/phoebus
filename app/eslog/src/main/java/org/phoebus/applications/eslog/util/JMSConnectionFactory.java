package org.phoebus.applications.eslog.util;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;

/**
 * Helper for connecting to a JMS server. Shields from the underling ActiveMQ
 * API, only providing a <code>javax.jms.Connection</code>.
 * <p>
 * Attempts to support a link up/down listener that's notified about the actual
 * JMS server (in case of 'failover' URLs, that can be any one from a list of
 * possible servers).
 * <p>
 * <b>Logging:</b> ActiveMQ uses org.apache.commons.logging, defaulting to its
 * Jdk14Logger. The rest of CSS uses Log4J, but since Log4J potentially gets
 * configured to send log messages to JMS, we would create a circular dependency
 * 
 * <pre>
 * JMS -> logs to Log4J -> sends messages to JMS
 * </pre>
 * <p>
 * The Jdk14Logger will by default print to messages like "Failovertransport
 * connected" to the console.
 * <p>
 * One way to avoid them is to set these VM arguments, either on the
 * command-line or in the *.product file:
 * 
 * <pre>
 *  -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
 *  -Dorg.apache.commons.logging.simplelog.defaultlog=fatal
 * </pre>
 *
 * @author Kay Kasemir
 */
public class JMSConnectionFactory
{
    /**
     * Connect to JMS
     * 
     * @param url
     *            URL of server. Details can differ for ActiveMQ or other
     *            implementations.
     * @return Connection
     * @throws JMSException
     *             on error
     */
    public static Connection connect(final String url) throws JMSException
    {
        return connect(url, ActiveMQConnection.DEFAULT_USER,
                ActiveMQConnection.DEFAULT_PASSWORD);
    }

    /**
     * Connect to JMS
     * 
     * @param url
     *            URL of server. Details can differ for ActiveMQ or other
     *            implementations.
     * @param user
     *            JMS user name
     * @param password
     *            JMS password
     * @return Connection
     * @throws JMSException
     *             on error
     */
    public static Connection connect(final String url, final String user,
            final String password) throws JMSException
    {
        // Instead of using JNDI lookup like this...
        // Context ctx = new InitialContext();
        // QueueConnectionFactory queueConnectionFactory =
        // (QueueConnectionFactory) ctx.lookup("SomeConnectionFactory");
        // ... which requires an appropriate jndi.properties file,
        // we directly use the ActiveMQConnectionFactory.
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                user, password, url);
        return factory.createConnection();
    }

    /**
     * Add a listener that is notified about JMS connection issues to an
     * existing connection. Connection should not be 'start'ed, yet.
     * <p>
     * The implementation depends on the underlying API. What works for ActiveMQ
     * might not be available for other implementations, in which case the
     * listener might never get called.
     * <p>
     * For ActiveMQ it's not clear how to track the connection state dependably.
     * For "failover:..." URLs, the initial connection.start() call will hang
     * until there is a connection established. On the other hand, it seems as
     * if it will already try to connect before 'start()' is called, so even
     * when calling addListener() before start(), the connection might already
     * be up. We call the JMSConnectionListener for that case, but another
     * 'linkUp' might result from race conditions.
     * <p>
     * So in summary this is meant to help track the connection state and JMS
     * server name, but only for info/debugging; it is not dependable.
     *
     * @param connection
     *            Connection to monitor
     * @param listener
     *            JMSConnectionListener to notify
     */
    public static void addListener(final Connection connection,
            final JMSConnectionListener listener)
    {
        final ActiveMQConnection amq_connection = (ActiveMQConnection) connection;
        amq_connection.addTransportListener(new TransportListener()
        {
            @Override
            public void onCommand(Object cmd)
            {
                // Ignore
                // Looks like one could track almost every send/receive
                // in here
            }

            @Override
            public void onException(IOException ex)
            {
                // Ignore
            }

            @Override
            public void transportInterupted()
            {
                listener.linkDown();
            }

            @Override
            public void transportResumed()
            {
                listener.linkUp(
                        amq_connection.getTransport().getRemoteAddress());
            }
        });
        // Is already connected?
        if (amq_connection.getTransport().isConnected())
            listener.linkUp(amq_connection.getTransport().getRemoteAddress());
    }
}