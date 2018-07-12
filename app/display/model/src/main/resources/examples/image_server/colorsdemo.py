# Generates RGB images.

from simple_image import createImage as createSimpleImage
from ntndarray import createImage as createNormativeImage
from pvaccess import PvaServer, PvScalarArray, STRING
from array import array
from time import sleep

data = ([0] * 20 + [127] * 10 + [255] * 10) * 1000
image = createSimpleImage(0, data, (100, 400), 'B')
server = PvaServer('image', image)

#a palette of 20x20 squares 6 hues wide and 5 shades tall
data = array('B')
shades = range(255, 0, -51)
rgb_ratios = ((1, 0, 0), (.8, .8, 0), (0, 1, 0), (0, .8, .8), (0, 0, 1), (.8, 0, .8))
for i in range(len(shades)):
    for repeat_row_count in range(20):
        for j in range(len(rgb_ratios)):
            data.extend([int(255-shades[i]*(1-rgb_ratios[j][k])) for k in range(3)] * 20)

width = len(rgb_ratios) * 20
height = len(shades) * 20

def stepImages():
    global data
    new_data = array('B')
    data_rgb2 = array('B')
    for y in range(height):
        new_row = data[y*width*3+20 : (y+1)*width*3] + data[y*width*3 : y*width*3+20]
        new_data.extend(new_row)
        data_rgb2.extend(new_row[0::3] + new_row[1::3] + new_row[2::3])
    data = new_data
    data_rgb3 = data[0::3] + data[1::3] + data[2::3]
    
    return (createSimpleImage(1, data, (3, width, height)),
    		createSimpleImage(2, data_rgb2, (width, 3, height)),
    		createSimpleImage(3, data_rgb3, (width, height, 3)),
		createSimpleImage(4, [x-128 for x in data], (3, width, height), 'b') )

names = ('rgb1_image', 'rgb2_image', 'rgb3_image', 'rgb1_signed')
for num, name in enumerate(names):
	server.addRecord(name, createSimpleImage(num, [], (), 'B'))

#10 rows, each row is 10 pix each of pure red, green, blue
data2 = [255] * 10 + [0] * 10 + [0] * 10 + \
        [0] * 10 + [255] * 10 + [0] * 10 + \
        [0] * 10 + [0] * 10 + [255] * 10
data2 = array('B', data2*5 + (data2[30:] + data2[:30])*5)
server.addRecord('rgb2_blocks', createSimpleImage(4, data2, (30, 3, 10)))
data3 = ([255]*10 + [0]*20)*5  +  ([0]*10 + [255]*10 + [0]*10)*5 + \
        ([0]*10 + [255]*10 + [0]*10)*5   +   ([0]*20 + [255]*10)*5 + \
        ([0]*20 + [255]*10)*5  +  ([255]*10 + [0]*20)*5
server.addRecord('rgb3_blocks', createSimpleImage(5, data3, (30, 10, 3), 'B'))
data3 = array('b', [255-x-128 for x in data3])
server.addRecord('rgb3_inverted', createSimpleImage(5, data3, (30, 10, 3)))

namesPv = PvScalarArray(STRING)
namesPv.set(server.getRecordNames())
server.addRecord('names', namesPv)

while True:
    for num, image in enumerate(stepImages()):
        server.update(names[num], image)
    sleep(1)

