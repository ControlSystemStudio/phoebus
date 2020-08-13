/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore.datamigration.git;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.providers.jmasar.JMasarDataProvider;
import org.phoebus.applications.saveandrestore.data.providers.jmasar.JMasarJerseyClient;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetController;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotController;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.preferences.PreferencesReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;


@Configuration
public class GitMigratorConfig {

    private static final int DEFAULT_READ_TIMEOUT = 1000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 1000; // ms

    private PreferencesReader preferencesReader;
    private PreferencesReader pvPreferencesReader;
    private Preferences preferences;

    @PostConstruct
    public void init(){
        preferencesReader = new PreferencesReader(getClass(), "/save_and_restore_preferences.properties");
        pvPreferencesReader = new PreferencesReader(getClass(), "/pv_ca_preferences.properties");
        preferences = PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class);
    }

    @Bean
    public Preferences preferences(){
        return preferences;
    }

    @Bean("preferencesReader")
    public PreferencesReader preferencesReader(){
        return preferencesReader;
    }

    @Bean("pvPreferencesReader")
    public PreferencesReader getPvPreferencesReader(){
        return pvPreferencesReader;
    }

    @Bean
    @Scope("singleton")
    public SaveAndRestoreService saveAndRestoreService(){
        return new SaveAndRestoreService();
    }

    @Bean
    public Boolean useMultipleTag() { return preferencesReader.getBoolean("useMultipleTag"); }

    @Bean
    public Boolean keepSavesetWithNoSnapshot() { return preferencesReader.getBoolean("keepSavesetWithNoSnapshot"); }

    @Bean
    public Boolean ignoreDuplicateSnapshots() { return preferencesReader.getBoolean("ignoreDuplicateSnapshots"); }

    @Bean
    public JMasarJerseyClient jmasarClient(){
        JMasarJerseyClient jMasarJerseyClient = new JMasarJerseyClient();
        jMasarJerseyClient.setServiceUrl(preferencesReader.get("jmasar.service.url"));

        return jMasarJerseyClient;
    }

    @Bean
    public DataProvider dataProvider(){
        return new JMasarDataProvider();
    }

    @Bean
    public ApplicationContextProvider applicationContextProvider(){
        return new ApplicationContextProvider();
    }

    @Bean
    public ExecutorService executor(){
        return new ThreadPoolExecutor(1, 1, 0L,TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public Client client(){
        Logger logger = LoggerFactory.getLogger(GitMigratorConfig.class.getName());

        int readTimeout = DEFAULT_READ_TIMEOUT;
        String readTimeoutString = preferencesReader.get("httpClient.readTimeout");
        try {
            readTimeout = Integer.parseInt(readTimeoutString);
            logger.debug("JMasar client using read timeout " + readTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.error("Property httpClient.readTimeout \"" + readTimeoutString + "\" is not a number, using default value " + DEFAULT_READ_TIMEOUT + " ms");
        }

        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        String connectTimeoutString = preferencesReader.get("httpClient.connectTimeout");
        try {
            connectTimeout = Integer.parseInt(connectTimeoutString);
            logger.debug("JMasar client using connect timeout " + connectTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.error("Property httpClient.connectTimeout \"" + connectTimeoutString + "\" is not a number, using default value " + DEFAULT_CONNECT_TIMEOUT + " ms");
        }

        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
        return Client.create(defaultClientConfig);
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
