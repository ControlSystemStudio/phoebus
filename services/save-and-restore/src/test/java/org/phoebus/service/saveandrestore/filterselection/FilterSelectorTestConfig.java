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

package org.phoebus.service.saveandrestore.filterselection;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchTreeRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.FilterRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.SnapshotDataRepository;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.phoebus.service.saveandrestore.web.config.AcceptHeaderResolver;
import org.phoebus.service.saveandrestore.web.config.WebSecurityConfig;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Base64Utils;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootConfiguration
@ComponentScan(basePackages = "org.phoebus.service.saveandrestore.filterselection")
@SuppressWarnings("unused")
@Profile("!IT")
public class FilterSelectorTestConfig {

    @Bean
    public FilterSelector testFilterSelector(){
        FilterSelector filterSelector = new FilterSelector() {
            @Override
            public List<String> getSupportedFilterNames() {
                return List.of("a", "b");
            }
        };
        return filterSelector;
    }

    @Bean
    public WebSocketHandler webSocketHandler(){
        return Mockito.mock(WebSocketHandler.class);
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public NodeDAO nodeDAO(){
        return Mockito.mock(NodeDAO.class);
    }
}
