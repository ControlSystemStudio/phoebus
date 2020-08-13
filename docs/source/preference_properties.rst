:orphan:

.. _preference_settings:

Preferences Listing
===================

The following preference settings are available for the various application features.
To use them in your settings file, remember to prefix each setting with the package name.


alarm
-----

File ../../app/alarm/model/src/main/resources/alarm_preferences.properties::

   # --------------------------------------
   # Package org.phoebus.applications.alarm
   # --------------------------------------
   
   # Kafka Server host:port
   server=localhost:9092
   
   # Name of alarm tree root
   config_name=Accelerator
   
   # Names of selectable alarm configurations
   # The `config_name` will be used as the default for newly opened tools,
   # and if `config_names` is empty, it remains the only option.
   # When one or more comma-separated configurations are listed,
   # the UI shows the selected name and allows switching
   # between them.
   config_names=Accelerator, Demo
   
   # Timeout in seconds for initial PV connection
   connection_timeout=30
   
   
   ## Area Panel
   
   # Item level for alarm area view:
   # 1 - Root element
   # 2 - Top-level "area" elements just below root
   # 3 - Show all the items at level 3
   alarm_area_level=2
   
   # Number of columns in the alarm area view
   alarm_area_column_count=3
   
   # Gap between alarm area panel items
   alarm_area_gap=5
   
   # Font size for the alarm area view
   alarm_area_font_size=15
   
   # Limit for the number of context menu items.
   # Separately applied to the number of 'guidance',
   # 'display' and 'command' menu entries.
   alarm_menu_max_items=10
   
   # Alarm table row limit
   # If there are more rows, they're suppressed
   alarm_table_max_rows=2500
   
   # Directory used for executing commands
   # May use Java system properties like this: $(prop_name)
   command_directory=$(user.home)
   
   # The threshold of messages that must accumulate before the annunciator begins to simply state: "There are X Alarm messages."
   annunciator_threshold=3
   
   # The number of messages the annunciator will retain before popping messages off the front of the message queue.
   annunciator_retention_count=100
   
   # Timeout in seconds at which server sends idle state updates
   # for the 'root' element if there's no real traffic.
   # Client will wait 3 times this long and then declare a timeout.
   idle_timeout=10
   
   # Name of the sender, the 'from' field of automated email actions
   automated_email_sender=Alarm Notifier <alarm_server@example.org>
   
   # Comma-separated list of automated actions on which to follow up
   # Options include mailto:, cmd:
   automated_action_followup=mailto:, cmd:
   
   # Optional heartbeat PV
   # When defined, alarm server will set it to 1 every heartbeat_secs
   #heartbeat_pv=Demo:AlarmServerHeartbeat
   heartbeat_pv=
   
   # Heartbeat PV period in seconds
   heartbeat_secs=10
   
   # Period for repeated annunciation
   #
   # If there are active alarms, i.e. alarms that have not been acknowleded,
   # a message "There are 47 active alarms" will be issued
   #
   # Format is HH:MM:SS, for example 00:15:00 to nag every 15 minutes.
   # Set to 0 to disable
   nag_period=00:15:00


alarm.logging.ui
----------------

File ../../app/alarm/logging-ui/src/main/resources/alarm_logging_preferences.properties::

   # -------------------------------------------------
   # Package org.phoebus.applications.alarm.logging.ui
   # -------------------------------------------------
   
   # location of elastic node/s
   es_host=130.199.219.152
   es_port=9200
   es_index=accelerator_alarms
   es_max_size=1000
   es_sniff=false


archive
-------

File ../../services/archive-engine/src/main/resources/archive_preferences.properties::

   # ----------------------------
   # Package org.csstudio.archive
   # ----------------------------
   
   # RDB URL for archived data
   #
   # Oracle example
   # url=jdbc:oracle:thin:user/password@//172.31.73.122:1521/prod
   #
   # PostgreSQL example
   # url=jdbc:postgresql://localhost/archive
   #
   # MySQL example
   url=jdbc:mysql://localhost/archive?rewriteBatchedStatements=true
   
   # RDB user and password
   # Some applications also provide command-line option to override.
   user=archive
   password=$archive
   
   # Schema name. Used with an added "." as prefix for table names.
   # For now this is only used with Oracle URLs and ignored for MySQL
   schema=
   
   # Timeout [seconds] for certain SQL queries
   # Fundamentally, the SQL queries for data take as long as they take
   # and any artificial timeout just breaks queries that would otherwise
   # have returned OK  few seconds after the timeout.
   # We've seen Oracle lockups, though, that caused JDBC to hang forever
   # because the SAMPLE table was locked. No error/exception, just hanging.
   # A timeout is used for operations other than getting the actual data,
   # for example the channel id-by-name query which _should_ return within
   # a shot time, to catch that type of RDB lockup.
   # timeout_secs=120
   # With PostgreSQL, the setQueryTimeout API is not implemented,
   # and calling it results in an exception.
   # Setting the timeout to 0 disables calls to setQueryTimeout.
   timeout_secs=0
   
   # Use a blob to read/write array samples?
   #
   # The original SAMPLE table did not contain an ARRAY_VAL column
   # for the array blob data, but instead used a separate ARRAY_VAL table.
   # When running against an old database, this parameter must be set to false.
   use_array_blob=true
   
   # Name of sample table for writing
   write_sample_table=sample
   
   # Maximum length of text samples written to SAMPLE.STR_VAL
   max_text_sample_length=80
   
   # Use postgres copy instead of insert
   use_postgres_copy=false
   
   # Seconds between log messages for Not-a-Number, futuristic, back-in-time values, buffer overruns
   # 24h = 24*60*60 = 86400
   log_trouble_samples=86400
   log_overrun=86400
   
   # Write period in seconds
   write_period=30
   
   # Maximum number of repeat counts for scanned channels
   max_repeats=60
   
   # Write batch size
   batch_size=500
   
   # Buffer reserve (N times what's ideally needed)
   buffer_reserve=2.0
   
   # Samples with time stamps this far ahead of the local time
   # are ignored
   # 24*60*60 = 86400 = 1 day
   ignored_future=86400


