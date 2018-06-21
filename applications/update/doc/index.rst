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
latest product.


Example:

  org.phoebus.applications.update/current_version=2018-06-18 13:10
  org.phoebus.applications.update/update_url=http://my.site.org/snapshots/phoebus/product-for-my-site.zip



Usage
-----

On startup, the update mechanism checks the `update_url`.
If that file is dated after the `current_version`, an "Update" button is added
to the status bar to indicate that an update is available.

Clicking that "Update" button opens a dialog with details on the current version,
the updated version, and the installation location that will be replaced
in the update.

When pressing "OK", the update is downloaded and the current installation is replaced.
Finally, a prompt indicates completion of the update, and the product exists
for you to start the updated version.
