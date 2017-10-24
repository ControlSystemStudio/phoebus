#   Load images which were pickled in the form of a tuple (width, height, data, name),
# then serve them with a pvaccess.PvaServer.
#   Notice that there are two modules which define createImage.
#   One (ntndarray.createImage) will create a "true" NTNDArray image.
# This PV will define all the required fields for an NTNDArray (according
# to EPICS v4 normative types doc). In compliance with that document, its
# id tag is 'epics:nt/NTNDArray:1.0'.
#   The other (simple_image.createImage) will create a reduced version of that
# type, which defines only the necessary and/or recognized fields for the Image Widget
# of Display Builder. Its id tag is 'NTNDArray:1.0'

#from ntndarray import createImage
from simple_image import createImage
from sys import argv
from pvaccess import PvaServer, PvScalarArray, STRING
import pickle

def importPickledImageData(pkl):
    x, y, data, name = pickle.load(pkl)
    print(name)
    uid = reduce(lambda a, b: a + ord(b), [0] + list(name))

    print("w=%d, h=%d, num=%d, uid=%d, name=%s" % (x, y, len(data), uid, name))
    image = None

    image = createImage(uid, data, (x, y))

    return (name, image)

def iseof(file):
    pos = file.tell()
    if not file.read(1):
        return True
    file.seek(pos)
    return False

pkl = open(argv[1], 'r')
server = PvaServer()
while not iseof(pkl):
    name, image = importPickledImageData(pkl)
    if image:
        print("serving image with name '%s'\n" % name)
        server.addRecord(name, image)

data = ([0] * 20 + [1] * 20) * 1000
image = createImage(0, data, (100, 400), 'b')
server.addRecord('generated', image)

names = PvScalarArray(STRING)
names.set(server.getRecordNames())
server.addRecord('names', names)


raw_input("Enter anything to quit\n")