archive.reader.appliance
------------------------

File ../../app/databrowser/src/main/resources/appliance_preferences.properties::

   # ----------------------------------------
   # Package org.phoebus.archive.reader.appliance
   # ----------------------------------------
   
   useStatisticsForOptimizedData=true
   useNewOptimizedOperator=true


archive.reader.rdb
------------------

File ../../app/databrowser/src/main/resources/archive_reader_rdb_preferences.properties::

   ---------------------------------------
   # Package org.phoebus.archive.reader.rdb
   # --------------------------------------
   
   # User and password for reading archived data
   user=archive
   password=$archive
   
   # Table prefix
   # For Oracle, this is typically the schema name,
   # including "."
   prefix=
   
   # Timeout [seconds] for certain SQL queries
   # Fundamentally, the SQL queries for data take as long as they take
   # and any artificial timeout just breaks queries that would otherwise
   # have returned OK a few seconds after the timeout.
   # We've seen Oracle lockups, though, that caused JDBC to hang forever
   # because the SAMPLE table was locked. No error/exception, just hanging.
   # A timeout is used for operations other than getting the actual data,
   # for example the channel id-by-name query which _should_ return within
   # a shot time, to catch that type of RDB lockup.
   timeout_secs=120
   # Setting the timeout to 0 disables calls to setQueryTimeout,
   # which may be required for PostgreSQL where the setQueryTimeout API is not implemented.
   # timeout_secs=0
   
   
   # Use a BLOB to read array samples?
   #
   # The original SAMPLE table did not contain an ARRAY_VAL column
   # for the array blob data, but instead used a separate ARRAY_VAL table.
   # When running against an old database, this parameter must be set to false.
   use_array_blob=true
   
   # Use stored procedures and functions for 'optimized' data readout?
   # Set to procedure name, or nothing to disable stored procedure.
   stored_procedure=
   starttime_function=
   
   # MySQL:
   # stored_procedure=archive.get_browser_data
   
   # PostgreSQL
   # stored_procedure=public.get_browser_data
   
   # Oracle:
   # stored_procedure=chan_arch.archive_reader_pkg.get_browser_data
   # starttime_function=SELECT chan_arch.archive_reader_pkg.get_actual_start_time (?, ?, ?)  FROM DUAL
   
   
   # JDBC Statement 'fetch size':
   # Number of samples to read in one network transfer.
   #
   # For Oracle, the default is 10.
   # Tests resulted in a speed increase up to fetch sizes of 1000.
   # On the other hand, bigger numbers can result in java.lang.OutOfMemoryError.
   fetch_size=1000


console
-------

File ../../app/console/src/main/resources/console_preferences.properties::

   # ----------------------------------------
   # Package org.phoebus.applications.console
   # ----------------------------------------
   
   # Number of output lines to keep.
   # Older output is dropped.
   output_line_limit=100
   
   # Number of lines to keep in input history,
   # accessible via up/down cursor keys
   history_size=20
   
   # Font name and size
   font_name=Liberation Mono
   font_size=14
   
   # Prompt (may include trailing space)
   prompt=>>>\
   
   # Prompt (input field) info
   prompt_info=Enter console command
   
   # 'Shell' to execute.
   #
   # Examples:
   #   /usr/bin/python -i
   #   /usr/bin/python -i /path/to/some/initial_file.py
   #   /bin/bash
   #
   # Value may include properties.
   shell=/usr/bin/python -i
   
   # Folder where the shell process should be started
   #
   # Value may include properties.
   directory=$(user.home)


display.builder.editor
----------------------

File ../../app/display/editor/src/main/resources/display_editor_preferences.properties::

   # ----------------------------------------
   # Package org.csstudio.display.builder.editor
   # ----------------------------------------
   
   # Widget types to hide from the palette
   #
   # Comma separated list of widget types that will not be shown
   # in the palette.
   # Existing displays that use these widgets can still be edited
   # and executed, but widgets do not appear in the palette to
   # discourage adding them to new displays.
   
   # Hiding widgets where representation has not been imported because of dependencies
   hidden_widget_types=linear-meter,knob,gauge,clock,digital_clock
   #
   #
   # GUI Menu action Applications / Display / New Display opens the following template
   new_display_template=examples:/initial.bob


