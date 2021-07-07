
Core Framework
==============

The core framework module of phoebus consists of a set of commonly used services, interfaces, and other utilities.


Selection Service
-----------------

The Selection Service provides a clean and powerful mechanism for allowing Phoebus applications to share the user selected
data. The use of Selection Service helps to build an integrated set of applications, capable of responding to user operations
in other applications, while still avoiding direct dependencies between applications and supporting a modular product.

The selection service allows applications to

 1. Publish their selection
 2. Register listeners to be notified of changes to the current selection


Adapter Service
---------------

The Adapter services provided by the framework is a means for Phoebus applications to provide runtime integration
between loosely coupled applications.

For example, most Phoebus applications needs to support making logbook entries. Without adapters, each application would
have to add a dependency to each of the supported logbook application. However, with the use of adapters each
application simply registers an AdapterFactory which describes how its model/selection can be adapted into an object
that can be used to create logbook entries. Additionally, by separating the AdapterFactory into a different module,
different adapters can be used by different users, each of which support different logbooks. This would not be possible
without the use of adapters due to the direct dependencies that would exist between the applications.


Here is an example where an application is including a context menu item to make log entires based on the selection.

case 1.

Without the use of Adapters and Selection service, each potential menu contribution has to be manually included/excluded.

.. code:: java

    ContextMenu menu = new ContextMenu();
    if (LogbookPreferences.is_supported)
        items.add(new SendLogbookAction(...));
    else if (LogbookPreferences.is_olog_supported)
        items.add(new SendOlogLogbookAction(...));
    else if (LogbookPreferences.is_elog_supported)
        items.add(new SendElogLogbookAction(...));

And the application would have an explicit dependency on module not necessarily needed.

.. code::

      <dependency>
        <artifactId>logbook-ui</artifactId>
         ...
      </dependency>
      <dependency>
        <artifactId>olog-logbook-ui</artifactId>
         ...
      </dependency>
      <dependency>
        <artifactId>elog-logbook-ui</artifactId>
         ...
      </dependency>

Case 2.

With the use of adapters and the selection service, each application simply needs to publish its selection object and register AdapterFactories. Supported items are then include at runtime and the application doe not have hard dependencies to any particular implementation.

.. code:: java

    ContextMenu menu = new ContextMenu();
    SelectionService.getInstance().setSelection(this, Arrays.asList(AppSelection(appModel)));
    List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
    supported.stream().forEach(action -> {
            MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
            ...
            items.add(menuItem);
        });

Register zero or more
`AdapterFactories <https://github.com/ControlSystemStudio/phoebus/blob/master/app/logbook/ui/src/main/java/org/phoebus/logbook/ui/adapters/AppSelectionAdapterFactory.java>`_
which provide the mechanism to adapt an AppSelection to a simple LogEntry or an Olog LogEntry
