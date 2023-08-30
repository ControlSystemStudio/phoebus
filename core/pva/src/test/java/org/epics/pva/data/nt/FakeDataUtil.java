/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
package org.epics.pva.data.nt;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FakeDataUtil {
    
    /**
     * Generates a sequence of arrays of numbers up to groupSize * nOfArrayGroups
     * 
     * @param groupSize Size of each group
     * @param multiplier To provide a float value
     * @param nOfArrayGroups How many groups to make
     * @return List of lists of fake data
     */
    public static List<List<Double>> fakeData(int groupSize, double multiplier, int nOfArrayGroups) {
        int maxInputNumber = groupSize * nOfArrayGroups;
        List<Double> listNumbers = IntStream.range(1, maxInputNumber).mapToDouble(i -> multiplier * i).boxed()
                .collect(Collectors.toList());
        return IntStream.range(0, nOfArrayGroups - 1)
                .mapToObj(i -> listNumbers.subList(i * groupSize, (i + 1) * groupSize))
                .collect(Collectors.toList());

    }

}
