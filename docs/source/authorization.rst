Authorization
=============

Authentication vs Authorization
-------------------------------

Phoebus depends on the operating system to authenticate the user.
The currently logged in user is who we expect to be interacting with Phoebus.

Phoebus does add basic authorization to control if the current user may
configure details of the window layout (lock and unlock panes)
or alarm system (add, remove, acknowledge alarms).

Configuring Authorization
-------------------------

A preference setting selects a more detailed authorization configuration file::

        org.phoebus.ui/authorization_file=/path/to/authorization.conf

See details of the ``org.phoebus.ui`` preferences for the possible locations
of that file.

Example authorization configuration file::

        # Authorization Settings
        #
        # Format:
        # authorization = Comma-separated list of user names
        #
        # The authorization name describes the authorization.
        # FULL is a special name to obtain all authorizations.
        #
        # Each entry in the list of user names is a regular expression for a user name.

        # Anybody can lock a dock pane, i.e. set it to 'fixed'
        lock_ui = .*

        # Anybody can acknowledge alarms
        alarm_ack = .*

        # Specific users may configure alarms, including both "jane" and "janet"
        #alarm_config = fred, jane.*, egon, 

        # Anybody can configure alarms
        alarm_config = .*

        # Full authorization.
        FULL = root
