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
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimestampFormats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.time.temporal.UnsupportedTemporalTypeException;

@RestController
public class SearchController extends BaseController {


    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    @SuppressWarnings("unused")
    @GetMapping("/search")
    public SearchResult search(@RequestParam MultiValueMap<String, String> allRequestParams) {
        for (String key : allRequestParams.keySet()) {
            if ("start".equalsIgnoreCase(key) || "end".equalsIgnoreCase(key)) {
                String value = allRequestParams.get(key).get(0);
                Object time = TimeParser.parseInstantOrTemporalAmount(value);
                if (time instanceof Instant) {
                    allRequestParams.get(key).clear();
                    allRequestParams.get(key).add(TimestampFormats.MILLI_FORMAT.format((Instant) time));
                } else if (time instanceof TemporalAmount) {
                    allRequestParams.get(key).clear();
                    try {
                        allRequestParams.get(key).add(TimestampFormats.MILLI_FORMAT.format(Instant.now().minus((TemporalAmount) time)));
                    } catch (UnsupportedTemporalTypeException e) { // E.g. if client sends "months" or "years"
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported date/time specified: " + value);
                    }
                }
            }
        }
        return nodeDAO.search(allRequestParams);
    }
}
