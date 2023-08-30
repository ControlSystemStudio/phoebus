Update
======

The 'update' application allows a product to self-update.

Assume you have a site-specific product, i.e. a ZIP file
that users at your site can download.
By including the 'update' application in your product
and setting two preference settings, your product
can self-update.

Configuration
-------------

The ``current_version`` setting needs to provide the current version of your product
in the format ``YYYY-MM-DD HH:MM``.
The ``update_url`` setting needs to point to a file or web URL that contains the
latest product. You most likely need to create separate products for each
architecture, because for example the JavaFX libraries are specific to an architecture,
and you want system-specific launch scripts, batch files or Mac apps.
The URL can thus contain ``$(arch)`` which will be  will be replaced by
"linux", "mac" or "win".


Example:

  org.phoebus.applications.update/current_version=2018-06-18 13:10
  org.phoebus.applications.update/update_url=http://my.site.org/snapshots/phoebus/product-for-my-site-$(arch).zip

There are additional settings that allow re-writing the file names
of the downloaded ZIP file or skipping files.
For details, see full description of the update preference settings.


Usage
-----

On startup, the update mechanism checks the ``update_url``.
If that file is dated after the ``current_version``, an "Update" button is added
to the status bar to indicate that an update is available.

Clicking that "Update" button opens a dialog with details on the current version,
the updated version, and the installation location that will be replaced
in the update.

When pressing "OK", the update is downloaded into an ``update/`` folder below your
current install location.
Finally, a prompt indicates completion of the update, and the product exists
for you to start the updated version.
Your launch script needs to check for the presence of an ``update/`` folder.
If one is found, it can replace the current installation with the content
of the update folder, delete it, and start the new version.


Details
-------

Earlier versions directly replaced the ``lib/*.jar`` files with a downloaded
update, but on Windows these are locked by the running instance.
The downloaded files are thus staged in an ``update/`` folder,
and the launcher script replaces the previous files with the new ones
before starting the JVM which then locks the files as they are used.
