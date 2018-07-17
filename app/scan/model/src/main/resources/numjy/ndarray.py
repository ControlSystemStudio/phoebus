""" Array support

Ideas based on numpy ndarray, but limited functionality.

Implementation heavily influenced by GDA scisoftpy,
Copyright 2010 Diamond Light Source Ltd.

@author: Kay Kasemir
"""

import math
import org.csstudio.ndarray.NDType as NDType
import org.csstudio.ndarray.NDArray as NDArray
import org.csstudio.ndarray.NDMath as NDMath
import org.csstudio.ndarray.NDMatrix as NDMatrix
import org.csstudio.ndarray.NDCompare as NDCompare
import org.csstudio.ndarray.NDShape as NDShape
import jarray
import java.lang.Class

# Use float to get nan, because float will be replaced with ndarray data type
nan = float('nan')
_float = float
_int = int
_bool = bool

# Data types
float = float64 = NDType.FLOAT64
float32 = NDType.FLOAT32
int = int64 = NDType.INT64
int32 = NDType.INT64
int16 = NDType.INT16
byte = int8 = NDType.INT8
bool = NDType.BOOL


def __isBoolArray__(array):
    """Check if array is boolean
       For non-ndarray, only checks first element
    """
    if isinstance(array, ndarray):
        return array.dtype == NDType.BOOL
    else:
        return len(array) > 0  and  isinstance(array[0], _bool)


def __toNDShape__(shape):
    """Create shape for scalar as well as list"""
    if isinstance(shape, (tuple, list)):
        if len(shape) == 1:
            return __toNDShape__(shape[0])
        return NDShape(shape)
    return NDShape([shape])


class ndarray_iter:
    """Iterator for elements in ND array
       Performs 'flat' iteration over all elements
    """
    def __init__(self, iter):
        self.iter = iter
        
    def __iter__(self):
        return self
    
    def next(self):
        if self.iter.hasNext():
            return self.iter.nextDouble()
        else:
            raise StopIteration

