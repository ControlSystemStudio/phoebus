#!/bin/sh
# Example for an external command with long runtime

echo "Long running command"
for i in 1 2 3 4 5 6 7 8 9 10
do
    sleep 1
    echo "Running for $i seconds"
done
echo "Finished OK"
