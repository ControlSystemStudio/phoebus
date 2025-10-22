package org.phoebus.pv.tango;

import java.util.List;

import org.epics.vtype.Display;
import org.epics.vtype.DisplayProvider;
import org.epics.vtype.EnumDisplay;

public class AdvancedEnumDisplay extends EnumDisplay implements DisplayProvider {

    private EnumDisplay source ;
    private Display display ;
    
    private AdvancedEnumDisplay(Display display,EnumDisplay source) {
        this.source = source;
        this.display = display;
    }
    
    @Override
    public List<String> getChoices() {
        return source != null ? source.getChoices() : null;
    }

    @Override
    public Display getDisplay() {
        return display;
    }
    
    
    /**
     * New EnumDisplay with the given choices.
     * 
     * @param choices the enum choices
     * @return the new display
     */
    public static EnumDisplay of(final Display display, final String... choices) {
        EnumDisplay enumDisp = choices != null ? EnumDisplay.of(choices) : null;
        return new AdvancedEnumDisplay(display,enumDisp);
    }

   

}