class ndarray:
    """N-Dimensional array
    
    Example:
    array([ 0, 1, 2, 3 ])
    array([ [ 0, 1 ], [ 2, 3 ], [ 4, 5 ] ])
    """
    def __init__(self, nda):
        self.nda = nda
        
    def getBase(self):
        """Base array, None if this array has no base"""
        if self.nda.getBase() is None:
            return
        return ndarray(self.nda.getBase())
    
    base = property(getBase)
    
    def getShape(self):
        """Shape of the array, one element per dimension"""
        return tuple(self.nda.getShape().getSizes())
    
    shape = property(getShape)
    
    def getType(self):
        """Get Data type of array elements"""
        return self.nda.getType()
    
    dtype = property(getType)
    
    def getRank(self):
        """Get number of dimensions"""
        return self.nda.getRank()
    
    ndim = property(getRank)
    
    def getStrides(self):
        """Get strides
           Note that these are array index strides,
           not raw byte buffer strides as in NumPy
        """
        return tuple(self.nda.getStrides().getStrides())
    
    strides = property(getStrides)

    def copy(self):
        """Create a copy of this array"""
        return ndarray(self.nda.clone())
    
    def reshape(self, *shape):
        """reshape(shape):
           Create array view with new shape
    
           Example:
           arange(6).reshape(3, 2)
           results in array([ [ 0, 1 ], [ 2, 3 ], [ 4, 5 ] ])
        """
        return ndarray(NDMatrix.reshape(self.nda, __toNDShape__(shape)))
    
    def transpose(self):
        """Compute transposed array, i.e. swap 'rows' and 'columns'"""
        return ndarray(NDMatrix.transpose(self.nda))
    
    T = property(transpose)
    
    def __len__(self):
        """Returns number of elements for the first dimension"""
        if len(self.shape) > 0:
            return self.shape[0]
        return 0
    
    def __getSlice__(self, indices):
        """@param indices: Indices that may address a slice
           @return: NDArray for slice, or None if indices don't refer to slice
        """
        # Turn single argument into tuple to allow following iteration code
        if not isinstance(indices, tuple):
            indices = ( indices, )
        
        given = len(indices)
        dim = self.nda.getRank()

        any_slice = False
        starts = []
        stops = []
        steps = []
        i = 0
        for i in range(dim):
            if i < given:
                index = indices[i]
                if isinstance(index, slice):
                    # Slice provided
                    any_slice = True
                    # Replace 'None' in any portion of the slice
                    start = 0 if index.start is None else index.start
                    stop  = self.nda.getShape().getSize(i) if index.stop is None else index.stop
                    step  = 1 if index.step is None else index.step
                else:
                    # Simple index provided: stop = step = 0 indicates
                    # to NDArray.getSlice() to 'collapse' this axis,
                    # using start as index
                    start = index
                    stop = step = 0
            else:
                # Nothing provided for this dimension, use full axis
                any_slice = True
                start = 0
                stop = self.nda.getShape().getSize(i)
                step = 1
            starts.append(start)
            stops.append(stop)
            steps.append(step)
        
        if any_slice:
            return self.nda.getSlice(starts, stops, steps)
        # There was a plain index for every dimension, no slice at all
        return None
        
    def __getitem__(self, indices):
        """Get element of array, or fetch sub-array
        
        Example:
        a = array([ [ 0, 1 ], [ 2, 3 ], [ 4, 5 ] ])
        a[1, 1] # Result is 3
        a[1] # Result is second row of the 3x2 array
        
        May also provide slice:
        a = arange(10)
        a[1:6:2] # Result is [ 1, 3, 5 ]
        
        Differing from numpy, this returns all values as float,
        so if they are later used for indexing, int() needs to be used.
        """
        slice = self.__getSlice__(indices)
        if slice is None:
            if isinstance(indices, (list, ndarray)):
                N = len(indices)
                if __isBoolArray__(indices):
                    result = []
                    for i in range(N):
                        if indices[i]:
                            result.append(self.nda.getDouble(i))
                    return array(result)
                else:
                    # Array of indices, each addresses one element of the array
                    result = zeros(N)
                    for i in range(N):
                        # Need _int because int is now set to the NDType name 'int'
                        result[i] = self.nda.getDouble(_int(indices[i]))
                    return result
            else:
                # Indices address one element of the array
                return self.nda.getDouble(indices)
        # else: Need to return slice/view of array
        return ndarray(slice)

    def __setitem__(self, indices, value):
        """Set element of array
        
        Example:
        a = zeros(3)
        a[1] = 1

        a = array([ [ 0, 1 ], [ 2, 3 ], [ 4, 5 ] ])
        a[1] = array([ 20, 30 ])
        """
        if isinstance(value, ndarray):
            # Create view for requested section of self,
            # then assign the provided value to it
            slice = self.__getSlice__(indices)
            slice.set(value.nda)
        else:
            self.nda.setDouble(value, indices)

    def __iter__(self):
        return ndarray_iter(self.nda.getIterator())
    
    def __neg__(self):
        """Return array where sign of each element has been reversed"""
        result = self.nda.clone()
        NDMath.negative(result)
        return ndarray(result)
    
    def __add__(self, value):
        """Add scalar to all elements, or add other array element-by-element"""
        if isinstance(value, ndarray):
            return ndarray(NDMath.add(self.nda, value.nda))
        else:
            result = self.nda.clone()
            NDMath.increment(result, value)
            return ndarray(result)
    
    def __radd__(self, value):
        """Add scalar to all elements, or add other array element-by-element"""
        return self.__add__(value)
    
    def __iadd__(self, value):
        """Add scalar to all elements, or add other array element-by-element"""
        if isinstance(value, ndarray):
            NDMath.increment(self.nda, value.nda)
        else:
            NDMath.increment(self.nda, value)
        return self
    
    def __sub__(self, value):
        """Subtract scalar from all elements, or sub. other array element-by-element"""
        if isinstance(value, ndarray):
            return ndarray(NDMath.subtract(self.nda, value.nda))
        else:
            result = self.nda.clone()
            NDMath.increment(result, -value)
            return ndarray(result)
    
    def __rsub__(self, value):
        """Subtract scalar from all elements, or sub. other array element-by-element"""
        result = self.nda.clone()
        NDMath.negative(result)
        NDMath.increment(result, value)
        return ndarray(result)

    def __isub__(self, value):
        """Subtract scalar from all elements, or sub. other array element-by-element"""
        return self.__iadd__(-value)
    
    def __mul__(self, value):
        """Multiply by scalar or by other array elements"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDMath.multiply(self.nda, value.nda))

    def __rmul__(self, value):
        """Multiply by scalar or by other array elements"""
        return self.__mul__(value)

    def __imul__(self, value):
        """Scale value by scalar or element-by-element"""
        if isinstance(value, ndarray):
            NDMath.scale(self.nda, value.nda)
        else:
            NDMath.scale(self.nda, value)
        return self
    
    def __div__(self, value):
        """Divide by scalar or by other array elements"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDMath.divide(self.nda, value.nda))

    def __rdiv__(self, value):
        """Divide by scalar or by other array elements"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDMath.divide(value.nda, self.nda))

    def __idiv__(self, value):
        """Divide value by scalar or element-by-element"""
        if isinstance(value, ndarray):
            NDMath.divide_elements(self.nda, value.nda)
        else:
            NDMath.divide_elements(self.nda, value)
        return self
    
    def __pow__(self, value):
        """Raise array elements to power specified by value"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDMath.power(self.nda, value.nda))

    def __rpow__(self, value):
        """Raise array elements to power specified by value"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDMath.power(value.nda, self.nda))
    
    def __eq__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.equal_to(self.nda, value.nda))
    
    def __ne__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.not_equal_to(self.nda, value.nda))

    def __lt__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.less_than(self.nda, value.nda))

    def __le__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.less_equal(self.nda, value.nda))

    def __gt__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.greater_than(self.nda, value.nda))

    def __ge__(self, value):
        """Element-wise comparison"""
        if not isinstance(value, ndarray):
            value = array([ value ])
        return ndarray(NDCompare.greater_equal(self.nda, value.nda))

    def __abs__(self):
        """Element-wise absolute values"""
        return ndarray(NDMath.abs(self.nda))
    
    def any(self):
        """Determine if any element is True (not zero)"""
        return NDCompare.any(self.nda)

    def all(self):
        """Determine if all elements are True (not zero)"""
        return NDCompare.all(self.nda)
    
    def sum(self):
        """Returns sum over all array elements"""
        return NDMath.sum(self.nda)

    def max(self):
        """Returns maximum array element"""
        return NDMath.max(self.nda)

    def min(self):
        """Returns minimum array element"""
        return NDMath.min(self.nda)

    def nonzero(self):
        """Return the indices of the elements that are non-zero.
           Returns a tuple of arrays, one for each dimension of a, containing the indices of the non-zero elements in that dimension.
           
           Compared to numpy, it does not return a tuple of arrays but a matrix,
           but either one allows addressing as [dimension, i] to get the index of the i'th non-zero element
        """
        return ndarray(NDCompare.nonzero(self.nda))
    
    def __str__(self):
        return self.nda.toString()
    
    def __repr__(self):
        if self.dtype == float:
            return "array(" + self.nda.toString() + ")"
        return "array(" + self.nda.toString() + ", dtype=" + str(self.dtype) + ")"
    

def zeros(shape, dtype=float):
    """zeros(shape, dtype=float)
    
    Create array of zeros, example:
    
    zeros( (2, 3) )
    """
    return ndarray(NDMatrix.zeros(dtype, __toNDShape__(shape)))


def ones(shape, dtype=float):
    """ones(shape, dtype=float)
    
    Create array of ones, example:
    
    ones( (2, 3) )
    """
    return ndarray(NDMatrix.ones(dtype, __toNDShape__(shape)))


def array(arg, dtype=None):
    """Create N-dimensional array from data
    
       Example:
          array([1, 2, 3])
          array([ [1, 2], [3, 4]])
    """
    if dtype is None:
        if isinstance(arg, ndarray):
            return ndarray(arg.nda.clone())
        else:
            return ndarray(NDArray.create(arg))
    return ndarray(NDArray.create(arg, dtype))


def arange(start, stop=None, step=1, dtype=None):
    """arange([start,] stop[, step=1])
    
    Return evenly spaced values within a given interval.
    
    Values are generated within the half-open interval ``[start, stop)``
    (in value words, the interval including `start` but excluding `stop`).
    
    Parameters
    ----------
    start : number, optional
        Start of interval.  The interval includes this value.  The default
        start value is 0.
    stop : number
        End of interval.  The interval does not include this value.
    step : number, optional
        Spacing between values.  For any output `out`, this is the distance
        between two adjacent values, ``out[i+1] - out[i]``.  The default
        step size is 1.  If `step` is specified, `start` must also be given.
        
    Examples:
    
    arange(5)
    arange(1, 5, 0.5)
    """
    if stop is None:
        # Only one number given, which is the 'stop'
        stop = start
        start = 0
    if dtype is None:
        return ndarray(NDMatrix.arange(start, stop, step))
    else:
        return ndarray(NDMatrix.arange(start, stop, step, dtype))
    

def linspace(start, stop, num=50, dtype=float):
    """linspace(start, stop, num=50, dtype=float)
    
    Return evenly spaced values from start to stop, including stop.
    Example:
    linspace(2, 10, 5)
    """
    return ndarray(NDMatrix.linspace(start, stop, num, dtype))

def any(value):
    """Determine if any element is True (not zero)"""
    return value.any()

def all(value):
    """Determine if all elements are True (not zero)"""
    return value.all()

def sum(array):
    """Returns sum over all array elements"""
    return array.sum()

def sqrt(value):
    """Determine square root of elements"""
    if not isinstance(value, ndarray):
        if value < 0:
            return nan
        return math.sqrt(value)
    return ndarray(NDMath.sqrt(value.nda))

def exp(value):
    """Determine square root of elements"""
    if not isinstance(value, ndarray):
        return math.exp(value)
    return ndarray(NDMath.exp(value.nda))

def log(value):
    """Determine log of elements"""
    if not isinstance(value, ndarray):
        return math.log(value)
    return ndarray(NDMath.log(value.nda))

def log10(value):
    """Determine log of elements (base 10)"""
    if not isinstance(value, ndarray):
        return math.log10(value)
    return ndarray(NDMath.log10(value.nda))

def copy(a):
    """copy(array):
       Create a copy of an array
    """
    return ndarray(a.nda.clone())

def reshape(a, shape):
    """reshape(array, shape):
    Create array view with new shape
    
    Example:
    reshape(arange(6), (3, 2))
    results in array([ [ 0, 1 ], [ 2, 3 ], [ 4, 5 ] ])
    """
    return ndarray(NDMatrix.reshape(self.nda, __toNDShape__(shape)))

def transpose(a, axes=None):
    """transpose(a, axes=None):
       Permute the axes of an array.
       By default, they are reversed.
       In a 2D array this would swap 'rows' and 'columns'
    """
    if axes is None:
        return a.transpose()
    return ndarray(NDMatrix.transpose(a.nda, axes))

def dot(a, b):
    """dot(a, b):
       Determine matrix 'dot' product of arrays a and b
    """
    result = ndarray(NDMatrix.dot(a.nda, b.nda))
    if result.ndim == 1 and len(result) == 1:
        return result[0]
    return result
