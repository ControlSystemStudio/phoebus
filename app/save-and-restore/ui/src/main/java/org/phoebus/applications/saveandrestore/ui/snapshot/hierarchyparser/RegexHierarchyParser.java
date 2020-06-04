package org.phoebus.applications.saveandrestore.ui.snapshot.hierarchyparser;

import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.ui.snapshot.TableEntry;
import org.phoebus.framework.preferences.PreferencesReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexHierarchyParser implements IHierarchyParser {

    private PreferencesReader preferencesReader = (PreferencesReader) ApplicationContextProvider.getApplicationContext().getBean("preferencesReader");
    private final String regexListString = preferencesReader.get("regexHierarchyParser.regexList");

    @Override
    public List<String> parse(String pvName) {
        System.out.println(regexListString);
        List<String> regexList = Arrays.asList(regexListString.split(","));
        System.out.println(regexList);
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
