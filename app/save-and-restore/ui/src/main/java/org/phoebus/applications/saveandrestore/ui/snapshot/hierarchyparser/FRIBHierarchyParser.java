package org.phoebus.applications.saveandrestore.ui.snapshot.hierarchyparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FRIBHierarchyParser implements IHierarchyParser {
    private final String sectionSeparator = ":";
    private final String subsectionSeparator = "_";

    public static final Logger LOGGER = Logger.getLogger(FRIBHierarchyParser.class.getName());

    private final List<String> parsedPV = new ArrayList<>();

    @Override
    public List<String> parse(String pvName) {
        parsedPV.clear();

        List<String> sections = Arrays.asList(pvName.split(sectionSeparator));
        sections = organizeSplit(sections);

        String systemString = sections.get(0);
        String deviceString = null;
        String signalString = null;

        if (sections.size() == 2) {
            deviceString = sections.get(1);
        } else if (sections.size() == 3) {
            deviceString = sections.get(1);
            signalString = sections.get(2);
        }

        if (systemString.contains(subsectionSeparator)) {
            List<String> split = Arrays.asList(systemString.split(subsectionSeparator));
            String system = split.get(0);
            String subSystem = split.get(1);

            // Checking PV names starting with "_"
            if (system.equals("")) {
                parsedPV.add("EMPTY");

                if (split.size() > 2) {
                    system = split.get(1);
                    subSystem = split.get(2);

                    if (split.size() > 3) {
                        for (int index = 3; index < split.size(); index++) {
                            subSystem += subsectionSeparator + split.get(index);
                        }
                    }
                }
            } else {
                if (split.size() > 2) {
                    for (int index = 2; index < split.size(); index++) {
                        subSystem += subsectionSeparator + split.get(index);
                    }
                }
            }

            if (subSystem.equals("")) {
                subSystem = "(EMPTY)";
            }

            parsedPV.add(system);
            parsedPV.add(subSystem);
        } else {
            parsedPV.add(systemString);
        }

        if (deviceString != null) {
            if (deviceString.contains(subsectionSeparator) && !(deviceString.contains("{") || deviceString.contains("}"))) {
                List<String> split = Arrays.asList(deviceString.split(subsectionSeparator));
                String device = split.get(0);
                String subDevice = split.get(1);

                if (device.equals("")) {
                    parsedPV.add("EMPTY");

                    if (split.size() > 2) {
                        device = split.get(1);
                        subDevice = split.get(2);

                        if (split.size() > 3) {
                            for (int index = 3; index < split.size(); index++) {
                                subDevice += subsectionSeparator + split.get(index);
                            }
                        }
                    }
                } else {
                    if (split.size() > 2) {
                        for (int index = 2; index < split.size(); index++) {
                            subDevice += subsectionSeparator + split.get(index);
                        }
                    }
                }

                if (subDevice.equals("")) {
                    subDevice = "(EMPTY)";
                }

                parsedPV.add(device);
                parsedPV.add(subDevice);
            } else {
                parsedPV.add(deviceString);
            }
        }

        if (signalString != null) {
            parsedPV.add(signalString);
        }

        return parsedPV;
    }

    private List<String> organizeSplit(List<String> sections) {
        if (sections.size() == 1) {
            return sections;
        }

        List<String> organizedSections = new ArrayList<>();

        int frontIndex = 0;
        int rearIndex = 1;

        while (rearIndex < sections.size()) {
            String frontSection = sections.get(frontIndex);
            String rearSection = sections.get(rearIndex);

            if (frontSection.contains("{") && !frontSection.contains("}")) {
                String combinedSection = frontSection + sectionSeparator + rearSection;

                sections.set(frontIndex, combinedSection);

                if (rearSection.contains("}")) {
                    organizedSections.add(combinedSection);

                    frontIndex = rearIndex + 1;
                    rearIndex = frontIndex + 1;

                    if (rearIndex == sections.size()) {
                        organizedSections.add(sections.get(frontIndex));

                        break;
                    }
                } else {
                    rearIndex++;

                    if (rearIndex == sections.size()) {
                        organizedSections.add(combinedSection);

                        break;
                    }
                }
            } else {
                organizedSections.add(frontSection);

                frontIndex++;
                rearIndex++;

                if (rearIndex == sections.size()) {
                    organizedSections.add(rearSection);

                    break;
                }
            }
        }

        if (organizedSections.size() > 3) {
            String lastSection = organizedSections.get(2);

            while (organizedSections.size() > 3) {
                String remainingSection = organizedSections.get(3);

                lastSection += ":" + remainingSection;
                organizedSections.remove(3);
            }

            organizedSections.set(2, lastSection);

            LOGGER.warning("PV name has more than 2 colons: " + organizedSections.stream().collect(Collectors.joining(":")));
        }

        return organizedSections;
    }
}
