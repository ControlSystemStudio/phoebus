package org.csstudio.display.builder.model.properties;

import java.util.HashMap;

public class CallPVActionInfo extends ActionInfo {
    private final String pv;
    private final HashMap<String, String> args;

    /**
     * @param description Action description
     */
    public CallPVActionInfo(final String description, final String pv, final HashMap<String, String> args) {
        super(description);
        this.pv = pv;
        this.args = args;
    }

    @Override
    public ActionType getType() {
        return ActionType.CALL_PV;
    }

    public String getPV() {
        return pv;
    }

    public HashMap<String, String> getArgs() {
        return args;
    }
}