display.builder.model
---------------------

File ../../app/display/model/src/main/resources/display_model_preferences.properties::

   # ----------------------------------------
   # Package org.csstudio.display.builder.model
   # ----------------------------------------
   
   
   # Widget classes
   # One or more *.bcf files, separated by ';'
   # Defaults to built-in copy of examples/classes.bcf
   class_files=examples:classes.bcf
   
   # Named colors
   # One or more *.def files, separated by ';'
   # Defaults to built-in copy of examples/color.def
   color_files=examples:color.def
   
   # Named fonts
   # One or more *.def files, separated by ';'
   # Defaults to built-in copy of examples/font.def
   font_files=examples:font.def
   
   # Global macros, used for all displays.
   #
   # Displays start with these macros,
   # and can then add new macros or overwrite
   # the values of these macros.
   #
   # Format:
   # Entries where the XML tag name is the macro name,
   # and the XML content is the macro value.
   # The macro name must be a valid XML tag name:
   # * Must start with character
   # * May then contain characters or numbers
   # * May also contain underscores
   #
   macros=<EXAMPLE_MACRO>Value from Preferences</EXAMPLE_MACRO><TEST>true</TEST>
   
   
   # Timeout [ms] for loading files: Displays, but also color, font, widget class files
   read_timeout=10000
   
   # Timeout [sec] for caching files loaded from a URL
   cache_timeout=60
   
   
   # 'BOY' *.opi files provide the font size in 'points'.
   # All other positions and sizes are in 'pixels'.
   # A point is meant to represent 1/72th of an inch.
   # The actual on-screen size display settings.
   # Plugging a different monitor into the computer can
   # potentially change the DPI settings of the graphics driver,
   # resulting in different font sizes.
   # The display builder uses fonts in pixels to avoid such changes.
   #
   # When reading legacy display files, we do not know the DPI
   # scaling that was used to create the display.
   # This factor is used to translate legacy font sizes
   # from 'points' into 'pixel':
   #
   # legacy_points = pixel * legacy_font_calibration
   #
   # The test program
   #   org.csstudio.display.builder.representation.swt.SWTFontCalibation
   # can be used to obtain the factor when executed on the original
   # platform where the legacy display files were created.
   #
   # When loading legacy files,
   # _increasing_ the legacy_font_calibration will
   # result in _smaller_ fonts in the display builder
   legacy_font_calibration=1.01
   
   # Maximum re-parse operations
   #
   # When reading legacy *.opi files and for example
   # finding a "TextUpdate" widget that has no <pv_name>,
   # it will be changed into a "Label" widget and then re-parsed.
   # If more than a certain number of re-parse operations are triggered
   # within one 'level' of the file (number of widgets at the root of the display,
   # or number of childred for a "Group" widget),
   # the parser assumes that it entered an infinite re-parse loop
   # and aborts.
   max_reparse_iterations=5000
   
   # When writing a display file, skip properties that are still at default values?
   skip_defaults=true


display.builder.representation
------------------------------

File ../../app/display/representation/src/main/resources/display_representation_preferences.properties::

   # ---------------------------------------------------
   # Package org.csstudio.display.builder.representation
   # ---------------------------------------------------
   
   ## Representation Tuning
   #
   # The representation 'throttles' updates to widgets.
   # When a widget requests an update, a little accumulation time
   # allows more updates to accumulate before actually performing
   # the queued update requests on the UI thread.
   #
   # An update delay then suppresses further updates to prevent
   # flooding the UI thread.
   #
   # Update runs that last longer than a threshold can be logged
   
   # Time waited after a trigger to allow for more updates to accumulate
   update_accumulation_time = 20
   
   # Pause between updates to prevent flooding the UI thread
   update_delay = 100
   
   # Period in seconds for logging update performance
   performance_log_period_secs = 5
   
   # UI thread durations above this threshold are logged
   performance_log_threshold_ms = 20
   
   # Pause between updates of plots (XY, lines)
   # Limit to 250ms=4 Hz
   plot_update_delay = 250
   
   # Pause between updates of image plots
   # Limit to 250ms=4 Hz
   image_update_delay = 250
   
   # Length limit for tool tips
   # Tool tips that are too long can be a problem
   # on some window systems.
   tooltip_length=150


display.builder.representation.javafx
-------------------------------------

File ../../app/display/representation-javafx/src/main/resources/jfx_repr_preferences.properties::

   # ----------------------------------------------------------
   # Package org.csstudio.display.builder.representation.javafx
   # ----------------------------------------------------------
   
   # When clicking on the 'slider' widget 'track',
   # should the value increment/decrement,
   # matching the behavior of EDM, BOY, ...?
   # Otherwise, jump to the clicked value right away.
   inc_dec_slider=true


