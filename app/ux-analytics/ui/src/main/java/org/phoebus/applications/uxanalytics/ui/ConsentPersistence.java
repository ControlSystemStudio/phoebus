package org.phoebus.applications.uxanalytics.ui;

import org.phoebus.framework.workbench.Locations;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class ConsentPersistence {

    static final String CONSENT_FILE = Locations.user().getAbsolutePath()+".phoebus-analytics-consent";

    static boolean consentIsPersistent(){
        return new File(CONSENT_FILE).exists();
    }

    static boolean getConsent(){
        File file = new File(CONSENT_FILE);
        if (!file.exists()){
            return false;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return new String(data).equals("1");
        }
        catch (IOException e){
            Logger.getLogger(ConsentPersistence.class.getPackageName()).log(Level.WARNING, "Error reading consent:", e);
            return false;
        }
    }

    private static void writeConsent(String c){
        try {
            FileWriter fileWriter = new FileWriter(CONSENT_FILE);
            fileWriter.write(c);
            fileWriter.close();
        } catch (IOException e) {
            Logger.getLogger(ConsentPersistence.class.getPackageName()).log(Level.WARNING, "Error storing consent:", e);
        }
    }

    static void storeConsent(){
        if (!getConsent()){
            writeConsent("1");
        }
    }

    static void revokeConsent(){
        if(getConsent()){
            writeConsent("0");
        }
    }

    static void deleteConsent(){
        File file = new File(CONSENT_FILE);
        if (file.exists()){
            file.delete();
        }
    }
}
