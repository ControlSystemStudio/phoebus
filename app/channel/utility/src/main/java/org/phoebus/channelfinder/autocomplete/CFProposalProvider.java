package org.phoebus.channelfinder.autocomplete;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.ChannelFinderService;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.spi.PVProposalProvider;

/**
 * A basic implementation for supporting autocomplete via channelfinder
 * 
 * @author Kunal Shroff
 *
 */
public class CFProposalProvider implements PVProposalProvider {

    private static final Logger log = Logger.getLogger(CFProposalProvider.class.getName());
    private ChannelFinderClient client = null;
    private boolean active = true;
    private static List<Proposal> EMPTY_RESULT = Collections.emptyList();

    public CFProposalProvider() {
        if (client == null) {
            client = ChannelFinderService.getInstance().getClient();
            if(client == null){
                log.log(Level.WARNING, "CF proposal provider got null CF client!");
                return;
            }
            try {
                client.getAllTags();
            } catch (Exception e) {
                active = false;
                log.log(Level.INFO, "Failed to create Channel Finder PVProposalProvider", e);
            }
        }
    }

    @Override
    public String getName() {
        return "Channel finder query";
    }

    private List<Proposal> result;

    @Override
    public List<Proposal> lookup(String searchString) {
        if(client == null){
            return EMPTY_RESULT;
        }
        // TODO this needs the v3.0.2 of channelfinder
        if (active) {
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put("~name", "*" + searchString + "*");
            // searchMap.put("~size", "20");
            result = client.find(searchMap).stream().limit(20).map((channel) -> {
                return new Proposal(channel.getName());
            }).collect(Collectors.toList());
            return result;
        } else {
            return Collections.emptyList();
        }

    }

}