display.builder.runtime
-----------------------

File ../../app/display/runtime/src/main/resources/display_runtime_preferences.properties::

   # --------------------------------------------
   # Package org.csstudio.display.builder.runtime
   # --------------------------------------------
   
   # Search path for Jython scripts used by the display runtime.
   # Note that format depends on the OS.
   # On UNIX systems, path entries are separated by ':', on Windows by ';'.
   # python_path=/home/controls/displays/scripts:/home/fred/my_scripts
   python_path=
   
   # PV Name Patches
   #
   # Translate PV names based on regular expression pattern and replacement
   #
   # Format:  pattern@replacement@pattern@replacement
   #
   # Setting must contain a sequence of pattern & replacement pairs,
   # all separated by '@'.
   #
   # The regular expression for the pattern can includes "( )" groups,
   # which are then used in the replacement via "$1", "$2", ..
   #
   # If the item separator character '@' itself is required within the pattern or replacement,
   # use '[@]' to distinguish it from the item separator, i.e.
   #
   #    [@]work@[@]home
   #
   # will patch "be@work" -> "be@home"
   #
   # Patches are applied in the order they're listed in the preference, i.e.
   # later patches are applied to names already patched by earlier ones.
   #
   # Example:
   # Remove PVManager's longString modifier,             'some_pv {"longString":true}' -> 'some_pv'
   # turn constant formula into constant local variable, '=42'                         -> 'loc://const42(42)'
   # as well as constant name into constant local var,   '="Fred"'                     -> 'loc://strFred("Fred")'
   pv_name_patches=\\{"longString":true\\}"@@^="([a-zA-Z]+)"@loc://str$1("$1")
   
   # PV update throttle in millisecs
   # 250ms = 4 Hz
   update_throttle=250
   
   # "Probe Display"
   # Added to context menu for ProcessVariables,
   # invoked with macro PV set to the PV name.
   # When left empty, the "Probe Display"
   # context menu entry is disabled.
   probe_display=examples:/probe.bob


display.converter.edm
---------------------

File ../../app/display/convert-edm/src/main/resources/edm_converter_preferences.properties::

   # ------------------------------------------
   # Package org.csstudio.display.converter.edm
   # ------------------------------------------
   
   # Path to the directory where the auto-converter will
   # generate auto-converted files.
   # May include system properties like $(user.home).
   # Target directory must be in the file system.
   # The folder is created if it doesn't exist.
   #
   # When left empty, the auto-converter is disabled.
   auto_converter_dir=
   
   # EDM colors.list file
   # Must be defined to use converter.
   # May be a file system path or http:/.. link
   colors_list=
   
   # Font mappings
   #
   # Format: EDMFontPattern=DisplayBuilderFont,Pattern=Font,...
   # EDMFontPattern is regular expression for the name used by EDM
   #
   # Patterns are checked in the order in which they're listed in here,
   # so a catch-all ".*" pattern should be at the end
   font_mappings=helvetica=Liberation Sans,courier=Liberation Mono,times=Liberation Serif,.*=Liberation Sans
   
   # Path to text file that lists EDM search paths.
   # May be a file system path or http:/.. link.
   #
   # In the file, each line in the text file contains a path,
   # which may be a file system path or a http:// link.
   # When trying to open an *.edl file,
   # converter will try each path in the order
   # listed in the file.
   # Lines starting with "#" are ignored.
   #
   # When the edm_paths_config is left empty,
   # the converter won't find files.
   edm_paths_config=


email
-----

File ../../core/email/src/main/resources/email_preferences.properties::

   # -------------------------
   # Package org.phoebus.email
   # -------------------------
   
   # smtp host
   # When set to "DISABLE", email support is disabled
   mailhost=smtp.bnl.gov
   
   # smtp port
   mailport=25
   
   # User and password for connecting to the mail host, usually left empty
   username=
   password=


errlog
------

File ../../app/errlog/src/main/resources/errlog_preferences.properties::

   # ---------------------------------------
   # Package org.phoebus.applications.errlog
   # ---------------------------------------
   
   # Number of lines to keep in error log
   max_lines = 500


filebrowser
-----------

File ../../app/filebrowser/src/main/resources/filebrowser_preferences.properties::

   # --------------------------------------------
   # Package org.phoebus.applications.filebrowser
   # --------------------------------------------
   
   # Initial root directory for newly opened file browser
   # May use system properties like "$(user.home)".
   # At runtime, user can select a different base directory,
   # but pressing the "Home" button reverts to this one.
   default_root=$(user.home)
   
   # Show hidden files (File.isHidden)?
   show_hidden=false


framework.autocomplete
----------------------

File ../../core/framework/src/main/resources/autocomplete_preferences.properties::

   # ------------------------------------------
   # Package org.phoebus.framework.autocomplete
   # ------------------------------------------
   
   # Enable the built-in PV proposal providers?
   enable_loc_pv_proposals=true
   enable_sim_pv_proposals=true
   enable_sys_pv_proposals=true
   enable_pva_pv_proposals=true
   enable_mqtt_pv_proposals=false
   
   # Site-specific proposal providers can be added via PVProposalProvider SPI,
   # and disabled by removing the contribution.


