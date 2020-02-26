Runtime Settings
================

When you run Phoebus, you may open a tab with the "PV Tree",
enter a PV name, move the window around etc.

When you exit Phoebus, the current location of windows, tabs,
and for example the PV name of an active "PV Tree"
are stored.

When you later restart Phoebus, it restores the windows
with their saved location and content.


Developer Notes
---------------

The state is persisted in a ``memento`` file,
located in the same directory as the user preferences.
To change the default from ``.phoebus/memento`` in your home directory
to a different location, see Preference Settings :ref:`preferences-notes`.

To always start Phoebus with a known window layout,
save the ``memento`` from a desired layout,
and then place that saved file in the user preferences directory
before starting a new instance of Phoebus.
