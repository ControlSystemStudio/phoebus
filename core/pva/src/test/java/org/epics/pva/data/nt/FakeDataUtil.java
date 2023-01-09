package org.epics.pva.data.nt;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FakeDataUtil {
    
    /**
     * Generates a sequence of arrays of numbers up to maxInputNumber
     * 
     * For example with maxInputNumber: 10, multiplier: 2, nOfArrayGroups: 2
     * returns {{2, 4, 6, 8, 10}, {12, 14, 16, 18, 20}}
     * 
     * @param maxInputNumber
     * @param multiplier
     * @param nOfArrayGroups
     * @return List of list of fake data
     */
    public static List<List<Double>> fakeData(int maxInputNumber, double multiplier, int nOfArrayGroups) {
        List<Double> listNumbers = IntStream.range(1, maxInputNumber).mapToDouble(i -> multiplier * i).boxed()
                .collect(Collectors.toList());
        int groupSize = maxInputNumber / nOfArrayGroups;
        return IntStream.range(0, nOfArrayGroups - 1)
                .mapToObj(i -> listNumbers.subList(i * groupSize, (i + 1) * groupSize))
                .collect(Collectors.toList());

    }

}
