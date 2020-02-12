.. _locations:

Locations
=========

Phoebus uses the following Java System properties
to locatehelp files, saved state etc.

These system variables are typically set automatically
as described below, but when necassary they can be
set when starting the product.

``phoebus.install``:
   Location where phoebus is installed.
   Has subdirectories ``lib/``, ``doc/``,
   and is used to locate the online help.
   Is automatically derived from the location
   of the framework JAR file.

``phoebus.user``:
   Location where phoebus keeps the memento
   and preferences.
   Defaults to ``.phoebus`` in the user's home directory.

Site-Specific Branding and Settings
-----------------------------------

The ``phoebus.install`` location is used for branding and site-specific settings.

``settings.ini``:
   At startup, Phoebus load preferences from this file if it is found
   in the install location.
   This allows packing site-specific settings into your product.
   
``site_splash.png``:
   This image will replace the default spash screen background
   with a site-specific version.
   It should be sized 480x300.

``site_logo.png``:
   This 64x64 sized image will replace the default window logo.
   
