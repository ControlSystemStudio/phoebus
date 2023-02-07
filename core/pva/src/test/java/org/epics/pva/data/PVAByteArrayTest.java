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

package org.epics.pva.data;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class PVAByteArrayTest {

    @Test
    public void fromString() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        foo.setValue("bar");

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    @Test
    public void fromPVAByteArray() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        PVAByteArray bar = new PVAByteArray("bar", false, (byte)98, (byte)97, (byte)114);
        foo.setValue(bar);

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    @Test
    public void fromPVADoubleArray() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        PVADoubleArray bar = new PVADoubleArray("bar", 98, 97, 114);
        foo.setValue(bar);

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    @Test
    public void fromByteArray() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        foo.setValue(new byte[]{(byte)98, (byte)97, (byte)114});

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    @Test
    public void fromDoubleArray() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        foo.setValue(new double[]{98.0, 97.0, 114.0});

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    @Test
    public void fromList() throws Exception{
        PVAByteArray foo = newPVAByteArray();
        List<Integer> list = Arrays.asList(98, 97, 114);
        foo.setValue(list);

        assertArrayEquals(new byte[]{(byte)98, (byte)97, (byte)114}, foo.get());
    }

    private PVAByteArray newPVAByteArray(){
        return new PVAByteArray("foo", false, (byte)102, (byte)111, (byte)111);
    }
}
