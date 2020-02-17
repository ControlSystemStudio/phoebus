/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.csstudio.apputil.formula.array;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScaleArrayFormulaFunctionTest {

   @Test
   public void compute() throws Exception{
      ScaleArrayFormulaFunction scaleArray =
              new ScaleArrayFormulaFunction();

      assertEquals("scale", scaleArray.getName());
      assertEquals("array", scaleArray.getCategory());

      VType factor = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());
      VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());
      VType offset = VDouble.of(1.0, Alarm.none(), Time.now(), Display.none());
      VNumberArray result = (VNumberArray)scaleArray.compute(array, factor, offset);

      assertEquals(3, result.getData().size());
      assertTrue(result.getData().getDouble(0) == 3);
      assertTrue(result.getData().getDouble(1) == 5);
      assertTrue(result.getData().getDouble(2) == 7);

      result = (VNumberArray)scaleArray.compute(array, factor);
      assertEquals(3, result.getData().size());
      assertTrue(result.getData().getDouble(0) == 2);
      assertTrue(result.getData().getDouble(1) == 4);
      assertTrue(result.getData().getDouble(2) == 6);


      assertEquals(3, result.getData().size());
      assertTrue(result.getData().getDouble(0) == 2);
      assertTrue(result.getData().getDouble(1) == 4);
      assertTrue(result.getData().getDouble(2) == 6);

      result = (VNumberArray)scaleArray.compute(factor, factor, offset);

      assertTrue(Double.valueOf(result.getData().getDouble(0)).equals(Double.NaN));
   }

   @Test(expected = Exception.class)
   public void testWrongNnumberOfArgumenst1() throws Exception{
      ScaleArrayFormulaFunction scaleArray =
              new ScaleArrayFormulaFunction();

      VType factor = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());
      VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());
      VType offset = VDouble.of(1.0, Alarm.none(), Time.now(), Display.none());

      scaleArray.compute(array, factor, offset, offset);
   }

   @Test(expected = Exception.class)
   public void testWrongNnumberOfArgumenst2() throws Exception{
      ScaleArrayFormulaFunction scaleArray =
              new ScaleArrayFormulaFunction();

      VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());

      scaleArray.compute(array);
   }
}
