#!/bin/sh
# Example for an external command with short runtime

echo "Example command"
echo "Received $# arguments:"
for arg in "$@"
do
    echo "Arg: $arg"
done
echo "Example warning" >&2
echo "Directory: "
echo `pwd`
if [ -f cmd_short.sh ]
then
    echo "Finished OK"
else
    echo "Wrong directory" >&2
    exit 2
 fi