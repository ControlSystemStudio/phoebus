Olog
====
Olog is an electronic logbook client for the logbook service maintained here: https://github.com/Olog/olog-es.

**NOTE**: this is an optional module. For information on how to build a site specific product, see
https://github.com/ControlSystemStudio/phoebus/tree/master/phoebus-product.

Features
--------
- Arbitrary number of "logbooks", configured in the service. A logbook entry is contained in one or several logbooks.

- Arbitrary number of "tags", configured in the service. A logbook entry may be associated with zero or several tags.

- Arbitrary number of "properties", configured on the service. A property is a named list of key/value pairs. The user may define values for the items in a property. A logbook entry is associated with zero or several properties.

- Arbitrary number of attachments, i.e. images or other file types.
  
- Markup as defined by the Commonmark specification (https://commonmark.org).
  
- Log entry editor invocation from context menu whereby context specific attachments or data are automatically appended to the log entry.

- Log entry viewers offer search capabilities based on meta data and content.

Launching the log entry editor
------------------------------
To launch the log entry editor, the user may select Applications -> Utility -> Send to Logbook from the menu:

.. image:: images/SendToLogbook.png

The log entry editor may also be launched from context menus, where applicable. For instance, with a right click on
the background of an OPI the launched context menu will include the Create Log item:

.. image:: images/ContextMenu.png
The Create Log context menu item is available also in a Databrowser plot area.

Editing a log entry
-------------------
The log entry editor is a non-modal dialog:

.. image:: images/LogEntryEditor.png

Mandatory data are:

- Username and password, see also preferences_.
  
- Title
  
- At least one logbook, see also preferences_. Additional logbooks - configured in the service - can be added from a list shown when pressing the down button:

.. image:: images/LogbookSelection.png

The body text of the log entry can be styled using markup as defined by the Commonmark specification
(https://commonmark.org). The Markup Help button will launch the system default browser to display a quick
reference.

Attachments
-----------
When the log entry editor is launched from a context menu, a screen shot is automatically appended, where applicable.
Additional images (or other type of attachments) may be added by expanding the Attachments editor:

.. image:: images/Attachments.png
Here user may select an image file from the file system (Add Image), a screen shot of the current CS Studio "desktop"
(CSS Window) or paste an image from the clipboard (Clipboard Image).

Other type of attachments are managed in the Files tab.

Embedded images
---------------
Images may be embedded in the body text using markup. The user should consult the quick reference (Markup Help button)
for details on how to do this. In general, users should use the Embed Image button to add image markup at the cursor position:

.. image:: images/EmbedImage.png
External image resources may be edited manually, e.g.:
``![alt-text](https://foo.com/bar.jpg)``. 
File URLs are not supported.

Properties
----------
Properties are edited by expanding the Properties editor. The below screen shot shows that one single property
(LCR shift info) holding five keys has been configured in the service:

.. image:: images/PropertiesEditor.png
User may select what properties to include in the log entry, and edit the values for the items in the property.


Log entry viewer
----------------
The menu item Applications -> Utility -> Log Entry Table will launch an application (in a new tab) in which the user may
search and view log entries:

.. image:: images/LogEntryTable.png
In the search field the user may specify criteria when searching for log entries. These criteria are based on 
the elements of a log entry as follows:

- ``desc`` or ``description``: The body text, whereby any markup characters are ignored. The search is case insensitive.

- ``title``: The title of the log entry. The search is case insensitive.

- ``level``: The value of the Level field.
  
- ``logbooks``: A comma separated list of logbook names. Log entries contained in either of the listed logbooks will match.

- ``tag``: A comma separated list of tag names. Log entries tagged with either of the listed tags will match.

- ``owner``: The author of a log entry as specified in the Username field when the entry was created.

- ``start``: Defines the start date limit in a search. Time may be specified using the format ``yyyy-MM-dd HH:mm:ss.SSS`` or a relative time like "8 hours" or "2 days".

- ``end``: Defines the end date limit in a search. Time may be specified using the format ``yyyy-MM-dd HH:mm:ss.SSS`` or a relative time like "8 hours" or "2 days". The value "now" is supported.

- ``properties``. Both property names as well as key name and value of the items in a property can be searched like so:

    - ``properties=property name`` find log entries containing a property named "property name"

    - ``properties=property name.key name`` find log entries containing a property names "property name" and that contains a key named "key name".

    - ``properties=property name.key name.value`` find log entries containing a property named "property name" and that contains a key named "key named" with a value of "value".

    - ``properties=property name 1|property name 2`` find log entries containing a property named "property name 1" **or** a property named "property name 2". The pipe character is used to separate search expressions.

.. _preferences:

Preferences
-----------
Preferences related to the electronic logbook are the following:

- ``org.phoebus.olog.es.api/olog_url``. This should be on the format ``http(s)://foo.com/Olog``, where the path element ``Olog`` may not be omitted.

- ``org.phoebus.logbook.olog.ui/default_logbooks``. This is a comma separated list of logbooks automatically associated with a new log entry.

- ``org.phoebus.logbook.olog.ui/level_field_name``. The text shown next to the drop-down below the password field. Sites may wish to customize this to override the default value "Level".

- ``org.phoebus.olog.es.api/levels``. List of items shown in the "Level" drop-down.
  
- ``org.phoebus.logbook.ui/save_credentials``. Indicates if user credentials should be cached. If ``true``, the user will
  have to specify credentials only for the first new log entry after launch of CS Studio. The side effect of credentials caching is that all entries will be created with the same user (owner) identity.








