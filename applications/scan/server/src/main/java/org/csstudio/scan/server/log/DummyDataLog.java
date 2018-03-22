package org.csstudio.scan.server.log;

import java.util.Collections;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanSample;

// TODO Replace DummyDataLog with Derby implementation, allowing SPI
public class DummyDataLog extends DataLog
{
    @Override
    protected void doLog(String device, ScanSample sample) throws Exception
    {
        // NOP
    }

    @Override
    public ScanData getScanData() throws Exception
    {
        return new ScanData(Collections.emptyMap());
    }
}
