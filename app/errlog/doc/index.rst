Error Log
=========

The Error Log is a read-only panel that copies and displays application messages.
It simplifies checking "print" statements from scripts, or error messages that might
explain why some display doesn't work as expected.

By default, these messages appear in the terminal where the Phoebus product was started.
On MS Windows, however, there may not be a terminal.
A site-specific launch script might also redirect the standard and error output of the
product to a file.
To the end user, it is thus not always obvious how to locate the application messages.

The "Error Log" shows messages written to the standard output, the error output
or the console logger in one predictable place.
It captures messages from the time it is started until it is closed. 

The typical process to check for error message is thus:

 1) Open the Error Log
 2) Now open the display which is suspected of issuing a message
 3) Check the Error Log, then close when no longer needed