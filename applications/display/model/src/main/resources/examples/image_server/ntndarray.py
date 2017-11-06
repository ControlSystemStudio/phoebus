# Defines the structures and functions necessary to create an "image" PV which
# conforms to the EPICS v4 Normative Type NTNDArray, the preferred image type.
# This image is a PvObject, suitable for serving with a PvaServer.

from pvaccess import BOOLEAN, BYTE, UBYTE, SHORT, USHORT, INT, UINT, LONG, ULONG, FLOAT, DOUBLE, STRING, PvObject, PvaServer
from copy import copy
from array import array
from struct import calcsize
from itertools import repeat

#idea: consider extending NtType

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
#codec_t
codec_t = {'name' : STRING, 'parameters' : ()}
#dimension_t
dimension_t = {'size' : INT,
               'offset' : INT,
               'fullSize' : INT,
               'binning' : INT,
               'reverse' : BOOLEAN}
#NTAttribute
ntattribute = {'name' : STRING,
                'value' : (),
                'description' : STRING,
                'sourceType': INT,
                'source' : INT}

# Defining NTNDArray (image) structure
ntndarray = {'value' : value_t,
            'codec' : codec_t,
            'compressedSize' : LONG,
            'uncompressedSize' : LONG,
            'dimension' : [dimension_t],
            'uniqueId' : INT,
            'attribute' : [ntattribute] }

# Initial values (copy before using)
init_codec = {'name' : ''} # don't touch parameters
init_attribute = {'name' : '', 'description' : '', 'sourceType' : 0, 'source' : ''}

# Setter methods
"""
Set the 'value', 'uncompressedSize', and 'compressedSize' fields of
an image from the given (assumed uncompressed) data.

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
    bytesize = calcsize(str(len(array))+typecode)
        #happily, the typecodes for array.array and struct.calcsize match
    value = [chr(x & 0xFF) for x in array] if typecode is 'b' else list(array)

    image['value'] = {value_field : value}
    image['uncompressedSize'] = bytesize
    image['compressedSize'] = bytesize

def setDimensions(image, sizes, offsets=repeat(0), binnings=repeat(1), reverses=repeat(False)):
#todo: offsets cumulative
    toDict_step3 = lambda fullSize, offset, binning, reverse, size : locals()
    toDict_step2 = lambda x : toDict_step3(*x)
    toDict = lambda x : toDict_step2(x + (x[0]-x[1],))
    dims = map(toDict, zip(sizes, offsets, binnings, reverses))
    image['dimension'] = dims

def setCodec(image, codec):
    image['codec'] = dict(init_codec, **codec)

#note: accepts an iterable of dictionaries, not a dictionary
#def setAttribute(image, attribute):
    #image['attribute'] = [dict(init_attribute, **a) for a in attribute]

# setters for Color Mode attribute
#def setColorMode(image, value):
    #todo: either replace or add a 'ColorMode' attribute from image['attribute']
#    setAttribute(image, ({'name': 'ColorMode', 'value' : ({'value' : value},), 'description': 'Color mode'},))

#def setMonoColor(image):
#    setColorMode(image, 0)

#def setRGB3Color(image):
#    setColorMode(image, 4)

# Create method
def createImage(uniqueId, data, sizes, typecode=None, offsets=repeat(0), binnings=repeat(1), reverses=repeat(False), codec = {}, attribute=[{}]):
    image = PvObject(ntndarray, {'uniqueId' : uniqueId},'epics:nt/NTNDArray:1.0')
    setCodec(image, codec)
    #setAttribute(image, attribute)
    setImage(image, data, typecode)
    setDimensions(image, sizes, offsets, binnings, reverses)
    
    return image

if __name__ == "__main__":
    image = createImage(0, [1,2,3], (3,), BYTE)
    setColorMode(image, 0)

