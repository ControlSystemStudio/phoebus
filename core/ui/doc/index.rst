Active Jobs
===========

Opened from the ``Applications``, ``Debug`` menu,
the Active Jobs panel displays ongoing background jobs.

Examples include jobs that load configuration files.
Long-running jobs can be cancelled.


Implementation Detail
---------------------

Canceling a job asks the application to abort an ongoing job
and depends on a proper implementation of the application feature
to honor the request.


PV List
=======

Opened from the ``Applications``, ``Debug`` menu,
the PV List panel lists all active PVs,
their connection state and the reference count.
