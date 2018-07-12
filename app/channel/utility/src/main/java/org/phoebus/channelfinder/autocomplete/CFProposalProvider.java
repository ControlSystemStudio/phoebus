package org.phoebus.channelfinder.autocomplete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public CFProposalProvider() {
        if (client == null) {
            client = ChannelFinderService.getInstance().getClient();
        }
    }

    @Override
    public String getName() {
        return "Channel finder query";
    }

    private List<Proposal> result;

    @Override
    public List<Proposal> lookup(String searchString) {
        // TODO this needs the v3.0.2 of channelfinder
         Map<String, String> searchMap = new HashMap<String, String>();
         searchMap.put("~name", "*" + searchString + "*");
        // searchMap.put("~size", "20");
        result = client.find(searchMap).stream().limit(20).map((channel) -> {
            return new Proposal(channel.getName());
        }).collect(Collectors.toList());
        return result;

    }

}