framework.workbench
-------------------

File ../../core/framework/src/main/resources/workbench_preferences.properties::

   # ---------------------------------------
   # Package org.phoebus.framework.workbench
   # ---------------------------------------
   
   # External applications
   #
   # Defines applications to use for specific file extensions
   #
   # Format:
   #
   # Each definition consists of name, file extensions, command.
   #
   # Name is the name of the definition, used to register the application.
   # File extensions is a '|'-separated list of file extensions (not including the 'dot').
   # Command is the path to the command.
   # The command will be invoked with the full path to the resource as an argument.
   #
   # Each definition must use a key that starts with "external_app_"
   
   # Examples:
   #
   # Start 'gedit' for text files
   # external_app_text=Text Editor,txt|dat|py|ini|db|xml|xsl|css|cmd|sh|st|log|out|md|shp,gedit
   #
   # Start 'eog' for images, 'firefox' for PDF files
   # external_app_image=Image Viewer,png|jpg|gif|jpeg,eog
   #
   # Start 'firefox' to view PDFs
   # external_app_pdf=PDF Viewer,pdf,firefox
   #
   # Example for some site-specific tool that opens 'alog' files
   # external_app_alog=Alignment Log,alog,/path/to/alog_viewer
   
   # Directory where external applications are started
   # May use system properties
   external_apps_directory=$(user.home)


javafx.rtplot
-------------

File ../../app/rtplot/src/main/resources/rt_plot_preferences.properties::

   # ----------------------------------
   # Package org.csstudio.javafx.rtplot
   # ----------------------------------
   
   # Coloring used to shade plot region beyond 'now'
   # in time-based plots. RGBA (all values 0..255)
   # Painted on on top of grid, before traces are drawn.
   #
   # Half-transparent, average of black & white,
   # works for both white and black backgrounds
   shady_future=128, 128, 128, 128
   
   # If you prefer a rose-colored future
   # shady_future=255, 128, 128, 25
   
   # If you prefer to not highlight the plot region beyond 'now'
   # shady_future=128, 128, 128, 0


logbook.ui
----------

File ../../app/logbook/ui/src/main/resources/log_ui_preferences.properties::

   # ------------------------------
   # Package org.phoebus.logbook.ui
   # ------------------------------
   
   # Site specific log book implementation name.
   # When empty, logbook submissions are disabled
   logbook_factory=inmemory
   
   # Comma-separated list of default logbooks for new log entries.
   default_logbooks=Scratch Pad
   
   # Whether or not to save user credentials to file so they only have to be entered once when making log entries.
   save_credentials=false


pv
--

File ../../core/pv/src/main/resources/pv_preferences.properties::

   # ----------------------
   # Package org.phoebus.pv
   # ----------------------
   
   # Default PV Type
   default=ca
   


pv.ca
-----

File ../../core/pv/src/main/resources/pv_ca_preferences.properties::

   # -------------------------
   # Package org.phoebus.pv.ca
   # -------------------------
   
   # Channel Access address list
   addr_list=
   
   auto_addr_list=true
   
   max_array_bytes=100000000
   
   server_port=5064
   
   repeater_port=5065
   
   beacon_period=15
   
   connection_timeout=30
   
   # Support variable length arrays?
   # auto, true, false
   variable_length_array=auto
   
   # Connect at lower priority for arrays
   # with more elements than this threshold
   large_array_threshold= 100000
   
   # Is the DBE_PROPERTY subscription supported
   # to monitor for changes in units, limits etc?
   dbe_property_supported=false
   
   # Mask to use for subscriptions
   # VALUE, ALARM, ARCHIVE
   monitor_mask=VALUE
   
   # Name server list
   name_servers=


pv.formula
----------

File ../../core/pv/src/main/resources/pv_formula_preferences.properties::

   # ------------------------------
   # Package org.phoebus.pv.formula
   # ------------------------------
   
   # Update throttle for input PVs
   throttle_ms=500


pv.mqtt
-------

File ../../core/pv/src/main/resources/pv_mqtt_preferences.properties::

   # ---------------------------
   # Package org.phoebus.pv.mqtt
   # ---------------------------
   
   # MQTT Broker
   # All "mqtt://some/tag" PVs will use this broker
   mqtt_broker=tcp://localhost:1883


pvtable
-------

File ../../app/pvtable/src/main/resources/pv_table_preferences.properties::

   # ----------------------------------------
   # Package org.phoebus.applications.pvtable
   # ----------------------------------------
   
   # Should all BYTE[] values be considered "long strings"
   treat_byte_array_as_string=true
   
   # Show the units when displaying values?
   show_units=true
   
   # Show a "Description" column that reads xxx.DESC?
   show_description=true
   
   # Default tolerance for newly added items
   tolerance=0.1
   
   # Maximum update period for PVs in millisecs
   max_update_period=500


pvtree
------

