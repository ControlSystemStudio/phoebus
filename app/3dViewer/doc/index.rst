3d Viewer
=========

Overview
--------

The 3d Viewer is a tool that allows users to configure 3 dimensional structures using spheres, cylinders, and boxes.

These structures are defined in shape file (*.shp) and parsed by the application. 

The resultant structure is then  rendered on screen. This structure can be viewed in the application which allows rotation, zoom, and movement.

The individual spheres, cylinders, and boxes that make up the structure can have their coordinates, sizes, and colors specified.

Shape (.shp) File Syntax
------------------------

The viewer parses shape files from beginning to end. Any error in the shape file will cause the parsing of the entire file to fail and no resulting
structure will be rendered.

**Background Color**
  The background color of the viewer can be controlled using the following command.
  
    ``background(r, g, b, A)``
    
  This command has four parameters. The red, green, and blue values for the color are the first three. Each color value is an integer from 0 through 255. 
  These are followed by the alpha value which allows you to control the transparency of the color. Alpha is a floating point number in the range [0, 1].
  An alpha of 0 is transparent while an alpha of 1 is opaque.
  
  Multiple background colors may be defined, however only the last defined background color will be used.
  
**Spheres**
  A sphere may be defined using the following command.
  
    ``sphere(x, y, z, R, r, g, b, A)``
  
  This command has eight parameters. The first three parameters are the x, y, and z values which represent the center point of the sphere in the three dimensional space.
  The x, y, and z parameters are floating point values. These are followed by the radius of the sphere, another floating point value.
  The final four parameters are the red, green, blue, and alpha values used to define the color of the sphere. Red, green, and blue are integer values in [0, 255] and 
  alpha is a floating point value in [0, 1].

**Cylinders**
  A cylinder may be defined using the following command.
  
    ``cylinder(x1, y1, z1, x2, y2, z2, R, r, g, b, A)``
    
  This command has eleven parameters. The first three parameters are the x, y, and z values which represent one end point of the cylinder. The second three parameters 
  are the x, y, and z values which represent the other end point of the cylinder. All x, y, and z parameters are floating point values. These are followed the cylinder's 
  radius. The radius is a floating point value. The final four parameters are the red, green, blue, and alpha values used to define the color of the sphere. Red, green, 
  and blue are integer values in [0, 255] and alpha is a floating point value in [0, 1].

**Boxes**
  A box may be defined using the following command.
  
    ``box(x1, y1, z1, x2, y2, z2, r, g, b, A)``
  
  This command has 10 parameters. The first three parameters are the x, y, and z values which represent one corner of the box.The second three parameters are the x, y, and z 
  values which represent the opposite corner of the box. All x, y, and z parameters are floating point values. The final four parameters are the red, green, blue, and alpha 
  values used to define the color of the sphere. Red, green, and blue are integer values in [0, 255] and alpha is a floating point value in [0, 1].
  
  If the first corner of a box was defined at (0, 0, 0) and the second corner at (100, 100, 100) then the box would have one corner at the origin and one corner at (100, 100, 100).
  Each of the boxes sides would be of length 100, and the boxes center point would be (50, 50, 50).

**Comments**
  Shape files can have comments. A comment is a line of text that starts with a '#'. This line will be ignored when parsing the shape file.

Example Shape File
------------------

::

    # This is a comment. It will be ignored when the file is parsed.
    
    # The background of the viewer is set to be nearly black.
    background(32, 32, 32, 1)
    
    # A blue box is defined with one corner at the origin, and the opposite
    # corner at (100, 100, 100). This will result in a cube with each side
    # being of magnitude 100.
    box(0, 0, 0, 100, 100, 100, 0, 0, 255, 1)
    
    # A red sphere of radius 10, is placed at each corner of the previously
    # defined box.
    sphere(  0,   0,   0, 10, 255, 0, 0, 1)
    sphere(100,   0,   0, 10, 255, 0, 0, 1)
    sphere(  0,   0, 100, 10, 255, 0, 0, 1)
    sphere(100,   0, 100, 10, 255, 0, 0, 1)
    sphere(  0, 100,   0, 10, 255, 0, 0, 1)
    sphere(100, 100,   0, 10, 255, 0, 0, 1)
    sphere(  0, 100, 100, 10, 255, 0, 0, 1)
    sphere(100, 100, 100, 10, 255, 0, 0, 1)

**Resulting Structure**

.. image:: example_struct.png
   :width: 50%
