/**
 * Copyright (C) 2018 European Spallation Source ERIC.
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
package org.phoebus.service.saveandrestore.web.config;

import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO;
import org.phoebus.service.saveandrestore.websocket.WebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link Configuration} class setting up beans for {@link org.springframework.stereotype.Controller} classes.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class WebConfiguration {

    @Value("${connection.timeout:5000}")
    public long connectionTimeout;

    @SuppressWarnings("unused")
    @Bean
    public long getConnectionTimeout(){
        return connectionTimeout;
    }

    /**
     *
     * @return A {@link NodeDAO} instance.
     */
    @SuppressWarnings("unused")
    @Bean
    public NodeDAO nodeDAO() {
        return new ElasticsearchDAO();
    }

    /**
     *
     * @return An {@link AcceptHeaderResolver} instance.
     */
    @SuppressWarnings("unused")
    @Bean
    public AcceptHeaderResolver acceptHeaderResolver() {
        return new AcceptHeaderResolver();
    }

    @SuppressWarnings("unused")
    @Bean
    @Scope("singleton")
    public SnapshotUtil snapshotRestorer(){
        return new SnapshotUtil();
    }

    @SuppressWarnings("unused")
    @Bean
    public ExecutorService executorService(){
        return Executors.newCachedThreadPool();
    }

    @SuppressWarnings("unused")
    @Bean(name = "sockets")
    @Scope("singleton")
    public List<WebSocket> getSockets() {
        return new CopyOnWriteArrayList<>();
    }
}
