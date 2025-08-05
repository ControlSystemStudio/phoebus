package org.phoebus.pv.pvws.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.vtype.VType;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.pvws.PVWS_PV;
import org.phoebus.pv.pvws.models.pv.PvwsData;
import org.phoebus.pv.pvws.models.pv.PvwsMetadata;
import org.phoebus.pv.pvws.utils.pv.VArrDecoder;
import org.phoebus.pv.pvws.utils.pv.toVType;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.phoebus.pv.pvws.utils.pv.MetadataHandler;

public class PVWS_Client extends WebSocketClient {

        public final ObjectMapper mapper;
        private final CountDownLatch latch;
        /*
        private SubscriptionHandler subHandler;
        private HeartbeatHandler heartbeatHandler;
        private ReconnectHandler reconnectHandler;
        private MetadataHandler metadataHandler;

         */

        public PVWS_Client(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
            super(serverUri);
            this.latch = latch;
            this.mapper = mapper;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            try {
                System.out.println("Connected to server");
                latch.countDown();
                //reconnectHandler.resetStatus();
                //heartbeatHandler.start();
            } catch (Exception e) {
                System.err.println("Exception in onOpen: " + e.getMessage());
                e.printStackTrace();

            }
        }

        @Override
        public void onMessage(String message) {

            System.out.println("📨👍👍 Received: " + message);



            try {
                JsonNode node = mapper.readTree(message);

                mapMetadata(node);


                String type = node.get("type").asText();
                switch (type) {
                    case "update":

                        PvwsData pvObj = mapper.treeToValue(node, PvwsData.class);

                        /* TODO: ADD REFETCH FUNCTIONALITY
                        if (!MetadataHandler.pvMetaMap.containsKey(pvObj.getPv())) {

                            final int MAX_SUBSCRIBE_ATTEMPTS = 5;
                            MetadataHandler.refetch(MAX_SUBSCRIBE_ATTEMPTS, pvObj, this);
                            return;

                        }*/


                        if (pvObj.getPv().endsWith(".RTYP")) {
                            return;
                        }
                        VArrDecoder.decodeArrValue(node, pvObj);


                        //subscribeAttempts.remove(pvObj.getPv()); // reset retry count if we got the meta data

                        // TODO: NEEDS separate class to handle this specific severity data and probably status too
                        updateSeverity(node, pvObj);

                        //merges class PV and json node of metadata together


                        //toVType.convert(pvObj);

                        mergeMetadata(pvObj);

                        VType vVal = toVType.convert(pvObj);

                        String pvname = ("pvws://" + pvObj.getPv());



                            PVPool.getPV(pvname).update(vVal);




                        break;
                    default:
                        System.out.println("⚠️ 😤Unknown message type: " + type);

                }
            } catch (Exception e) {
                System.err.println("Error parsing or processing message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void mapMetadata(JsonNode node) throws JsonProcessingException {

            PvwsMetadata pvMeta = mapper.treeToValue(node, PvwsMetadata.class);

            if (pvMeta.getVtype() != null)
                MetadataHandler.setData(pvMeta); // comment this line out to test missing

        }

        private void updateSeverity(JsonNode node, PvwsData pvData)
        {
            if (node.has("severity"))// if severity changes set it in cached value
            {
                String currPV = pvData.getPv();
                String currSeverity = pvData.getSeverity();
                MetadataHandler.pvMetaMap.get(currPV).setSeverity(currSeverity);
            }
        }

        private void mergeMetadata(PvwsData pvData) throws IOException {
            JsonNode nodeMerge = mapper.valueToTree(MetadataHandler.pvMetaMap.get(pvData.getPv()));
            mapper.readerForUpdating(pvData).readValue(nodeMerge);
        }


        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("❌ Disconnected. Reason: " + reason);
            /* TODO: HEARTBEAT AND RECONN HANDLER
             heartbeatHandler.stop();

            attemptReconnect();

             */
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("🚨 WebSocket Error: " + ex.getMessage());
            /* TODO: HEARTBEAT AND RECONN HANDLER
            heartbeatHandler.stop();
            attemptReconnect();

             */
            this.close();
        }


        public void closeClient() {
            this.close();
        }




        /* TODO: ADD HANDLERS
        public void setSubscriptionHandler(SubscriptionHandler subHandler) {
            this.subHandler = subHandler;
        }

        public void setHeartbeatHandler(HeartbeatHandler heartbeatHandler) {
            this.heartbeatHandler = heartbeatHandler;
        }

        public void setReconnectHandler(ReconnectHandler reconnectHandler) {
            this.reconnectHandler = reconnectHandler;
        }

        public void setMetadataHandler(MetadataHandler metadataHandler) {
            this.metadataHandler = metadataHandler;
        }

        public void subscribeClient(String[] pvs) throws JsonProcessingException {
            subHandler.subscribe(pvs);
        }

        public void unSubscribeClient(String[] pvs) throws JsonProcessingException {
            subHandler.unSubscribe(pvs);
        }

        public void attemptReconnect() {
            this.reconnectHandler.attemptReconnect();
        }
        */


        /* TODO: NEEDS HEARTBEAT HANDLER AND IDEALLY REFACTOR THESE 2 INTO THE HEARTBEAT CLASS
        @Override
        public void onWebsocketPing(WebSocket conn, Framedata f) {
            System.out.println("Received Ping frame");
            super.onWebsocketPing(conn, f);
        }

        @Override
        public void onWebsocketPong(WebSocket conn, Framedata f) {
            System.out.println("Received Pong frame"); // you could also comment this out to test the heartbeat timeout just for visual clarity
            super.onWebsocketPong(conn, f);

           heartbeatHandler.setLastPongTime(System.currentTimeMillis());
        }

         */


    }


