Example Product
===============

This example product selects most of the Phoebus `core-*` and `app-*` components
into an executable UI product.

Each site will likely want to create their own product to

 1. Select which components they need
 2. Add a custom `settings.ini` for their EPICS network settings
 3. Maybe add a custom splash screen, welcome page, launcher
 4. Some sites might want to add site-specific source code

To accomplish 1-3, no source code is needed, only a `pom.xml` or `ant` build file
as shown in this directory.

For site-specific examples, see

 * https://github.com/shroffk/nsls2-phoebus (actually creates several products for 'beamline' vs. 'accelerator')
 * https://github.com/kasemir/phoebus-sns