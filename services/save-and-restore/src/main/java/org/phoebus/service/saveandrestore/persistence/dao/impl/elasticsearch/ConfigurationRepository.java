/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
 */

package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class ConfigurationRepository implements CrudRepository<Configuration, String> {

    @Value("${elasticsearch.folder_node.index:saveandrestore_configuration}")
    public String ES_CONFIGURATION_INDEX;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private final Logger logger = Logger.getLogger(ConfigurationRepository.class.getName());

    @Override
    public <S extends Configuration> S save(S entity) {
        try {
            IndexRequest<Configuration> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_CONFIGURATION_INDEX)
                                    .id(entity.getUniqueId())
                                    .document(entity)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_CONFIGURATION_INDEX).id(response.id()));
                GetResponse<Configuration> resp =
                        client.get(getRequest, Configuration.class);
                return (S) resp.source();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save save set for config id " + entity.getUniqueId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save save set for config id " + entity.getUniqueId());
        }
        return null;
    }

    @Override
    public <S extends Configuration> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<Configuration> findById(String id) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_CONFIGURATION_INDEX).id(id));
            GetResponse<Configuration> resp =
                    client.get(getRequest, Configuration.class);

            if (!resp.found()) {
                return Optional.empty();
            }
            return Optional.of(resp.source());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve save set with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve save set with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {
        return false;
    }

    @Override
    public Iterable<Configuration>  findAll() {
        return null;
    }

    @Override
    public Iterable<Configuration> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String s) {

    }

    @Override
    public void delete(Configuration entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends Configuration> entities) {

    }

    @Override
    public void deleteAll() {

    }
}
