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
import java.util.Iterator;
import java.util.List;

import org.phoebus.vtype.VType;

/** Interface to archive data retrieval.
 *
 *  <p>Historic remark:
 *  Based on the org.csstudio.archive.reader.ArchiveReader
 *  which evolved from contributions and ideas by
 *  Craig McChesney, Sergei Chevtsov, Peregrine McGehee,
 *  Jan Hatje, Albert Kagarmanov, Blaz Lipuscek.
 *
 *  <p>
 *  Key differences to the previous CS-Studio API:
 *  <ul>
 *  <li>No more 'key'.
 *      Only the Channel Archiver network data server uses
 *      a 'key' to identify one of its sub-archives.
 *      All other data servers simply provide data for
 *      a given channel name and time range.
 *      To map to a channel archiver network data server with N
 *      different 'keys', N different Archive Reader URLs
 *      will need to be used which then include the key.
 *  <li>Values are returned as plain {@link Iterator}.
 *  </ul>
 *
 *  @author Kay Kasemir
 */
public interface ArchiveReader extends Closeable
{
    /** Arbitrary description string, may span multiple lines,
     *  with details left to the implementation.
     *  @return Description string.
     */
    public String getDescription();

    /** URL for this ArchiveServer.
     *  @return URL as a string.
     */
    public String getURL();

    /** Find channel in given sub-archive via "file glob" pattern search.
     *  <p>
     *  In principle, globs can be translated into regular expressions,
     *  then use <code>getNamesByRegExp()</code>. But file globs
     *  can be a lot faster for some implementations, and are also known
     *  by more users, so a designated call appeared appropriate.
     *  <p>
     *  The search should be case-insensitive, but details can depend
     *  on the implementation.
     *  <p>
     *  It is not fully defined how to handle an empty pattern.
     *  Ideally, the result would be empty.
     *  To locate <u>all</u> channels in the archive, the user
     *  needs to specifically search for "*".
     *  But some existing implementations return all channels
     *  for an empty pattern...
     *
     *  @param glob_pattern Pattern for channel name with '*' or '?'.
     *  @return List of matching channel names. Might be empty.
     *  @throws Exception on wrong key or internal error.
     */
    public List<String> getNamesByPattern(String glob_pattern)
        throws Exception;

    /** Find channel in given sub-archive via regular expression search.
     *  <p>
     *  For case-sensitivity and handling of empty regular expression refer to
     *  <code>getNamesByPattern</code>
     *
     *  @param reg_exp Regular Expression for channel name.
     *  @return List of matching channel names. Might be empty.
     *  @throws Exception on wrong key or internal error.
     */
    public List<String> getNamesByRegExp(String reg_exp)
        throws Exception;

    /** Read original, raw samples from the archive
     *  @param name Channel name
     *  @param start Start time
     *  @param end End time
     *  @return ValueIterator for the 'raw' samples in the archive
     *  @throws UnknownChannelException when channel is not known
     *  @throws Exception on error
     */

    // Custom ValueIterator where
    //       public VType next() throws Exception;
    // -> OK to use plain Iterator<VType>.
    // Exception when cannot be created,
    // then on error during iteration just log message and return hasNext() == false.

    // Spliterator<VType> ?
    // Good for Stream, but 'split' is not possible with RDB-based ResultSet.
    // Main use case is plain iteration over all samples to
    // add them to plot.
    // Plain iterator is faster than Stream:
    // https://jaxenter.com/java-performance-tutorial-how-fast-are-the-java-8-streams-118830.html
    public Iterator<VType> getRawValues(String name,
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
     *  @return ValueIterator for the 'optimized' samples in the archive
     *  @throws UnknownChannelException when channel is not known
     *  @throws Exception on error
     */
    public default Iterator<VType> getOptimizedValues(String name,
        Instant start, Instant end, int count) throws UnknownChannelException, Exception
    {
        return getRawValues(name, start, end);
    }

    /** Cancel an ongoing get*() call. */
    public void cancel();

    /** Must be called when archive is no longer used to release resources */
    @Override
    public void close();
}
