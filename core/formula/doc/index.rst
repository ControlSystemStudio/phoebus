Formulas
========


Function categories
___________________

*Arrays*

**arrayDiv(VNumberArray array1, VNumberArray array2)** - Returns a VNumberarray where
each element is defined as array1[x] / array2[x]. The input arrays must be of
equal length.

**arrayDivScalar(VNumberArray array, VNumber denominator)** -
Returns a VNumberArray where each element is defined as array[x] / denominator.

**arrayDivScalarInv(VNumber nominator, VNumberArray array)** -
Returns a VNumberArray where each element is defined as nominator / array[x].

**arrayMult(VNumberArray array1, VNumberArray array2)** - Returns a VNumberArray where
each element is defined as array1[x] * array2[x]. The input arrays must be of
equal length.

**arrayOf([VString | VNumber]...)** - Returns a VStringArray or VNumberArray depending on the input argument types. All
elements of the input argument list must be either VString or VNumber.

**arrayPow(VNumberArray array, VNumber exponent)** - Returns a VNumberArray where each
element is defined as pow(array[x], exponent).

**arraySum(VNumberArray array, VNumber offset)** - Returns a VNumberArray where each
element is defined as array[x] + offset. To subtract, use negative offset.

**elementAt([VNumberArray | VStringArray] array , VNumber index)** - Returns a VNumber
or VString at the specified index of the input array. If the index is invalid, a NaN or
empty string is returned.

**histogramOf(VNumberArray array [, VNumber binCount])** - Computes a histogram for the input array. The binCount
argument is optional and defaults to 100.

**scale(VNumberArray array, VNumber factor [,VNumber offset])** - Returns a VNumberArray
where each element is defined as array[x] * factor [+ offset]. The offset is optional.

**subArray([VNumberArray | VStringArray] array, VNumber fromIndex, VNumber toIndex)** -
Returns a VNumberArray or VStringArray that is a sub-array defined by the fromIndex
and toIndex. The indexes must be valid, e.g. fromIndex > 0, fromIndex < toIndex etc.

**arrayRangeOf(VNumberArray array)** - Returns a Display Range of the given array
This includes the display min, max

**arrayStats(VNumberArray array)** - Returns a VStatistic with the statistical information of the given array
This includes the average, min, max, and element count

**arrayMax(VNumberArray array)** - Returns a VDouble with the greatest value of the given array

**arrayMin(VNumberArray array)** - Returns a VDouble with the smallest value of the given array