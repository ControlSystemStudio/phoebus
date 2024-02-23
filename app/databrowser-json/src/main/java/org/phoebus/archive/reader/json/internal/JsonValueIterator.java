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
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.json.JsonArchiveReader;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Iterator for the {@link JsonArchiveReader}. This class is only intended for
 * instantiation by that class.
 * </p>
 *
 * <p>
 * Like most iterators, instances of this class are <em>not</em> thread-safe.
 * The one exception is the {@link #cancel()} method, which may be called by
 * any thread. In order to implement cancellation in a thread-safe way, calling
 * this method only results in a flag being set. The iterator is then closed
 * the next time {@link #hasNext()} is called.
 * </p>
 */
public class JsonValueIterator implements ValueIterator {

    private volatile boolean canceled = false;
    private final boolean honor_zero_precision;
    private final Logger logger;
    private VType next_value;
    private Consumer<JsonValueIterator> on_close;
    private JsonParser parser;
    private final String request_url;

    /**
     * Create an iterator reading samples from a JSON parser. The parser is
     * <em>not</em> closed when this iterator is closed. However, the
     * <code>on_close</code> function is called when the iterator is closed, so
     * the calling code can pass a function that closes the parser.
     *
     * @param parser
     *  JSON parser from which samples are read. The iterator expects that the
     *  parser’s current token is the start of an array and reads samples until
     *  the current token is the corresponding end of an array.
     * @param on_close
     *  function that is called when the iterator is closed. May be
     *  <code>null</code>.
     * @param request_url
     *  URL that was used to retrieve the JSON data. This is only used when
     *  logging error messages.
     * @param honor_zero_precision
     *  whether a precision of zero should result in no fractional digits being
     *  used in the number format of returned values (<code>true</code>) or a
     *  default number  format should be used when the precision is zero
     *  (<code>false</code>). This only applies to floating-point values.
     *  Integer values always use  a number format that does not include
     *  fractional digits.
     * @throws IOException
     *  if initial operations on the JSON parser fail or if the JSON document
     *  is malformed. Errors that occur later do not result in an exception
     *  being thrown. Instead, the error is logged and {@link #hasNext()}
     *  returns <code>false</code>.
     */
    public JsonValueIterator(
            final JsonParser parser,
            final Consumer<JsonValueIterator> on_close,
            final String request_url,
            final boolean honor_zero_precision)
            throws IOException {
        this.logger = Logger.getLogger(getClass().getName());
        this.honor_zero_precision = honor_zero_precision;
        this.on_close = on_close;
        this.parser = parser;
        this.request_url = request_url;
        final var token = this.parser.currentToken();
        if (token == null) {
            throw new IOException("Unexpected end of stream.");
        }
        if (token != JsonToken.START_ARRAY) {
            // The server response is malformed, so we cannot continue.
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        // We try to read the first sample. If that sample is malformed, the
        // exception is raised before an iterator is even returned. If it is
        // well-formed, there is a good chance that the remaining samples are
        // going to be well-formed as well.
        hasNextInternal();
    }

    /**
     * Cancels this iterator. Subsequent calls to {@link  #hasNext()} return
     * <code>false</code>. For use by {@link JsonArchiveReader} only.
     */
    public void cancel() {
        this.canceled = true;
    }

    @Override
    public void close() {
        // The parser field also serves as an indicator whether this iterator
        // has been closed. If the parser is null, we know that the iterator
        // has already been closed.
        if (parser != null) {
            // We have to call the on_close callback. Besides other things,
            // this ensures that the parser is closed.
            if (on_close != null) {
                on_close.accept(this);
            }
            // Give up references that are not needed any longer. Setting the
            // parser reference to null also has the effect that this iterator
            // is marked as closed.
            next_value = null;
            on_close = null;
            parser = null;
        }
    }

    @Override
    public boolean hasNext() {
        final boolean has_next;
        // The hasNext method is not supposed to throw an exception, so when
        // there is an exception, we log it and return false.
        try {
            has_next = hasNextInternal();
        } catch (IOException e) {
            close();
            logger.log(
                    Level.SEVERE,
                    "Error while trying to read sample from server response "
                            + "for URL \""
                            + request_url
                            + "\": "
                            + e.getMessage(),
                    e);
            return false;
        }
        return has_next;
    }

    @Override
    public VType next() {
        // We check whether next_value is null before calling hasNext(). If we
        // called hasNext() directly, this method would throw an exception when
        // cancel was called between calling hasNext() and next(). As cancel()
        // may be called by a different thread, this could result in an
        // unexpected NoSuchElementException being thrown. Therefore, we rather
        // return the already retrieved element and close the iterator on the
        // next call to hasNext().
        if (next_value == null && !hasNext()) {
            // If the parser is null, the last call to hasNext() might have
            // returned true, but close() has been called in between.
            if (parser == null) {
                throw new NoSuchElementException(
                        "This iterator has been closed, so no more elements "
                                + "available.");
            }
            // The last call to hasNext() must have returned false, so this
            // call to next clearly is a violation of the API.
            throw new NoSuchElementException(
                    "next() called while hasNext() == false.");
        }
        VType returnValue = next_value;
        next_value = null;
        return returnValue;
    }

    private boolean fetchNext() throws IOException {
        if (canceled) {
            return false;
        }
        final var token = parser.nextToken();
        if (token == null) {
            throw new IOException(
                    "Stream ended prematurely while trying to read next "
                            + "sample.");
        }
        if (token == JsonToken.END_ARRAY) {
            // There should be no data after the end of the array.
            final var next_token = parser.nextToken();
            if (next_token != null) {
                throw new JsonParseException(
                        parser,
                        "Expected end-of-stream but found " + next_token + ".",
                        parser.getTokenLocation());
            }
            return false;
        }
        next_value = JsonVTypeReader.readValue(parser, honor_zero_precision);
        return true;
    }

    private boolean hasNextInternal() throws IOException {
        if (next_value != null) {
            // We already fetched the next value.
            return true;
        }
        if (parser == null) {
            // The iterator has been closed.
            return false;
        }
        if (fetchNext()) {
            return true;
        }
        close();
        return false;
    }

}
