Display Navigation
==================

A visual representation of all the navigation graph of an OPI screen.

The application searches all the controls of an OPI screens for actions which launch other OPI screens. The information
is then formatted into a

1. A unique list of all the OPI screens which are linked to the current OPI screen.
2. A tree view showing the linked files while in a manner which preserves the


Opening the Display Navigation View
-----------------------------------

The context menu for .opi and .bob files will have an action Open with *Display Navigation View*

.. image:: /images/navigation-context-action.png

List Link
---------

The list link view shows a unique of all the OPI files linked to the selected OPI. Loops and multiple
links are ignored to create a flat list of only unique OPI screens.

.. image:: /images/list-link.png

The *copy* context menu action can be used to copy the complete path of all linked files.

Link Tree
---------

A tree view of all linked files to the current OPI screen. The tree view does not break loops and cyclic links.

.. image:: /images/link-tree.png