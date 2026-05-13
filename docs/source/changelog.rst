Change Log
==========

A list of API changes, major features, and bug fixes included in each release.

The list is compiled by phoebus developers to document the most important changes associated with each release. For minor bug fixes and patches please refer to github issues and PRs.

Release 4.6.5 (current development version)
-------------------------------------------
Date: TBD

* Avoid deadlock in script compilation and execution.
* Logbook modules restructured.
* Optimized display builder file save implementation.
* Display builder and scan editor undo stack maintained after file save.
* Logbook search cancellable.
* Fix for channel finder property editor.
* Scan infor parser performance improvement.
* PV race condition fix.
* Highlight overdrawn area of embedded display in edit mode.
* RTPlot / Tank widget: new ``parallel_rendering`` preference
  (``org.csstudio.javafx.rtplot/parallel_rendering``).
  When ``true``, each RTTank instance renders on the shared thread-pool rather
  than on the single global ``UpdateThrottle.TIMER`` thread, eliminating the
  serialisation bottleneck that caused visible lag with many simultaneous Tank
  or scale-mode Progress Bar widgets.  Defaults to ``false``; requires restart.
* Display Builder: new ``progressbar_scale_mode`` preference
  (``org.csstudio.display.builder.representation/progressbar_scale_mode``).
  When ``true``, the Progress Bar widget is rendered by the RTTank engine,
  adding a numeric scale, tick marks, format/precision control, an optional
  second (opposite) scale, and alarm-limit lines (``level_lolo`` /
  ``level_low`` / ``level_high`` / ``level_hihi``).  When ``false`` (default),
  the standard JFX ``ProgressBar`` look-and-feel is preserved.
  Requires restart.
* Display Builder (internal): extracted ``RTScaledWidgetRepresentation``
  abstract base class, shared by ``TankRepresentation`` and the new
  ``RTProgressBarRepresentation``, to eliminate duplicated range/orientation
  update logic.


Release 4.6.4 
-------------------------------------------
Date: Nov 16, 2020

**NOTE:** Bug in archiver settings handling causing the client to use default URL instead of the configured one.

* Alarm Datasource
* Simplification of the APIs for the `ContextMenuService`, `SelectionService`, and `AdapterService`.
* File browser context menu items to create new display or data browser plot.
* Array operations in formula functions.
* Display Builder:
    * The symbol widget shows a semi-transparent rectangle with the color "INVALID" for disconnected PVs.
    * The LED widget shows the label when the PV is disconnected and both the "On" and "Off" labels are the same.
* Statistics tab in Databrowser.
* PV status column (value, invalid, disconnected etc) in PV List.
* Number of undo/redo actions in display editor configurable (org.csstudio.display.builder.editor/undo_stack_size)
* Save & Restore enhancements
    * Set point may be edited prior to restore.
    * Display filter in snapshotData view.
    * Integration with Channel Finder.
    * Support for additional data types.
    * Extended server API to enable use of "file paths".

Release 4.6.3
-------------
Date: May 25, 2020

* Save & Restore application

Release 4.6.2
--------------
Date: Apr 27, 2020


Release 4.6.1
-------------
Date: Feb 4, 2020


Release 4.6.0
-------------
Date: Nov 28, 2019

* First release of phoebus framework
