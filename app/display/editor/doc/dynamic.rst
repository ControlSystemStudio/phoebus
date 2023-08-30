=======
Dynamic
=======

There are instances when setting static values for widgets properties is not enough. There are some use cases where one
would like to change the visibility, color, and some other attributed of a widget dynamically at runtime.

The Display builder provides three mechanism to process data from one or more datasources at runtime and dynamically
configure some aspects of a widget.

Care should be taken when using these mechanism, incorrect usage of these can have an adverse impact of the performance
and behaviour of Phoebus.

.. toctree::
   :maxdepth: 1

   formula_functions
   rules
   scripts