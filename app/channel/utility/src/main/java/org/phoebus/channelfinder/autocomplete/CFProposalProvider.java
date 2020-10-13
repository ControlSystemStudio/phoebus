package org.phoebus.channelfinder.autocomplete;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.ChannelFinderException;
import org.phoebus.channelfinder.ChannelFinderService;
import org.phoebus.channelfinder.utility.ConnectionCheckJob;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.spi.PVProposalProvider;

/**
 * A basic implementation for supporting autocomplete via channelfinder
 * 
 * @author Kunal Shroff
 *
 */
public class CFProposalProvider implements PVProposalProvider {

    public static final Logger logger = Logger.getLogger(CFProposalProvider.class.getName());

    private ChannelFinderClient client = null;
    private boolean active = true;
    private static List<Proposal> EMPTY_RESULT = Collections.emptyList();

    public CFProposalProvider() {
        if (client == null) {
            client = ChannelFinderService.getInstance().getClient();
            if(client == null){
                logger.log(Level.WARNING, "CF proposal provider got null CF client!");
                return;
            }

            ConnectionCheckJob.submit(this.client, new BiConsumer<String, Exception>() {
                @Override
                public void accept(String s, Exception e) {
                    active = false;
                    Throwable cause = e.getCause();
                    while (cause != null && ! (cause instanceof ChannelFinderException))
                        cause = cause.getCause();
                    if (cause != null)
                        e = (Exception)cause;
                    CFProposalProvider.logger.log(Level.INFO, "Failed to create Channel Finder PVProposalProvider", e);
                }
            });
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
