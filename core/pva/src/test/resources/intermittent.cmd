# To run IOC that keeps quitting:
#
# while true
# do 
#   softIocPVA -x blah -m N='' -d demo.db intermittent.cmd
#   sleep 1
# done# 

epicsThreadSleep(5)
dbpf "blah:exit" 0