File ../../app/pvtree/src/main/resources/pv_tree_preferences.properties::

   # ---------------------------------------
   # Package org.phoebus.applications.pvtree
   # ---------------------------------------
   
   # The channel access DBR_STRING has a length limit of 40 chars.
   # Since EPICS base R3.14.11, reading fields with an added '$' returns
   # their value as a char[] without length limitation.
   # For older IOCs, this will however fail, so set this option
   # only if all IOCs are at least version R3.14.11
   read_long_fields=true
   
   # For each record type, list the fields to read and trace as 'links'.
   #  Format: record_type (field1, field2) ; record_type (...)
   #
   # Fields can simply be listed as 'INP', 'DOL'.
   # The syntax INPA-L is a shortcut for INPA, INPB, INPC, ..., INPL
   # The syntax INP001-128 is a shortcut for INP001, INP002, ..., INP128
   # The general syntax is "FIELDxxx-yyy",
   # where "xxx" and "yyy" are the initial and final value.
   # "xxx" and "yyy" need to be of the same length, i.e. "1-9" or "01-42", NOT "1-42".
   # For characters, only single-char "A-Z" is supported, NOT "AA-ZZ",
   # where it's also unclear if that should turn into AA, AB, AC, .., AZ, BA, BB, BC, .., ZZ
   # or AA, BB, .., ZZ
   #
   # bigASub is a CSIRO/ASCAP record type, doesn't hurt to add that to the shared configuration
   #
   # scalcout is a bit unfortunate since there is no shortcut for INAA-INLL.
   #
   # alarm record has INP1-10. 1-9 handled by pattern, INP10 listed
   
   fields=aai(INP);ai(INP);bi(INP);compress(INP);longin(INP);mbbi(INP);mbbiDirect(INP);mbboDirect(INP);stringin(INP);lsi(INP);subArray(INP);waveform(INP);aao(DOL);ao(DOL);bo(DOL);fanout(DOL);longout(DOL);mbbo(DOL);stringout(DOL);sub(INPA-L);genSub(INPA-L);calc(INPA-L);calcout(INPA-L);aSub(INPA-U);seq(SELN);bigASub(INP001-128);scalcout(INPA-L,INAA,INBB,INCC,INDD,INEE,INFF,INGG,INHH,INII,INJJ,INKK,INLL);alarm(INP1-9,INP10)
   
   
   # Max update period in seconds
   update_period=0.5


saveandrestore
--------------

File ../../app/save-and-restore/ui/src/main/resources/save_and_restore_preferences.properties::

   # -----------------------------------------------
   # Package org.phoebus.applications.saveandrestore
   # -----------------------------------------------
   
   # The URL to the save-and-restore service
   jmasar.service.url=http://localhost:8080
   
   # Read timeout (in ms) used by the Jersey client
   httpClient.readTimeout=1000
   
   # Connect timeout in (ms) used by the Jersey client
   httpClient.connectTimeout=1000


scan.client
-----------

File ../../app/scan/client/src/main/resources/scan_client_preferences.properties::

   # ----------------------------------------
   # Package org.csstudio.scan.client
   # ----------------------------------------
   
   # Name of host where scan server is running
   host=localhost
   
   # TCP port of scan server REST interface
   port=4810
   
   # Poll period [millisecs] of the scan client (scan monitor, plot, ...)
   poll_period=1000


scan.ui
-------

File ../../app/scan/ui/src/main/resources/scan_ui_preferences.properties::

   # ----------------------------
   # Package org.csstudio.scan.ui
   # ----------------------------
   
   # Show scan monitor status bar?
   monitor_status=false


security
--------

File ../../core/security/src/main/resources/phoebus_security_preferences.properties::

   # ----------------------------
   # Package org.phoebus.security
   # ----------------------------
   
   # Authorization file
   #
   # If left empty, the built-in core/security/authorization.conf is used.
   #
   # When specifying a plain file name like "authorization.conf",
   # the install location (Locations.install()) is searched for that file name.
   #
   # The file name can also be an absolute path like /some/path/auth.conf.
   #
   # Finally, the file name may use a system property like $(auth_file)
   # which in turn could be set to either BUILTIN, a file in the install location,
   # or an absolute path.
   #
   # When set to an invalid file, the user will have no authorizations at all.
   
   # Use built-in core/security/authorization.conf
   authorization_file=
   
   # Use authorization.conf in the install location
   #authorization_file=authorization.conf
   


trends.databrowser3
-------------------

