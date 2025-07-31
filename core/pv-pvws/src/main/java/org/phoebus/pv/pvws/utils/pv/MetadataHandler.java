package org.phoebus.pv.pvws.utils.pv;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.phoebus.pv.pvws.models.pv.PvwsData;
import org.phoebus.pv.pvws.models.pv.PvwsMetadata;

import java.util.concurrent.ConcurrentHashMap;


public class MetadataHandler {
    public static final ConcurrentHashMap<String, PvwsMetadata> pvMetaMap = new ConcurrentHashMap<String, PvwsMetadata>();
    //private final ConcurrentHashMap<String, Integer> subscribeAttempts;

    /*public MetadataHandler(ConcurrentHashMap<String, PvwsMetadata> pvMetaMap, ConcurrentHashMap<String, Integer> subscribeAttempts){
        this.pvMetaMap = pvMetaMap;
        this.subscribeAttempts = subscribeAttempts;

    }*/


    public static void setData(PvwsMetadata pv) {
        pvMetaMap.putIfAbsent(pv.getPv(), pv);

    }
    
    /*
    public void refetch(int MAX_SUBSCRIBE_ATTEMPTS, PvwsData pvData, SessionHandler client) throws JsonProcessingException {
        int currentAttempts = subscribeAttempts.getOrDefault(pvData.getPv(), 0);
        if (currentAttempts >= MAX_SUBSCRIBE_ATTEMPTS) {
            System.err.println("Max subscribe attempts reached for PV: " + pvData.getPv());
            client.unSubscribeClient(new String[]{pvData.getPv()});
            return;
        }

        System.out.println("Missed first message for: " + pvData.getPv() + ": attempt " + (currentAttempts + 1));
        try {
            subscribeAttempts.put(pvData.getPv(), currentAttempts + 1);
            client.unSubscribeClient(new String[]{pvData.getPv()});
            Thread.sleep(100);
            client.subscribeClient(new String[]{pvData.getPv()});

        }catch(Exception e) {
            System.err.println("Error unsubscribing or resubscribing PV: " + e.getMessage());
        }
        
     */


    //}

}
