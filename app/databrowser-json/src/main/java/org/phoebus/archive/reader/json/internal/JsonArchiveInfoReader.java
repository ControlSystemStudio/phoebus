/*******************************************************************************
 * Copyright (c) 2013-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json.internal;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads a {@link ArchiveInfo} objects from a {@link JsonParser}.
 */
public final class JsonArchiveInfoReader {

    /**
     * Information about an archive that is available on the server.
     *
     * @param archive_description the archive’s description.
     * @param archive_key key identifying the archive on the server.
     * @param archive_name the archive’s name.
     */
    public record ArchiveInfo(
            String archive_description,
            int archive_key,
            String archive_name) {
    }

    private JsonArchiveInfoReader() {
    }

    /**
     * Reads a {@link ArchiveInfo} value from a {@link JsonParser}. When
     * calling this method, the parser’s current token must be
     * {@link JsonToken#START_ARRAY START_ARRAY} and when the method returns
     * successfully, the parser’s current token is the corresponding
     * {@link JsonToken#END_ARRAY END_ARRAY}.
     *
     * @param parser JSON parser from which the tokens are read.
     * @return list representing the parsed JSON array.
     * @throws IOException
     *  if the JSON data is malformed or there is an I/O problem.
     */
    public static List<ArchiveInfo> readArchiveInfos(JsonParser parser)
            throws IOException {
        var token = parser.currentToken();
        if (token == null) {
            throw new IOException("Unexpected end of stream.");
        }
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        final var archive_infos = new LinkedList<ArchiveInfo>();
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_ARRAY) {
                break;
            }
            archive_infos.add(readArchiveInfo(parser));
        }
        return archive_infos;
    }

    private static void duplicateFieldIfNotNull(
            final JsonParser parser,
            final String field_name,
            final Object field_value)
            throws JsonParseException {
        if (field_value != null) {
            throw new JsonParseException(
                    parser,
                    "Field \"" + field_name + "\" occurs twice.",
                    parser.getTokenLocation());
        }
    }

    private static ArchiveInfo readArchiveInfo(JsonParser parser)
            throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException(
                    parser,
                    "Expected START_OBJECT but got " + token,
                    parser.getTokenLocation());
        }
        Integer archive_key = null;
        String archive_name = null;
        String archive_description = null;
        String field_name = null;
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            if (field_name == null) {
                if (token == JsonToken.FIELD_NAME) {
                    field_name = parser.getCurrentName();
                    continue;
                } else {
                    throw new JsonParseException(
                            parser,
                            "Expected FIELD_NAME but got " + token,
                            parser.getTokenLocation());
                }
            }
            switch (field_name) {
                case "description" -> {
                    duplicateFieldIfNotNull(
                            parser, field_name, archive_description);
                    archive_description = readStringValue(parser);
                }
                case "key" -> {
                    duplicateFieldIfNotNull(parser, field_name, archive_key);
                    archive_key = readIntValue(parser);
                }
                case "name" -> {
                    duplicateFieldIfNotNull(parser, field_name, archive_name);
                    archive_name = readStringValue(parser);
                }
                default -> throw new JsonParseException(
                        parser,
                        "Found unknown field \"" + field_name + "\".",
                        parser.getTokenLocation());
            }
            field_name = null;
        }
        if (archive_description == null
                || archive_key == null
                || archive_name == null) {
            throw new JsonParseException(
                    parser,
                    "Mandatory field is missing in object.",
                    parser.getTokenLocation());
        }
        return new ArchiveInfo(archive_description, archive_key, archive_name);
    }

    private static int readIntValue(final JsonParser parser)
            throws IOException {
        final var token = parser.getCurrentToken();
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_NUMBER_INT but got "
                            + token,
                    parser.getTokenLocation());
        }
        return parser.getIntValue();
    }

    private static String readStringValue(final JsonParser parser)
            throws IOException {
        final var token = parser.currentToken();
        if (token != JsonToken.VALUE_STRING) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_STRING but got " + token,
                    parser.getTokenLocation());
        }
        return parser.getText();
    }

}
