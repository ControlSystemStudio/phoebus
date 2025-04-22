Formulas
========

Several types of formulas are supported.
A prefix of the form "=xx(..)" is typically used to select the formula.


Operators
----
You can use the following operators :

| **Arithmetic and maths operators :**
| " **+** " - Addition.
| " **-** " - Subtraction.
| " **\*** " - Multiplication.
| " **/** " - Division.
| " **^** " - Power.
| **rnd(x)** - Return a floating point number between 0 and x.
| **max(expression...)** - Return the maximum between all expressions.
| **min(expression...)** - Return the minimum between all expressions.

| **Logical and comparison operators :**
| " **&** " / "  **&&**  " - AND.
| " **|** " / "  **||**  " - OR.
| " **!(...)** " - NOT.
| " **==** " - Equals.
| " **!=** " - Not equals.
| " **>=** " - Greater than or equal.
| " **>** " - Greater than.
| " **<=** " - Lesser than or equal.
| " **<** " - Lesser than.

| **Conditional operator :**
| **<condition> ? <expr1> : <expr2>** - Return a first expression <expr1> if condition is true or a second expression <expr2> if it's false.


Maths
-----
**abs(double value)** - Returns the absolute value of a double value.

**acos(double value)** - Returns the inverse cosine of a double value. The returned angle is in the
range 0.0 through pi.

**asin(double value)** - Returns the inverse sine of a double value. The returned angle is in the
range -pi/2 through pi/2.

**atan(double value)** - Returns the inverse tangent of a double value. the returned angle is in the
range -pi/2 through pi/2.

**atan2(double y, double x)** - Returns the angle theta from the conversion of rectangular coordinates (x, y)
to polar coordinates (r, theta). This method computes the phase theta by computing an arc tangent
of y/x in the range of -pi to pi.

**ceil(double value)** - Returns the smallest double value that is greater than or equal to the argument
and is equal to a mathematical integer.

**cos(double value)** - Returns the trigonometric cosine of an angle.

**cosh(double value)** - Returns the hyperbolic cosine of a double value. The hyperbolic cosine
of x is defined to be (e^x + e^-x)/2 where e is Euler's number.

**exp(double value)** - Returns Euler's number e raised to the power of a double value.

**expm1(double value)** - Returns (e^x) -1. Note that for values of x near 0, the exact sum of expm1(x) + 1
is much closer to the true result of e^x than exp(x).

**floor(double value)** - Returns the largest double value that is less than or equal to the
argument and is equal to a mathematical integer.

**hypot(double x, double y)** - Returns hypotenuse : sqrt(x² +y²) without intermediate overflow or underflow.

**log(double value)** - Returns the natural logarithm (base e) of a double value.

**log10(double value)** - Returns the base 10 logarithm of a double value.

**pow(double base, double exponent)** - Returns the value of the first argument raised
to the power of the second argument.

**round(double value)** - Returns the closest long to the argument, with ties rounding to positive infinity.

**sin(double value)** - Returns the trigonometric sine of an angle.

**sinh(double value)** - Returns the hyperbolic sine of a double value. The hyperbolic sine
of x is defined to be (e^x - e^-x)/2 where e is Euler's number.

**sqrt(double value)** - Returns the correctly rounded positive square root of a double value.

**tan(double value)** - Returns the trigonometric tangent of an angle.

**tanh(double value)** - Returns the hyperbolic tangent of a double value. The hyperbolic tangent
of x is defined to be (e^x - e^-x)/(e^x + e^-x), in other words, sinh(x)/cosh(x).
Note that the absolute value of the exact tanh is always less than 1.

**toDegrees(double value)** - Converts an angle measured in radians to an approximately equivalent
angle measured in degrees. The conversion from radians to degrees is generally inexact;
users should not expect cos(toRadians(90.0)) to exactly equal 0.0.

**toRadians(double value)** - Converts an angle measured in degrees to an approximately equivalent
angle measured in radians. The conversion from degrees to radians is generally inexact.

