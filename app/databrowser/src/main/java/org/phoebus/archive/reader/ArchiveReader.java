/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.io.Closeable;
import java.time.Instant;
import java.util.Collection;

/** Interface to archive data retrieval.
 *
 *  Based on the org.csstudio.archive.reader.ArchiveReader
 *  which evolved from contributions and ideas by
 *  Craig McChesney, Sergei Chevtsov, Peregrine McGehee,
 *  Jan Hatje, Albert Kagarmanov, Blaz Lipuscek.
 *
 *  @author Kay Kasemir
 */
public interface ArchiveReader extends Closeable
{
    // Comparison with previous CS-Studio API:
    //
    // No more 'key'.
    // Only the Channel Archiver network data server uses
    // a 'key' to identify one of its sub-archives.
    // All other data servers simply provide data for
    // a given channel name and time range.
    // To map to a channel archiver network data server with N
    // different 'keys', N different Archive Reader URLs
    // need to be used which now include the key.
    //
    // ValueIterator is based on plain {@link Iterator},
    // no longer using a custom ValueIterator where
    // next() throws Exception.
    // The plain Iterator is generally known.
    // The get*() calls can still throw an Exception
    // when data simply cannot be fetched.
    // When there is a problem later on while iterating
    // over the samples, they can log an error message
    // and return hasNext() == false.
    //
    // A Stream or Spliterator<VType> was considered but abandoned.
    // Main use case is plain iteration over all samples to add them to plot
    // or to export them, and a plain iterator is faster than Stream:
    // https://jaxenter.com/java-performance-tutorial-how-fast-are-the-java-8-streams-118830.html

    /** Arbitrary description string, may span multiple lines,
     *  with details left to the implementation.
     *  @return Description string.
     */
    public String getDescription();

    /** Find channel in given sub-archive via "file glob" pattern search.
     *  <p>
     *  The search should be case-insensitive.
     *  <p>
     *  An empty pattern should return an empty result.
     *  To locate <u>all</u> channels in the archive,
     *  search for "*".
     *
     *  @param glob_pattern Pattern for channel name with '*' or '?'.
     *  @return Matching channel names. Might be empty.
     *  @throws Exception on wrong key or internal error.
     */
    public Collection<String> getNamesByPattern(String glob_pattern)
        throws Exception;

    /** Read original, raw samples from the archive
     *  @param name Channel name
     *  @param start Start time
     *  @param end End time
     *  @return {@link ValueIterator} for the 'raw' samples in the archive
     *  @throws UnknownChannelException when channel is not known
     *  @throws Exception on error
     */
    public ValueIterator getRawValues(String name,
            Instant start, Instant end) throws UnknownChannelException, Exception;

    /** Read optimized samples from the archive.
     *  <p>
     *  The exact behavior is up to the implementation.
     *  In the simplest case, a data provider can fall back to
     *  <code>getRawValues</code>, i.e. return the raw data.
     *  Ideally, however, the result will contain about <code>count</code>
     *  values that represent the data between the <code>start</code> and
     *  <code>end</code> time, for example by segmenting the time range into
     *  'count' buckets and returning the min/max/average for each bucket.
     *  If the raw data contains less than the requested 'count',
     *  or the raw data is not numeric and thus cannot be reduced,
     *  the method can fall back to returning the original samples.
     *
     *  @param name Channel name
     *  @param start Start time
     *  @param end End time
     *  @param count Hint for number of values
     *  @return {@link ValueIterator} for the 'optimized' samples in the archive
     *  @throws UnknownChannelException when channel is not known
     *  @throws Exception on error
     */
    public default ValueIterator getOptimizedValues(String name,
        Instant start, Instant end, int count) throws UnknownChannelException, Exception
    {
        return getRawValues(name, start, end);
    }

    /** Cancel an ongoing get*() call. */
    public default void cancel()
    {
        // NOP
    };

    /** Must be called when archive is no longer used to release resources */
    @Override
    public default void close()
    {
        // NOP
    }
}
