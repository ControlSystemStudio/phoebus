Credentials Management
======================

Overview
--------

Some applications may need to prompt the user for credentials, e.g. when connecting to a protected
remote service. One such example would be an electronic logbook.

Phoebus may be configured to store the credentials entered by the user in order to not prompt repeatedly .
In order to also support an explicit logout capability, the Credentials Management application offers means to
remove stored credentials. This is in practice equivalent to "logging out" from a remote service.

The application is launched from the main window toolbar. There is no menu item entry to launch it.

.. image:: images/CredentialsManagement.png

The above screen shot shows that credentials have been stored for the "logbook" scope. Additional application
scopes may exist, and user may also choose to "logout" from all scopes, i.e. to remove all stored credentials.

