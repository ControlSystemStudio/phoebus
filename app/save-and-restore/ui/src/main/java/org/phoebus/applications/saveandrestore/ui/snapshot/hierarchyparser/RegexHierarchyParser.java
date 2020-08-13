/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.snapshot.hierarchyparser;

import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.framework.preferences.PreferencesReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hierarchy parser using regex(es) provided by the settings.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class RegexHierarchyParser implements IHierarchyParser {

    private PreferencesReader preferencesReader = (PreferencesReader) ApplicationContextProvider.getApplicationContext().getBean("preferencesReader");
    private final String regexListString = preferencesReader.get("regexHierarchyParser.regexList");

    @Override
    public List<String> parse(String pvName) {
        List<String> regexList = Arrays.asList(regexListString.split(","));
        List<Pattern> regexPatterns = regexList.stream()
                .map(regex -> Pattern.compile(regex.trim()))
                .collect(Collectors.toList());

        List<String> parsedPV = new ArrayList<>();

        regexPatterns.stream()
                .filter(pattern -> {
                    Matcher matcher = pattern.matcher(pvName);
                    if (matcher.find()) {
                        for (int group = 1; group < matcher.groupCount() + 1; group++) {
                            parsedPV.add(matcher.group(group));
                        }

                        return true;
                    }
                    return false;
                }).findFirst()
                .ifPresentOrElse(action -> {}, () -> {
                    parsedPV.add("(Unmatched)");
                    parsedPV.add(pvName);
                });

        return parsedPV;
    }
}
