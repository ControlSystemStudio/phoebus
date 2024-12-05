/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.logbook;

import java.time.Instant;
import java.util.Collection;

/**
 * Encapsulates elements representing a log entry template.
 * @param id Unique id, determined and set by service when a {@link LogTemplate} is created, i.e. may be <code>null</code>
 * @param name A case-insensitive name for the {@link LogTemplate}, must be unique among all saved {@link LogTemplate}s.
 * @param owner User id set by service when a {@link LogTemplate} is created, i.e. may be <code>null</code>
 * @param createdDate Create date set by service when a {@link LogTemplate} is created, i.e. may be <code>null</code>
 * @param modifiedDate Modify date set by service when a {@link LogTemplate} is created, i.e. may be <code>null</code>
 * @param title May be <code>null</code>.
 * @param source Markdown body content. May be <code>null</code>.
 * @param level Must not be <code>null</code> or empty.
 * @param logbooks May be <code>null</code> or empty list.
 * @param tags May be <code>null</code> or empty list.
 * @param properties May be <code>null</code> or empty list.
 */
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

    @Override
    public String toString(){
        return name;
    }
}
