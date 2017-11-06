# Helper script to imagedemo.py which extracts image data and stores it for later use.
#
# Pickles images in the form of tuples (width, height, data, name), where width, height,
# and name are the width, height, and name of the image, respectively, and data is the pixels
# of the image.
#
# Where the image has 3 color channels (RGB) or 4 (RGBA), the channels may be selected for the
# image using command line arguments. (Help: python pickle_image.py -h)
# Example: python pickle_image.py images.pkl *-rgb.png --red *-gs.png
#
# Requires pyPNG installed.

import png, sys, pickle, string, argparse
from array import array

# Stuff about parsing the command line.
# You can choose which color channels to use from each png.
parser=argparse.ArgumentParser(description='pickle some PNG images')
parser.add_argument('pkl', help='Pickle file')
parser.add_argument('--append', dest='mode', const='a', action='store_const', help="Append to pickle file")
parser.add_argument('--write', dest='mode', const='w', action='store_const', default='w', help="Write to (truncate) pickle file (default)")
colors = ('red', 'green', 'blue', 'alpha')
class ColorAction(argparse.Action):
    def __init__(self, option_strings, dest, nargs='+', **kwargs):
        super(ColorAction, self).__init__(option_strings, dest=dest, nargs=nargs, **kwargs)
    def __call__(self, parser, namespace, values, option_string=None):
        if not hasattr(namespace, 'colors') or getattr(namespace, 'colors') is None:
            setattr(namespace, 'colors', {None : []})
        if '--colors' in self.option_strings:
            for c in values[0]:
                __call__(self, parser, namespace, values[1:], '-' + c)
        else:
            colors_dict = namespace.colors
            color_seq = 'rgb' if option_string is None or self.option_strings[0][1] is 'n' \
                            else self.option_strings[0][1]
            for name in values:
                if name not in namespace.colors[None]:
                    namespace.colors[None].append(name)
                if not name in namespace.colors:
                    namespace.colors[name] = color_seq
                else:
                    for color in color_seq:
                        if not color in namespace.colors[name]:
                            namespace.colors[name] = namespace.colors[name] + color
parser.add_argument('png', nargs='*', action=ColorAction, help='PNG images to be pickled (all available color channels)')
for i, color in enumerate(colors):
    parser.add_argument('-' + color[0], '--' + color, nargs='+', dest="colors", metavar="PNG",
            action=ColorAction, help="Select %s (%dth) color channel" % (color, i+1))
parser.add_argument('-n', '--none', '--any', nargs='+', dest="colors", metavar="PNG",
        action=ColorAction, help="Select none/any/all color channels")
parser.add_argument('-c', '--colors', nargs='+', dest="colors", action=ColorAction, metavar="PNG",
        help='Select specified color channels (r=red, g=green, b=blue, a=alpha, n=none/any/all)')

args = parser.parse_args()

pkl = open(args.pkl, args.mode if args.mode is not None else 'w')
#for each image file, read and pickle
for filename in args.colors.get(None, ()):
    try:
        img = open(filename, 'r')
        img_info = png.Reader(img).read()
        x, y = (img_info[0], img_info[1])
        z = img_info[3].get('planes', 1)
        it = img_info[2]
        data = sum(it, it.next())
        data = array('h', data)
        if z == 3 or z == 4:
            for i, color in enumerate(('.red', '.green', '.blue', '.alpha')):
                if i >= z: break
                colors = args.colors.get(filename, '')
                if color[1] in colors:
                    name = filename + color if len(args.colors[filename]) > 1 or 'n' in colors else filename
                    pickle.dump((x, y, data[i::3], name), pkl)
                    print("Pickled %s channel for image %s as %s" % (color[1:], filename, name))
        elif z == 1:
            pickle.dump((x, y, data, filename), pkl)
            print("Pickled image %s" % filename)
        else:
            print("Unexpected dimensions for %s: (%d, %d, %d)" % (x, y, z, filename))
    except Exception as e:
        print("Cannot pickle image %s due to %r" % (filename, e))