File ../../app/databrowser/src/main/resources/databrowser_preferences.properties::

   # ----------------------------------------
   # Package org.csstudio.trends.databrowser3
   # ----------------------------------------
   
   # Default auto scale value
   # Possible values are: true to enable the automatic calculation of the min/max Y-axis, or false to use min/max fixed values.
   use_auto_scale=false
   
   # Default time span displayed in plot in seconds
   time_span=3600
   
   # Default scan period in seconds. 0 for 'monitor'
   scan_period=0.0
   
   # Default plot update period in seconds
   update_period=3.0
   
   # .. elements in live sample buffer
   live_buffer_size=5000
   
   # Default line width
   line_width=2
   
   # Opacity of 'area'
   #   0%: Area totally transparent (invisible)
   #  20%: Area quite transparent
   # 100%: Area uses  solid color
   opacity=40
   
   # Default trace type for newly created traces.
   # Allowed values are defined by org.csstudio.trends.databrowser3.model.TraceType:
   # AREA, ERROR_BARS, SINGLE_LINE, AREA_DIRECT, SINGLE_LINE_DIRECT, SQUARES, ...
   trace_type=AREA
   
   # Delay in milliseconds that delays archive requests when
   # the user moves the time axis to avoid a flurry of archive requests
   # while interactively zooming and panning
   archive_fetch_delay=500
   
   # Number of concurrent archive fetch requests.
   # When more requests are necessary, the background jobs
   # will wait until the previously submitted jobs complete,
   # to limit the number of concurrent requests.
   #
   # Ideally, the number can be high, but to limit the number
   # of concurrent requests to for example an RDB,
   # this value can be lowered.
   #
   # Note that this does not apply to 'exporting' data
   # in spreadsheet form, where data for N channels is still
   # collected by reading from N concurrent archive readers.
   concurrent_requests=1000
   
   # Number of binned samples to request for optimized archive access.
   # Negative values scale the display width,
   # i.e. -3 means: 3 times Display pixel width.
   plot_bins=-3
   
   # Suggested data servers
   # Format:  <url>*<url>|<name>
   # List of URLs, separated by '*'.
   # Each URL may be followed by an "|alias"
   #
   # RDB URLs
   # jdbc:mysql://localhost/archive
   #
   # Archive Appliance
   # pbraw\://arcapp01.site.org:17668/retrieval
   #
   # Channel Archiver Network Data Server
   # xnds://localhost/archive/cgi/ArchiveDataServer.cgi
   #
   # Channel Archiver index file (binary) or index.xml (list of indices)
   # cadf:/path/to/index
   # cadf:/path/to/index.xml
   urls=jdbc:mysql://localhost/archive|RDB*xnds://localhost/archive/cgi/ArchiveDataServer.cgi
   
   # Default data sources for newly added channels
   # Format: Same as 'urls'
   archives=jdbc:mysql://localhost/archive|RDB*xnds://localhost/archive/cgi/ArchiveDataServer.cgi
   
   # When opening existing data browser plot,
   # use archive data sources specified in the configuration file (original default)
   # or ignore saved data sources and instead use the preference settings?
   use_default_archives=false
   
   # If there is an error in retrieving archived data,
   # including that the channel is not found in the archive,
   # should this be displayed in a dialog box,
   # or logged as a WARNING (and thus visible on the console)?
   prompt_for_errors=false
   
   # Re-scale behavior when archived data arrives: NONE, STAGGER
   archive_rescale=STAGGER
   
   # Shortcuts offered in the Time Axis configuration
   # Format:
   # Text for shortcut,start_spec|Another shortcut,start_spec
   time_span_shortcuts=30 Minutes,-30 min|1 Hour,-1 hour|12 Hours,-12 hour|1 Day,-1 days|7 Days,-7 days
   
   #It is a path to the directory where the PLT files for WebDataBrowser are placed.
   plt_repository=/opt/codac/opi/databrowser/
   
   #SendEmailAction default sender
   # By defining a default email, users who select "Email.." from the context menu
   # do not need to enter an email address.
   # If left empty, elog dialog will require users to enter a "From:" address for the sender.
   email_default_sender=
   
   # Automatically refresh history data when the liver buffer is full
   # This will prevent the horizontal lines in the shown data when the buffer
   # is too small to cover the selected time range
   automatic_history_refresh=false
   
   # Scroll step, i.e. size of the 'jump' left when scrolling, in seconds.
   # (was called 'future_buffer')
   scroll_step = 5
   
   # Display the trace names on the Value Axis
   # the default value is "true". "false" to not show the trace names on the Axis
   use_trace_names = true
   
   # Prompt / warn when trying to request raw data?
   prompt_for_raw_data_request = true
   
   # Prompt / warn when making trace invisible?
   prompt_for_visibility = true
   
   # Shortcuts offered in the Time Axis configuration
   # Format:
   # Text for shortcut,start_spec|Another shortcut,start_spec
   time_span_shortcuts=30 Minutes,-30 min|1 Hour,-1 hour|12 Hours,-12 hour|1 Day,-1 days|7 Days,-7 days


ui
--

