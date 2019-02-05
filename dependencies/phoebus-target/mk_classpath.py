# Create .classpath file by listing all the target/lib/*.jar files
import glob

print ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
print ("<classpath>")

for jar in sorted(glob.glob('target/lib/*.jar')):
    # Skip (comment) javafx jars
    # Oracle JDK 9 and 10 include them,
    # in which case we do NOT put them into .classpath
    # Open JDK 11 on the other hand needs them in the .classpath...
    skip = "/javafx" in jar
    if skip:
        print ("    <!--")
    print ("    <classpathentry exported=\"true\" kind=\"lib\" path=\"" + jar + "\"/>")
    if skip:
        print ("    -->")

print ("    <classpathentry kind=\"output\" path=\"bin\"/>")
print ("</classpath>")
