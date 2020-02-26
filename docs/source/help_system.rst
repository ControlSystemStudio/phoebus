Help System
===========

Help files are `*.rst` files with reStructuredText markup
as described at http://www.sphinx-doc.org/en/stable/rest.html.

The top-level repository for help files is phoebus-doc
(https://github.com/kasemir/phoebus-doc)
and a snapshot of the current version is accessible on
http://phoebus-doc.readthedocs.io.

The top-level help repository provides the overall structure
and content that describes the Phoebus-based CS-Studio in general.
Each phoebus application can contribute help content that
describes the specific application in more detail.
This is done by adding a ``doc/`` folder with an ``index.rst``
file to the application sources.
When phoebus-doc is built, it includes all ``phoebus/**/doc/index.rst``
in the Applications section of the manual.
While the ``*.rst`` markup is ultimately converted into HTML,
some applications might have already have HTML content generated
by other means, for example from Javadoc.
Any ``doc/html`` folder found in the applications source code is
copied into the file html folder. To appear in the manual,
it needs to be linked from the ``index.rst`` of an application
via ``raw`` tags. For an example, refer to the display builder editor help.

Overall, the help content is thus generated from a combination of

1. Top-level content defined in ``phoebus-doc/source/*.rst``
2. Application specific help copied from the ``phoebus/**/doc/index.rst`` source tree
3. Pre-generated HTML folders copied from the ``phoebus/**/doc/html`` source tree

In addition to building the help files locally as described below,
or viewing a snapshot of the current version online
under the link mentioned above, the help content is also bundled into
the phoebus-based CS-Studio product.
When the phoebus product is built,
it checks for the HTML version of the manual
in ``phoebus-doc/build/html``.
If found, it is bundled into the product.

Complete build steps of manual and product::

    # Obtain sources for documentation and product
    git clone https://github.com/kasemir/phoebus-doc.git
    git clone https://github.com/shroffk/phoebus.git

    # Some application code may contain html content
    # that needs to be generated, for example from Javadoc
    ( cd phoebus/app/display/editor;  ant -f javadoc.xml clean all )

    # Building the manual will locate and include
    # all ../phoebus/applications/**/doc/index.rst
    ( cd phoebus-doc; make clean html )
    # Windows: Use make.bat html

    # Fetch dependencies
    ( cd phoebus/dependencies; mvn clean install )
    
    # Build the product, which bundles help from
    # ../phoebus-doc/build/html
    # as phoebus-product/target/doc
    ( cd phoebus; ant clean dist )
    
    # Could now run the product
    ( cd phoebus/phoebus-product; sh phoebus.sh )

    # or distribute the ZIP file,
    # phoebus/phoebus-product/target/phoebus-0.0.1.zip


Internals
---------

In ``phoebus-doc/source/conf.py``, the ``createAppIndex()`` method
checks for the phoebus sources and builds the application section
of the manual.

When invoking the Phoebus ``Help`` menu,
it looks for a ``doc/`` folder in the installation location (see :ref:`locations`).

As a fallback for development in the IDE, it looks for ``phoebus-doc/build/html``.
