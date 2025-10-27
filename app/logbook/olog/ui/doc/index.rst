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

Missing features
----------------
In contrast to other markup implementations, HTML tags are **not** supported. Any such tags entered by user will
be rendered as plain text.


Launching the log entry editor
------------------------------
The log entry editor is launched as a non-modal window using one of the following methods:

- From the dedicated button in the application toolbar.

- From application menu Applications -> Utility -> Send to Logbook.

- Using the New Log Entry button in the log entry details view of the logbook application.

- Using the New Log Entry context menu item in the search result list view of the logbook application. This option also supports the keyboard combination CTRL+N.

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
Here user may attach any number of files of arbitrary types:

- ``Add Files`` will launch the native file browser dialog from which user may select any number of files.
- ``Clipboard`` will attach the file - if any - currently copied to the host OS clipboard.
- ``CSS Window`` will attach an image of the current application window.
- ``Embed New`` will launch the dialog to embed an image to the log entry body, see below.
- ``Embed Selected`` will embed user selected image files previously added to the list of attachments.

**NOTE**: The Olog service will not accept upload of attachments larger than the configured limit of 50MB. The Olog service
can be configured to use a different limit, but users should keep in mind that download of large attachments to
the log viewer may incur delays in terms of UI updates.

**NOTE**: Since iOS 11 the default camera image format is HEIC/HEIF (High-Efficiency Image Format). This type of
image file is not supported. Consequently upload of HEIC files is blocked by the application. Moreover, HEIC files converted to JPEG
in native Mac OS applications (e.g. Preview) may also fail to render and are also blocked from upload.

Embedded images
---------------
Images may be embedded in the body text using markup. The user should consult the quick reference (Markup Help button)
for details on how to do this. In general, users should use the Embed Image button to add image markup at the cursor position:

.. image:: images/EmbedImage.png
External image resources may be edited manually, e.g.:
``![alt-text](https://foo.com/bar.jpg)``. 
File URLs are not supported.

Links
-----
Links contained in a log entry will be opened in the default browser rather than in the view showing the log entry.

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

User may choose to hide some details of each log entry in the list in order to fit more items in the view and to reduce the need
for scrolling. This can be done using the keyboard shortcut ``CTRL+SHIFT+D``, or by selecting the
``Show/Hide Details`` item from the context menu invoked through a right click in the table view. The choice
to show or hide details is persisted between restarts of the application.

.. image:: images/ContextMenuLogEntryTable.png

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

Query history
^^^^^^^^^^^^^

Search queries entered by the user are put onto a first-in-first-out query history list. A button next to the search
field will expand a drop-down box to show previously used queries, see screen shot below. Queries are ordered by last-used-time
where the most recent query is on top. When new queries are entered by user, older queries may be
flushed out as the maximum size of the list is limited (15 by default, configurable between 5 and 30). The "default"
search query - rendered in bold font in the list - as defined in the preferences is however never flushed.

When user has selected a query from the list, a search button (up or down arrow) must be clicked in order to dispatch the search request.
Pressing ENTER when editing a query in the search field will also trigger a search, and the query is put in
the history list.

.. image:: images/QueryHistory.png

Pagination
^^^^^^^^^^

Each search request will retrieve a limited number of matching log entries to render in the list view. This limit
- aka "page size" - defaults to 30, but may be changed by a property value override. In addition, user may override the
default page size in the UI. Page size must be between 1 and 999. If the search results in a hit count larger
than the page size, the UI will render page navigation buttons below the list of log entries. The current page and
total number of pages is also shown, see screen shot.
The navigation buttons are not rendered if hit count less or equal to the page size.

.. image:: images/pagination.png

.. _preferences:

Periodic Search
^^^^^^^^^^^^^^^

When a user-initiated search request has completed, a background task is launched to repeatedly (once every 30 seconds) perform a new search
using the same search query. If the user edits the query to launch a new search request, the current periodic search is
aborted and re-launched when the search request completes.

The periodic search feature will consequently keep the list of matching queries updated when new log entries matching the current query are added.

Any failure in a search request - whether manually triggered by the user or by the background task - will abort the
periodic search. User will need to trigger another search request to restart the process.

Attachment Preview
------------------

When viewing a log entry, attachments are listed in the attachments view. A preview of an image attachment is shown
when user selects it. To see the attachment in full resolution, user may click on the preview image, or double-click
in the attachment list.

If user double-clicks on a OPI file attachment (.bob file), the application will launch that OPI in run mode.

If user double-clicks on a Data Browser attachment (.plt file), the application will launch the Data Browser.

Preview of non-image files is not offered in the application. However, external viewers may be configured for
arbitrary file extensions, see preference_settings_ (framework.workbench) for more information.

Log Entry Grouping
------------------

The preference setting ``log_entry_groups_support`` - if set to ``true`` - will enable the "log entry grouping"
feature. With this users will be able to reply to individual log entries implicitly creating a group of log entries. To use this
feature user can choose to:

- Press the Reply button shown in the log entry view:

.. image:: images/ReplyToLogEntry.png

- Select "Group Selected Entries" from the context menu shown on right click in the search result table view. This menu item is enabled when at least two items are selected:

.. image:: images/ContextMenuLogEntryTable.png

Log entries that are contained in a log entry group are rendered with a "reply" icon in the search result table view:

.. image:: images/ReplyAnnotation.png

In the log entry view, the "Show/Hide Group" button (see screen shot above) can be used to show all log entries of a group sequentially,
ordered on created date with oldest log entry on top. In this merged view attachments and properties are not shown.
Clicking on a header in the merged view will show that log entry in full.

**NOTE**: To be able to group log entries user must be authenticated in one of the following manners:

* Use "credentials caching" through preference setting ``org.phoebus.logbook.olog.ui/save_credentials``. Once a log entry has been created, credentials will be reused when creating a group.
* Use the Credentials Management app to sign in to the logbook context.

Limitations
^^^^^^^^^^^

Please consider the following limitations of the log entry group feature:

- A log entry group should not be regarded as a discussion thread.
- There is no support for "groups of groups", or "sub-groups".
- There is no parent-child relation between log entries in a group, i.e. there is no internal structure of the log entries in a group.
- A log entry may be included in only one log entry group. It is hence not possible to create a new group of log entries if these are already contained in different groups.

Preferences
-----------
Preferences related to the electronic logbook are the following:

- ``org.phoebus.olog.es.api/olog_url``. This should be on the format ``http(s)://foo.com/Olog``, where the path element ``Olog`` may not be omitted.

- ``org.phoebus.logbook.olog.ui/default_logbooks``. This is a comma separated list of logbooks automatically associated with a new log entry.

- ``org.phoebus.logbook.olog.ui/level_field_name``. The text shown next to the drop-down below the password field. Sites may wish to customize this to override the default value "Level".

- ``org.phoebus.olog.es.api/levels``. List of items shown in the "Level" drop-down.
  
- ``org.phoebus.logbook.ui/save_credentials``. Indicates if user credentials should be cached. If ``true``, the user will
  have to specify credentials only for the first new log entry after launch of CS Studio. The side effect of credentials caching is that all entries will be created with the same user (owner) identity.

- ``search_result_page_size``. The maximum number of hits per page to fetch and render in a search. User may override in the UI. Value must be 1 - 999, default is 30.

- ``log_entry_groups_support``. If true, user may reply to log entries and create a log entry group from a selection of existing log entries.




