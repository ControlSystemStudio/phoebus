/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ProposalProvider} offering proposals based on existing save & restore tag names.
 */
public class TagProposalProvider implements ProposalProvider {

    private final List<String> tagNames = new ArrayList<>();
    private final SaveAndRestoreService saveAndRestoreService;

    public TagProposalProvider(SaveAndRestoreService saveAndRestoreService){
        this.saveAndRestoreService = saveAndRestoreService;
    }

    @Override
    public String getName(){
        return "Save & Restore tags";
    }

    @Override
    public List<Proposal> lookup(String text){
        if(tagNames.isEmpty()){
            List<Tag> allTags;
            try {
                allTags = saveAndRestoreService.getAllTags();
            } catch (Exception e) {
                Logger.getLogger(TagProposalProvider.class.getName())
                        .log(Level.WARNING, "Unable to get tags from save & restore service", e);
                return Collections.emptyList();
            }
            allTags.forEach(tag -> {
                if(!tagNames.contains(tag.getName()) && !tag.getName().equalsIgnoreCase(Tag.GOLDEN)){ // Add tags names only once. Omit "golden" tag.
                    tagNames.add(tag.getName());
                }
            });
        }
        List<Proposal> proposals = new ArrayList<>();
        tagNames.forEach(tagName -> {
            if(tagName.contains(text)){
                proposals.add(new Proposal(tagName));
            }
        });
        return proposals;
    }
}
