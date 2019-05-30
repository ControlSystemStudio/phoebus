/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore;


import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.providers.jmasar.JMasarDataProvider;
import org.phoebus.applications.saveandrestore.data.providers.jmasar.JMasarJerseyClient;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetController;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Configuration
@PropertySource("${propertiesLocation:classpath:save_and_restore_preferences.properties}")
public class AppConfig {

    @Value("${httpClient.readTimeout:1000}")
    private int readTimeout;

    @Value("${httpClient.connectTimeout:1000}")
    private int connectTimeout;

    @Bean
    public JMasarJerseyClient jmasarClient(){
        return new JMasarJerseyClient();
    }

    @Bean
    public DataProvider dataProvider(){
        return new JMasarDataProvider();
    }

    @Bean
    public SaveAndRestoreService saveAndRestoreService(){
        return new SaveAndRestoreService();
    }

    @Bean("saveAndRestoreAppInstance")
    public SaveAndRestoreAppInstance saveAndRestoreAppInstance(){
        return new SaveAndRestoreAppInstance();
    }

    @Bean
    public SaveAndRestoreController saveAndRestoreController(){
        return new SaveAndRestoreController();
    }

    @Bean
    @Scope("prototype")
    public SnapshotController snapshotController(){
        return new SnapshotController();
    }

    @Bean
    @Scope("prototype")
    public SaveSetController saveSetController(){
        return new SaveSetController();
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
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
        return Client.create(defaultClientConfig);
    }
}
