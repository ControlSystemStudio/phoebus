import subprocess
str = "echo \"There are {} new messages.\" | festival --tts".format(5)
subprocess.call(str, shell=True)