package org.phoebus.archive.reader.appliance;

import java.util.concurrent.ConcurrentHashMap;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.channelarchiver.XMLRPCArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/**
 * SPI for "pbraw:" archive URLs
 * @author Miha Novak <miha.novak@cosylab.com>
 */
public class ApplianceArchiveReaderFactory implements ArchiveReaderFactory{

    final private static ConcurrentHashMap<String, ApplianceArchiveReader> cache = new ConcurrentHashMap<String, ApplianceArchiveReader>();
    public static final String PREFIX = "pbraw:";

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        ApplianceArchiveReader result = cache.get(url);
        if( result == null ) {
            final ApplianceArchiveReader reader = new ApplianceArchiveReader(url,
                    AppliancePreferences.useStatisticsForOptimizedData,
                    AppliancePreferences.useNewOptimizedOperator);
            result = cache.putIfAbsent(url, reader);
          if( result == null ) {
            result = reader;
          }
        }
        return result;
    }

}
