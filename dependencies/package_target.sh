#!/bin/sh

# A script which creates and packages the Phoebus target maven repo
# The outout tar/zip can be downloaded and the phoebus sources can be built against is using 
# the maven.repo.local option 

mvn clean install -Dmaven.repo.local=targetRepository -P packageTarget,docs
tar -cf phoebus-target.tar targetRepository/
rm -rf targetRepository