File ../../core/ui/src/main/resources/phoebus_ui_preferences.properties::

   # ----------------------
   # Package org.phoebus.ui
   # ----------------------
   
   # Show the splash screen?
   # Can also be set via '-splash' resp. '-nosplash' command line options
   splash=true
   
   # 'Welcome' URL
   #
   # When left empty, the built-in welcome.html resource is used.
   # Site-specific products can set this to their desired URL,
   # which may include Java system properties to bundle content
   # with the product, for example
   #  file:$(phoebus.install)/welcome_to_hawkins_labs.html
   welcome=
   
   # Default applications
   #
   # When there are multiple applications that handle
   # a resource, the setting determines the one used by default.
   #
   # Format is comma-separated list with sub-text of default application names.
   # For example, "run, exe" would pick "display_runtime" over "display_editor",
   # and "foo_executor" over "foo_creator".
   # The patterns "edit, creat" would inversely open the editor-type apps.
   #
   # This makes the display_runtime and the 3d_viewer default apps,
   # using display_editor and a potentially configured text editor for *.shp files secondary
   default_apps=run,3d,convert_edm
   
   # Hide SPI-provided menu entries
   # Comma-separated list of class names
   hide_spi_menu=org.phoebus.ui.monitoring.FreezeUI
   
   # Top resources to show in "File" menu and toolbar
   #
   # Format:
   # uri1 | uri2,Display name 2 | uri3,Display name 3
   top_resources=examples:/01_main.bob?app=display_runtime,Example Display | pv://?sim://sine&app=probe,Probe Example | pv://?sim://sine&loc://x(10)&app=pv_table,PV Table Example | http://www.google.com?app=web, Google
   
   # Home display file. "Home display" button will navigate to this display.
   home_display=examples:/01_main.bob?app=display_runtime,Example Display
   
   # How many array elements to show when formatting as text?
   max_array_formatting=256
   
   # UI Responsiveness Monitor Period
   # Period between tests [millisec],
   # i.e. the minimum detected UI freeze duration
   # Set to 0 to disable
   ui_monitor_period=500
   
   # Show user ID in status bar?
   status_show_user=true
   


update
------

File ../../app/update/src/main/resources/update_preferences.properties::

   # ----------------------------------------
   # Package org.phoebus.applications.update
   # ----------------------------------------
   
   # Time to wait [seconds] for update check
   # to allow more important tools to start
   delay=10
   
   # Version time/date
   #
   # If the distribution found at the `update_url`
   # is later than this date, an update will be performed.
   #
   # The updated distribution must contain a new value for
   # the org.phoebus.applications.update/current_version setting.
   #
   # By for example publishing updates with a 'current_version'
   # that's one month ahead, you can suppress minor updates
   # for a month.
   #
   # Format: YYYY-MM-DD HH:MM
   #current_version=2018-06-18 13:10
   current_version=
   
   
   # Location where updates can be found
   #
   # The file:, http: or https: URL is checked.
   # If it exists, and its modification time is after `current_version`,
   # the updated distribution is downloaded
   # and the current Locations.install() is replaced.
   #
   # Location may include system properties
   # and $(arch) will be replaced by "linux", "mac" or "win"
   # to allow locations specific to each architecture.
   #
   # Empty: Do not perform any update check
   update_url=
   # update_url=https://controlssoftware.sns.ornl.gov/css_phoebus/nightly/product-sns-$(arch).zip
   
   
   # List of regular expressions, comma-separated, which will be
   # removed from the ZIP file entry.
   # If result is empty string, the entry is skipped.
   #
   # The update ZIP file can have various formats.
   #
   # Basic ZIP file:
   #    phoebus-{site, version}/*
   #
   # => Remove 'phoebus-.*' from entry name
   #    to install _content_ of zip into install_location
   #    without creating yet another subdir
   #
   # ZIP that's packaged for Windows, including JDK:
   #    product-sns-0.0.1/*
   #    jdk/*
   #
   # => Remove 'product-sns-*' from entry name,
   #    skip 'jdk'.
   #
   # ZIP that's packaged for Mac: Either
   #    phoebus.app/product-sns-0.0.1/*  => Remove .../
   #    phoebus.app/jdk/*                => Skip
   #    phoebus.app/Contents/*           => Skip
   # or:
   #    CSS_Phoebus.app/product-sns-0.0.1/*  => Remove .../
   #    CSS_Phoebus.app/jdk/*                => Skip
   #    CSS_Phoebus.app/Contents/*           => Skip
   #
   # Example:
   # phoebus\.app/  - Strip Mac "phoebus.app/" from entries
   #                  so they look more like the Windows example
   #
   # phoebus-[^/]+/ - Strip phoebus product name from ZIP entry
   #
   # jdk/.*         - Remove complete jdk entry to skip it
   removals=CSS_Phoebus\\.app/Contents/.*,CSS_Phoebus\\.app/,phoebus\\.app/Contents/.*,phoebus\\.app/,phoebus-[^/]+/,product-[^/]+/,jdk/.*


viewer3d
--------

File ../../app/3d-viewer/src/main/resources/3d_viewer_preferences.properties::

   # --------------------------------
   # Package org.phoebus.app.viewer3d
   # --------------------------------
   
   # Time out for reading from a URI
   read_timeout=10000
   
   # Default directory for the file chooser.
   default_dir=$(user.home)
   
   # Cone is approximated with these many faces.
   # 3: Triangular base, most minimalistic
   # 8: Looks pretty good
   # Higher: Approaches circular base,
   # but adds CPU & memory usage
   # and doesn't really look much better
   cone_faces=8


