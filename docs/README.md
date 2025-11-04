Phoebus Documentation
=====================

Documentation for https://github.com/ControlSystemStudio/phoebus

View latest snapshot at https://control-system-studio.readthedocs.io

You can build a local copy using Pixi or a local installation of sphinx.

## Option 1: Using Pixi (Recommended)

Install [Pixi](https://pixi.sh) and run:

```bash
# Build the documentation
pixi run build

# Or directly build HTML
pixi run html

# Serve documentation locally
pixi run serve

# Clean build artifacts
pixi run clean
```

## Option 2: Using Sphinx directly

You need to install sphinx:
```bash
# Standard way
pip install Sphinx

# Some RedHat setups
sudo yum install python-sphinx
```

Then build the web version:
```bash
make clean html
```

The above creates a document tree starting with `build/html/index.html`.

As detailed below, the documentation includes components from the phoebus source code tree,
which in turn may include generated HTML content.
The complete documentation build process therefore is:
```
# Obtain sources for Documentation and Product
git clone https://github.com/ControlSystemStudio/phoebus.git

# Build the Javadoc, i.e. html files to be included in the manual
( cd phoebus/app/display/editor; ant -f javadoc.xml clean all )

# Building the documentation
( cd phoebus/doc; make clean html )
```

When then building the Phoebus UI product, it will include the document tree
as online help accessible via the `Help` menu.


Documentation Components
------------------------

1) Top-Level Documentation

   The files in this repository, i.e. the files in `phoebus/doc/source`, starting with `index.rst`,
   form the basis of the documentation tree, meant to provide the top-level documentation.
   
   For details on the `*.rst` file format, refer to the
   ReStructured Text reference, see http://www.sphinx-doc.org/en/stable/rest.html

2) Phoebus Module Documentation

   The `../phoebus/core`, `../phoebus/app` and `../phoebus/services`
   folders are checked for `doc/index.rst` files to include.
   This allows Phoebus source code to contribute to the documentation.
   For example, application modules will be added
   to the "Applications" section of the top-level documentation.
   
   Additionally, the folders are also checked for `doc/images/` folders.
   Resources used by the `index.rst` files should be places in this folder to 
   ensure they are available to sphinx to generate the documentation.

3) Preference Descriptions

   The content of all `../phoebus/**preferences.properties` files
   is added to a "Preferences Listing" appendix of the documentation,
   with a generated listing of preference packages.
   
   The preference file should start with a `# Package ...` header
   to allow listing it in the table of contents.
   
   Example:
   
   ```
   # --------------------------------
   # Package the.phoebus.package.name
   # --------------------------------
   
   # Description of some setting
   the_setting = default_value
   ```
   
4) Plain HTML

   The content of all `../phoebus/**/doc/html` folders is copied into the
   generated html output directory tree.
   
   This allows including existing HTML content "as is".
   An `index.rst` file in the corresponding phoebus module may then refer
   to it via `raw` link directives.
   See `../phoebus/app/display/editor/doc` for an example.
   
   The inclusion of plain HTML content is meant to allow adding for example
   Java Doc that is auto-generated, where it would be impractical to rewrite
   the information as `*.rst`.
   Note that the direct inclusion of existing HTML content is only possible when the
   `*.rst` files are rendered as HTML, in which case `raw` directives can
   then link them to the documentation.
   When the `*.rst` files are rendered via LaTeX or PDF, plain HTML content is ignored.
   
   Whenever possible, documentation should thus use the `*.rst` file format
   and be included via the first two options.
   

For technical details on how the document components are assembled,
check `createAppIndex()` and `createPreferenceAppendix()` in `source/conf.py`.
