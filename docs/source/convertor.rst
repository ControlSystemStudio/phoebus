=================
AdvancedConvertor
=================

OPI to BOB convertor
--------------------


Table:

+ Description
+ Command Line
+ Converter application

Description
-----------

AdvancedConverter is a tool to convert massively and recursively CSS OPI to Phoebus BOB files. It will automatically convert the widgets and their properties from the legacy file format.

The converter takes folder or files and convert any OPI files into a BOB files (without delete previous OPI files).
The programm keep the hierarchy, subfolders, scrypt and files inside. 

Command Line
------------
To use this AdvancedConvertor, you can do it via this command line invocation:

Usage: Usage: -main org.csstudio.display.builder.model.AdvancedConverter [-help] [-output /path/to/folder] </path/to/opi/folder>

Converts BOY "*".opi files to Display Builder "*".bob format

**-output** /path/to/folder   - Folder into which converted files are written

<files>                   - One or more files to convert

Exemples : 
 *Convert and copy in another folder*

**-main org.csstudio.display.builder.model.AdvancedConverter** *-output* **output/path/to/folder input/path/to/folder**

*Convert and stay in the folder*

**-main org.csstudio.display.builder.model.AdvancedConverter  input/path/to/folder**

Converter application
---------------------

Located in *Utility -> OPI converter*, it will generate a pop up window. In this pop up, you can choose a input file or folder with the Browse button in the input section. In a similar way, you can choose or not a output folder.
To run the conversion you need to press the run button. 

If the output is empty, the conversion will be done in the input folder.

Right before the conversion, you might have an overriding message window. It appear when you already converted a file in the output folder.

If you select **YES**, it will **delete** all bob files present in the output folder and process the conversion normaly and convert evey opi files. 

If you select **NO**, you return in the browsing section.

During the conversion, you can follow the conversion process with the progress bar. When the conversion is done, you return in the browsing section and can close the window with the "x" button or rerun a conversion.
