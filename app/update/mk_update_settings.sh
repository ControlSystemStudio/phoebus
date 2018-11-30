#!/bin/sh

if [ $# -ne 1 ]
then
    echo 'Usage:  mk_update_settings.sh  url'
    echo ''
    echo 'URL, for example http://your.update.site/path/to/product-$(arch).zip,'
    echo 'is where the update process will check for a new version.'
    echo 'It may contain $(arch), which gets replaced by "linux", "mac" or "win"'
    echo 'depending on the OS.'
    echo ''
    echo 'Redirect the output into a file settings.ini'
    echo 'in the product directory, which will be bundled in the product distribution ZIP.'
    echo ''
fi

URL="$1"
# Use end of today as version, so anything build on the next day will look new
# and suggest update
VERSION=`date +'%Y-%m-%d 23:59'`

echo ""
echo "# Self-update"
echo "org.phoebus.applications.update/current_version=$VERSION"
echo "org.phoebus.applications.update/update_url=$URL"
