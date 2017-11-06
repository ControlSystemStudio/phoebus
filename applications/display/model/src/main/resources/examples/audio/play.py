from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil
from java.util.concurrent import TimeUnit
from java.lang import Throwable

max_duration = 5
play = PVUtil.getInt(pvs[0])
if play:
    widget.setPropertyValue("text", "Playing audio..")
    # Start playing audio
    audio = ScriptUtil.playAudio(widget, "timer.wav")
    try:
        # Could wait until end of playback with
        #   audio.get()
        # In this example, using a timeout to limit the playback
        audio.get(max_duration, TimeUnit.SECONDS)
        # Without timeout, we're done
        widget.setPropertyValue("text", "Done")
    except Throwable, ex:
        # In case of timeout, we cancel the player
        print(str(ex))
        widget.setPropertyValue("text", "Cancelling")
        audio.cancel(True)
# else: Called with play=0, so don't do anything
    
print("Script done")

