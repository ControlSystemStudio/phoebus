Performance Monitor
===================

Invoking the menu Applications, Debug, Performance monitor
adds a button to the status bar that displays performance information.
Invoking it again will remove it.


Memory
------
Displays how much memory is allocated, and how much of that is available.
For example, "Avail: 28% of 0.21GB".

Pressing the button triggers a garbage collection.


Frame Rate
----------
Measures the JavaFX frame rate, nominally 60Hz.

System properties that can influence the result:

`-Dquantum.multithreaded=true`
`-Djavafx.animation.fullspeed=true`
`-Djavafx.animation.framerate=120`
`-Djavafx.animation.pulse=120`
