# Defines the structures and functions necessary to create an "image" PV which
# PARTIALLY conforms to the EPICS v4 Normative Type NTNDArray, the preferred image type.
# The image defines a minimal selection of fields necessary for display purposes.
# This image is a PvObject, suitable for serving with a PvaServer.


from pvaccess import BOOLEAN, BYTE, UBYTE, SHORT, USHORT, INT, UINT, LONG, ULONG, FLOAT, DOUBLE, STRING, PvObject, PvaServer
from copy import copy
from array import array
from struct import calcsize
from itertools import repeat

# Dicts for transforming between arrays (array.array type) and PvArray values
typecode_to_name = {'b' : 'byte', 'B' : 'ubyte', 'h' : 'short', 'H' : 'ushort', 'i' : 'int', 'I' : 'uint', 'l' : 'long', 'L' : 'ulong', 'f' : 'float', 'd' : 'double'}
pvtype_to_typecode = { BYTE : 'b', UBYTE : 'B', SHORT : 'h', USHORT : 'H', INT : 'i', UINT : 'I', LONG : 'l', ULONG : 'L', FLOAT : 'f', DOUBLE : 'd' }

# Defining sub-structures
#value_t: a restricted union of array types
value_t = ({'booleanValue' : [BOOLEAN],
            'byteValue' : [BYTE],
            'ubyteValue' : [UBYTE],
            'shortValue' : [SHORT],
            'ushortValue' : [USHORT],
            'intValue' : [INT],
            'uintValue' : [UINT],
            'longValue' : [LONG],
            'ulongValue' : [ULONG],
            'floatValue' : [FLOAT],
            'doubleValue' : [DOUBLE]},)
#dimension_t
dimension_t = {'size' : INT}
# Defining image structure
ndarray = {'value' : value_t,
        'dimension' : [dimension_t],
        'uniqueId' : INT}

# Setter methods
"""
Set the 'value' field of an image.

The param. 'array' should be an iterable like array.array or list,
with values of the same type, which type corresponds to 'typecode'.
If 'array' does not have a typecode attribute,
typecode should be either an array.array typecode
(one of 'b', 'B', 'h', 'H', 'i', 'I', 'l', 'L', 'f', 'd')
or a pvaccess pvtype
(one of BYTE, UBYTE, SHORT, USHORT, ..., ULONG, FLOAT, DOUBLE).
If 'array' does have a typecode attribute, its typecode is used
in place of the 'typecode' parameter.
"""
def setImage(image, array, typecode=None):
    if hasattr(array, 'typecode'):
        typecode = array.typecode
    if not isinstance(typecode, str):
        typecode = pvtype_to_typecode[typecode]

    value_field = typecode_to_name[typecode] + 'Value'
    value = [chr(x & 0xFF) for x in array] if typecode is 'b' else list(array)

    image['value'] = {value_field : value}

def setDimensions(image, sizes):
    dims = map(lambda size : {'size' : size}, sizes)
    image['dimension'] = dims

# Create method
def createImage(uniqueId, data, sizes, typecode=None):
    image = PvObject(ndarray, {'uniqueId' : uniqueId},'NTNDArray:1.0')
        #note: not truly 'epics:nt/NTNDArray:1.0' (not compliant with normative types),
        #so a simple 'NTNDArray:1.0' is used to identify it as an image
    setImage(image, data, typecode)
    setDimensions(image, sizes)
    return image

if __name__ == "__main__":
    image = createImage(0, [1,2,3], (3,), BYTE)

