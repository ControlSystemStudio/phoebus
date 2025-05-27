/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.filterselection;


import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BeamModeFilterSelector extends FilterSelector{

    public BeamModeFilterSelector(){
        try {
            PV pv = PVPool.getPV("ao1");
            pv.onValueEvent().subscribe(value -> {
                if (!VTypeHelper.isDisconnected(value)) {
                   if(VTypeHelper.toDouble(value) > 1.0){
                       filterSelected("golden only");
                   }
                   else{
                       filterUnselected("golden only");
                   }
                }
                else{
                    filterUnselected("golden only");
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSupportedFilterNames() {
        return List.of("golden only");
    }



}
