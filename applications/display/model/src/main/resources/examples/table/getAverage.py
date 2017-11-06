from org.csstudio.display.builder.runtime.script import PVUtil
qualities = PVUtil.getStructureElement(pvs[0], "Quality")
average = 0
for rating in qualities:
	average += rating
average /= len(qualities)
widget.setPropertyValue("text", str(average))
