import os

f = open("release.log", "a")
f.write(os.getcwd())
f.write("Updating the classpath file in preparation for phoebus release \n")