**fac(double value)** - Calculate the factorial of the given number.


Arrays
------
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

**arrayTotal(VNumberArray array)** - Returns the sum of all elements in the input array.

**elementAt([VNumberArray | VStringArray] array , VNumber index)** - Returns a VNumber
or VString at the specified index of the input array. If the index is invalid, a NaN or
empty string is returned.

**arrayIndex([VNumberArray | VStringArray] array, [VNumber | VString] value)** - Returns the index of the first
occurrence of the specified value in the input array. If the value is not found, -1 is returned.

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

**arraySampleWithStride(VNumberArray array, VNumber stride, VNumber offset)** - Returns a VNumberArray where each element is defined as array\[x \* stride + offset\].

**arrayCumSum(VNumberArray array)** - Returns a VNumberArray where each element is defined as the cumulative sum of the input array.

String
------
**concat(String s...)** - Concatenate a list of strings of a string array.

**strEqual(String s1, String s2)** - Compare value of 2 strings. Return true if s1 equals s2, else false.


Enum
----
**enumOf(VNumber value, VNumberArray intervals, VStringArray labels)** - Creates a VEnum based a value and a set of intervals.

**indexOf(Enum e)** - Return the index of the enum value.

Time
----

**timestamp(VType value)** - returns the timestamp of the provided value formatted using the millisecond format.

**timestamp(VType value, String format)** - returns the timestamp of the provided value formatted using the user defined format.

Alarm
-----
**highestSeverity(String s...)** - Returns the highest severity.

**majorAlarm(Boolean condition, String s)** - Returns a string with major severity when the given condition is true.

**minorAlarm(Boolean condition, String s)** - Returns a string with minor severity when the given condition is true.


Bitwise operation
-----------------
**bitAND(long a, long b)** - Bitwise AND, operation "a & b". Throw an exception if 'a' or 'b' are floating-point numbers.

**bitOR(long a, long b)** - Bitwise OR, operation "a | b". Throw an exception if 'a' or 'b' are floating-point numbers.

**bitXOR(long a, long b)** - Bitwise XOR, operation "a ^ b". Throw an exception if 'a' or 'b' are floating-point numbers.

**bitLeftShift(long a, long b)** - Bitwise Left Shift, operation "a << b". Throw an exception if 'a' or 'b' are floating-point numbers.

**bitRightShift(long a, long b)** - Bitwise Right Shift, operation "a >> b". Throw an exception if 'a' or 'b' are floating-point numbers.

**bitNOT(long a)** - Bitwise NOT, operation "~a". Throw an exception if 'a' is a floating-point number.


Area detector
-------------
**adData(VNumberArray data, String type)** - Map the area detector data to the specified type,
i.e. [Int8, UInt8, Int16, UInt16, Int32, UInt32, Float32, Float64].

**imageHeight(VImage image)** - Fetch height of image.

**imageWidth(VImage image)** - Fetch width of image.

**imageValue(VImage image)** - Fetch array data of image.

**imageXOffset(VImage image)** - Fetch horizontal offset of image.

**imageXReversed(VImage image)** - Fetch horizontal reversal of image.

**imageYOffset(VImage image)** - Fetch vertical offset of image.

**imageYReversed(VImage image)** - Fetch vertical reversal of image.

**imageDataHorizontalProfile(VNumberArray image, VNumber imageWidth, VNumber yPosition)** - Fetch the horizontal profile data for the given Image data at a specific y position.

**imageDataVerticalProfile(VNumberArray image, VNumber imageWidth, VNumber xPosition)** - Fetch the vertical profile data for the given Image data at a specific x position.

**imageHorizontalProfile(VImage image, VNumber yPosition)** - Fetch the horizontal profile of the given Image at a specific y position.

**imageVerticalProfile(VImage image, VNumber xPosition)** - Fetch the vertical profile of the given Image at a specific x position.
