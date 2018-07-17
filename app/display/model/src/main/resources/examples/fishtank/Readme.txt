Example for Water Tank Heater as used in EPICS Database introductions.

Requires an EPICS base installation with a 'softIoc'
command to execute the EPICS database files as per st.cmd.

The display files need a USER macro.
On Linux systems, $USER is an environment variable
and the display builder falls back to env. vars.
On other systems, define $(USER) either in a screen that calls the heater.bob file,
or set as a dislpay builder preference.
Edit/Preferences/CSS Applications/Display/Display Builder
