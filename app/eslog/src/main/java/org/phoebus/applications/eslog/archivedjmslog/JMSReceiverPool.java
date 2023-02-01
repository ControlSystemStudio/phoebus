package org.phoebus.applications.eslog.archivedjmslog;

import java.util.HashMap;
import java.util.Map;

import org.phoebus.applications.eslog.Activator;

/** Lets several clients share one JMS connection. */
public class JMSReceiverPool
{
    /** Connections can be shared if these properties match. */
    private static class Key
    {
        private final Integer pass;

        private final String url;

        private final String user;

        public Key(String url, String user, String pass)
        {
            this.url = url;
            this.user = user;
            // We include the password so that the connection cannot be
            // hijacked.
            // But there's no need to keep it in memory as plain text. Let's
            // apply minimal hashing at least.
            this.pass = (null == pass) ? null : pass.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Key))
            {
                return false;
            }
            Key k = (Key) o;
            if (!this.url.equals(k.url))
            {
                return false;
            }
            if ((null == this.user) != (null == k.user))
            {
                return false;
            }
            if ((null != this.user) && !this.url.equals(k.user))
            {
                return false;
            }
            if ((null == this.pass) != (null == k.pass))
            {
                return false;
            }
            return ((null == this.pass) || this.pass.equals(k.pass));
        }

        @Override
        public int hashCode()
        {
            int r = this.url.hashCode();
            if (null != this.user)
            {
                r ^= this.user.hashCode();
            }
            if (null != this.pass)
            {
                r ^= this.pass;
            }
            return r;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            return "Key: " + this.url + " / " + this.user;
        }
    }

    static Map<Key, JMSReceiver> activeModels = new HashMap<>();

    public static synchronized JMSReceiver getReceiver(String url, String user,
            String password, String topic)
    {
        Activator.checkParameterString(url, "url"); //$NON-NLS-1$
        Activator.checkParameterString(topic, "topic"); //$NON-NLS-1$
        Key k = new Key(url, user, password);
        JMSReceiver receiver = activeModels.computeIfAbsent(k, key -> {
            JMSReceiver recv = new JMSReceiver(url, user, password);
            recv.start();
            return recv;
        });
        receiver.subscribeToTopic(topic);
        return receiver;
    }
}
