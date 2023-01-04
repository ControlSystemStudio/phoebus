package org.epics.pva.data.nt;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAString;
import org.junit.Test;

public class PVAScalarTest {
    @Test
    public void testDouble() throws PVAScalarValueNameException {
        PVAScalar<PVADouble> doubleScalar = (new PVAScalar.Builder<PVADouble>()).name("pvDoubleName").value( new PVADouble("value", 1.1)).build();
        assertEquals(new PVADouble("value", 1.1), doubleScalar.get("value"));
    }

    @Test
    public void testString() throws PVAScalarValueNameException {
        PVAScalar<PVAString> stringScalar = (new PVAScalar.Builder<PVAString>()).name("pvStringName").value( new PVAString("value", "1.1")).build();
        assertEquals(new PVAString("value", "1.1"), stringScalar.get("value"));
    }

    @Test
    public void testPVAScalarValueNameException() {
        
        PVAScalar.Builder<PVAString> builder = (new PVAScalar.Builder<PVAString>()).name("pvName").value(new PVAString("notvalue", "the value"));
       
        assertThrows(PVAScalarValueNameException.class, () -> {
            builder.build();
        });
    }

}
