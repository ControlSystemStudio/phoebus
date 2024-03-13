package org.csstudio.display.builder.model.properties;

import java.util.HashMap;

public class CallPVActionInfo extends ActionInfo {
    private final String pv;
    private final HashMap<String, String> args;
    private final String returnPV;

    /**
     * @param description Action description
     */
    public CallPVActionInfo(final String description, final String pv, final HashMap<String, String> args, final String returnPV) {
        super(description);
        this.pv = pv;
        this.args = args;
        this.returnPV = returnPV;
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

    public String getReturnPV() {
        return returnPV;
    }
}
