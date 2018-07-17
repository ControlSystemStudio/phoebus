from org.csstudio.display.builder.runtime.script import PVUtil
import time
    
run = PVUtil.getInt(pvs[0])
max_time = PVUtil.getString(pvs[1])
indicator = pvs[2]

def format(secs):
    """Format seconds as 'MM:SS' """
    sec = int(secs)
    min = sec / 60
    sec -= min*60
    return "%02d:%02d" % (min, sec)
  
if run:
    # Expecting "MM:SS" as runtime
    total = float(max_time[0:2])*60 + float(max_time[3:5])
    end = time.time() + total
    now = time.time()
    while run  and  now < end:
        secs = end - now
        fraction = secs/total
        widget.setPropertyValue("total_angle", 360.0*fraction)
        
        if fraction < 0.25:
            color = [ 255, 0, 0 ]
        elif fraction < 0.4:
            color = [ 255, 255, 0 ]
        else:
            color = [ 0, 255, 0 ] 
        widget.setPropertyValue("background_color", color)
        
        indicator.write(format(secs))
        time.sleep(0.2)
        now = time.time()
        run = PVUtil.getInt(pvs[0])

    widget.setPropertyValue("total_angle", 360.0)
    indicator.write(format(total))
# else: Script triggered once more by run==0, ignore
