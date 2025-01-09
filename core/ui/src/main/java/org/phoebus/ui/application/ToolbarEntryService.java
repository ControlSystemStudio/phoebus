package org.phoebus.ui.application;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.phoebus.ui.Preferences;
import org.phoebus.ui.spi.ToolbarEntry;

public class ToolbarEntryService {

    private static ToolbarEntryService toolbarEntryService;
    private List<ToolbarEntry> toolbarEntries = new ArrayList<>();

    private ToolbarEntryService() {
        final List<ToolbarEntry>  available = new ArrayList<>();
        ServiceLoader.load(ToolbarEntry.class).forEach(available::add);

        // Add desired toolbar entries in specified order
        for (String desired : Preferences.toolbar_entries.split(" *, *"))
        {
            if (desired.equals("*"))
            {    // Add all that are left, done
                toolbarEntries.addAll(available);
                break;
            }
            // Should desired entry actually be removed?
            boolean suppress = desired.startsWith("!");
            if (suppress)
                desired = desired.substring(1);
            // Skip entries handled in PhoebusApplication
            if (desired.equals("Home") ||  desired.equals("Top Resources")  ||  desired.equals("Layouts") || desired.equals("Add Layouts"))
                continue;
            // Add specific 'desired' entry
            ToolbarEntry found = null;
            for (ToolbarEntry entry : available)
                if (entry.getId().equalsIgnoreCase(desired))
                {
                    found = entry;
                    break;
                }
            if (found != null)
            {
                available.remove(found);
                if (! suppress)
                    toolbarEntries.add(found);
            }
            else
                System.out.println("toolbar_entries: Cannot find '" + desired + "'");
        }
    }

    public static synchronized ToolbarEntryService getInstance() {
        if (toolbarEntryService == null) {
            toolbarEntryService = new ToolbarEntryService();
        }
        return toolbarEntryService;
    }

    /**
     * Get the list of registered toolbar entries
     * @return
     */
    public List<ToolbarEntry> listToolbarEntries(){
        return toolbarEntries;
    } 
    
}
