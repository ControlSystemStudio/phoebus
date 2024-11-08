/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.logbook;

import java.time.Instant;
import java.util.Collection;

public record LogTemplate(String id,
                          String name,
                          String owner,
                          Instant createdDate,
                          Instant modifiedDate,
                          String title,
                          String source,
                          String level,
                          Collection<Logbook> logbooks,
                          Collection<Tag> tags,
                          Collection<Property> properties){

    public LogTemplate(){
        this(null, null, null, null, null, null, null, null, null, null, null);
    }
}
