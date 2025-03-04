/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.mqtt;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.phoebus.pv.PV;

/** MQTT Topic subscription handler
 *
 *  <p>Dispatches MQTT data to {@link MQTT_PV}s
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class MQTT_PVConn implements MqttCallback
{
    MqttClient myClient;
    MqttConnectOptions connOpt;

    /** Mapping from topic to PVs */
    final ConcurrentHashMap<String, CopyOnWriteArrayList<MQTT_PV>> subscribers = new ConcurrentHashMap<>();

    volatile private String brokerURL = MQTT_Preferences.mqtt_broker;
    volatile private String clientID;

    //Random integer in case
    final static Integer randInt = ThreadLocalRandom.current().nextInt(0, 1000000 + 1);

    //volatile private String userName, passWord;

    MQTT_PVConn()
    {
        connect();
    }

    /**
     * Called when a message arrives from a subscribed topic
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
     */
    @Override
    public void messageArrived(String topic, MqttMessage msg) throws Exception
    {
        for (MQTT_PV pv : subscribers.get(topic))
            pv.messageArrived(topic, msg);
    }

    /** @param topicStr Topic
     *  @param pv PV
     *  @throws Exception on error
     */
    public void subscribeTopic (String topicStr, MQTT_PV pv) throws Exception
    {
        if (!connect())
        {
            PV.logger.log(Level.WARNING, "Could not subscribe to mqtt topic \"" + topicStr
                    + "\" due to no broker connection");
            throw new Exception("MQTT subscribe failed: no broker connection");
        }

        final List<MQTT_PV> pvs = subscribers.computeIfAbsent(topicStr, topic ->
        {
            int subQoS = 0;
            try
            {
                myClient.subscribe(topicStr, subQoS);
            }
            catch (Exception ex)
            {
                PV.logger.log(Level.WARNING, "Cannot subscribe to MQTT topic '" + topicStr + "'", ex);
            }
            return new CopyOnWriteArrayList<>();
        });
        pvs.add(pv);
    }

    /** @param topicStr Topic
     *  @param pv PV
     *  @throws Exception on error
     */
    public void unsubscribeTopic (String topicStr, MQTT_PV pv) throws Exception
    {
        if (!connect())
        {
            PV.logger.log(Level.WARNING, "Could not unsubscribe to mqtt topic \"" + topicStr
                    + "\" due to no broker connection");
            throw new Exception("MQTT unsubscribe failed: no broker connection");
        }

        final CopyOnWriteArrayList<MQTT_PV> pvs = subscribers.get(topicStr);
        if (pvs == null)
        {
            PV.logger.log(Level.WARNING, "Could not unsubscribe to mqtt topic \"" + topicStr
                    + "\" due to no internal record of topic");
            throw new Exception("MQTT unsubscribe failed: no topic record");
        }

        pvs.remove(pv);
        if (pvs.isEmpty())
        {
            subscribers.remove(topicStr);
            myClient.unsubscribe(topicStr);
            if (subscribers.isEmpty())
                disconnect();
        }
    }

    /** @param topicStr Topic
     *  @param pubMsg Message
     *  @param pubQoS QOS
     *  @param retained Retain message?
     *  @throws Exception on error
     */
    public void publishTopic(String topicStr, String pubMsg, int pubQoS, boolean retained) throws Exception
    {
        if (!connect())
        {
            PV.logger.log(Level.WARNING, "Could not publish to mqtt topic \"" + topicStr
                    + "\" due to no broker connection");
            throw new Exception("MQTT publish failed: no broker connection");
        }

        MqttTopic topic = myClient.getTopic(topicStr);
        MqttMessage message = new MqttMessage(pubMsg.getBytes());
        message.setQos(pubQoS);
        message.setRetained(retained);

        MqttDeliveryToken token = null;
        try {
            // publish message to broker
            token = topic.publish(message);
            // Wait until the message has been delivered to the broker
            token.waitForCompletion();
            Thread.sleep(100);
        } catch (Exception ex) {
            throw new Exception("Failed to publish message to broker", ex);
        }
    }

    private synchronized void disconnect()
    {
        if (!myClient.isConnected())
        {
            return;
        }

        try
        {
            // wait to ensure subscribed messages are delivered
            Thread.sleep(100);
            myClient.disconnect();
        }
        catch (Exception ex)
        {
            PV.logger.log(Level.WARNING, "Failed to disconnect from MQTT broker " + brokerURL, ex);
        }
    }

    private synchronized boolean connect()
    {
        if (myClient != null && myClient.isConnected())
        {
            return true;
        }

        generateClientID();
        setOptions();

        // Connect to Broker
        try
        {
            myClient = new MqttClient(brokerURL, clientID);
            myClient.setCallback(this);
            myClient.connect(connOpt);
        }
        catch (MqttException ex)
        {
            PV.logger.log(Level.SEVERE, "Could not connect to MQTT broker " + brokerURL, ex);
        }

        return myClient.isConnected();
    }

    private void generateClientID()
    {
        try
        {
            NetworkInterface nwi = NetworkInterface.getByIndex(0);
            byte mac[] = nwi.getHardwareAddress();
            clientID = String.valueOf(mac) + String.valueOf(randInt);
        }
        catch(Exception e)
        {
            try {
                InetAddress address = InetAddress.getLocalHost();
                clientID = address.getCanonicalHostName() + String.valueOf(randInt);
            }
            catch(Exception e2)
            {
                clientID = String.valueOf(randInt);
            }
        }

        //System MAC address (or hostname) + random integer + object hash... hopefully unique?
        clientID += "-" + System.identityHashCode(this);
    }

    private void setOptions()
    {
        connOpt = new MqttConnectOptions();

        connOpt.setCleanSession(true);
        connOpt.setKeepAliveInterval(30);
        connOpt.setWill("ERROR", "PV Disconnected".getBytes(), 0, true);
        //connOpt.setUserName(userName);
        //connOpt.setPassword(passWord.getBytes());
        //TODO: Look up best practices for reconnect
    }

    /**
     * Called when connection to broker is lost
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.Throwable)
     */
    @Override
    public void connectionLost(Throwable arg0)
    {
        PV.logger.log(Level.FINE, "Disconnected from MQTT broker " + brokerURL);

        // Connect to Broker
        // TODO: attempt reconnect repeatedly in background thread with timer backoff and eventual timeout
        try {
            myClient.connect(connOpt);
        } catch (MqttException ex) {
            PV.logger.log(Level.SEVERE, "Could not reconnect to MQTT broker " + brokerURL);
            ex.printStackTrace();
        }
    }

    /**
     * Called when message at QoS 1 or 2 acknowledges arrival at broker (QoS 0 will never ack)
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken)
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0)
    {
        // TODO Auto-generated method stub
    }

}
