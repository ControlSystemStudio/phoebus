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
package org.phoebus.applications.saveandrestore.filehandler.csv;

import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parser for importing exported saveset and snapshot CSV files.
 * Also parses old SNP and BMS files.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class CSVParser extends CSVCommon {
    static private FileInputStream fileInputStream = null;

    final private Map<Integer, String> columnHeaders = new HashMap<>();
    final private List<Map<String, String>> entries = new ArrayList<>();
    final private List<String> hierarchy = new ArrayList<>();
    final private List<String> encapsulatedDataInQuotes = new ArrayList<>();
    final private List<Tag> tags = new ArrayList<>();
    private String savesetName;
    private String snapshotName;
    private String description;
    private String creator;
    private Instant timestamp;

    public List<String> getHierarchy() {
        return hierarchy;
    }

    public String getSavesetName() {
        return savesetName;
    }

    public void setSavesetName(String savesetName) {
        this.savesetName = savesetName;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<Integer, String> getColumnHeaders() {
        return columnHeaders;
    }

    public List<Map<String, String>> getEntries() {
        return entries;
    }

    static public CSVParser parse(File csvFile) throws Exception {
        fileInputStream = new FileInputStream(csvFile);

        CSVParser csvParser = new CSVParser();

        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.matches(Comment(HIERARCHY_TAG))) {
                List<String> hierarchy = Arrays.asList(Uncomment(reader.readLine()).split(DirectoryUtilities.HIERARCHY_SPLITTER));
                hierarchy = hierarchy.stream().map(String::trim).collect(Collectors.toList());

                csvParser.getHierarchy().addAll(hierarchy);
            } else if (line.equals(Comment(SNAPSHOTNAME_TAG))) {
                csvParser.setSnapshotName(Uncomment(reader.readLine()));
            } else if (line.equals(Comment(SAVESETNAME_TAG))) {
                csvParser.setSavesetName(Uncomment(reader.readLine()));
            } else if (line.matches(Comment(DESCRIPTION_TAG))) {
                String description = "";

                while (true) {
                    reader.mark(10000);
                    line = reader.readLine();

                    if (!line.startsWith(COMMENT_PREFIX) || SUPPORTED_TAGS.contains(Uncomment(line))) {
                        description = description.trim();

                        reader.reset();

                        break;
                    }

                    description += Uncomment(line) + System.lineSeparator();
                }

                csvParser.setDescription(description);
            } else if (line.matches(Comment(CREATOR_TAG))) {
                csvParser.setCreator(Uncomment(reader.readLine()));
            } else if (line.matches(Comment(DATE_TAG))) {
                csvParser.setTimestamp(TIMESTAMP_FORMATTER.get().parse(Uncomment(reader.readLine())).toInstant());
            } else if (line.matches(Comment(TAGS_TAG))) {
                while (true) {
                    reader.mark(10000);
                    line = reader.readLine();

                    if (!line.startsWith(COMMENT_PREFIX) || SUPPORTED_TAGS.contains(Uncomment(line))) {
                        reader.reset();

                        break;
                    }

                    String[] columns = csvParser.split(Uncomment(line));

                    Tag tag = Tag.builder()
                            .name(columns[0])
                            .comment(columns[1])
                            .userName(columns[2])
                            .created(TIMESTAMP_FORMATTER.get().parse(columns[3]))
                            .build();

                    csvParser.getTags().add(tag);
                }
            } else if (line.startsWith(COMMENT_PREFIX)) {
                // Do nothing for comments
            } else if (!line.startsWith(COMMENT_PREFIX) && csvParser.getColumnHeaders().isEmpty()) {
                csvParser.analyzeColumnHeader(line);
            } else {
                String[] columns = csvParser.split(line);

                Map<String, String> entry = new HashMap<>();
                for (int index = 0; index < columns.length; index++) {
                    entry.put(csvParser.getColumnHeaders().get(index), columns[index]);
                }

                csvParser.getEntries().add(entry);
            }
        }

        return csvParser;
    }

    public List<Tag> getTags() {
        return tags;
    }

    private String[] split(String line) throws Exception {
        encapsulatedDataInQuotes.clear();

        while (true) {
            if (line.indexOf("\"") >= 0) {
                int startIndex = line.indexOf("\"");
                int endIndex = line.indexOf("\"", startIndex + 1);

                if (endIndex < 0) {
                    throw new Exception("Odd number of quotation marks!");
                }

                encapsulatedDataInQuotes.add(line.substring(startIndex, endIndex + 1));
                line = line.replace(encapsulatedDataInQuotes.get(encapsulatedDataInQuotes.size() - 1), "dataInQuotes_" + (encapsulatedDataInQuotes.size() - 1));
            } else {
                break;
            }
        }

        String[] columns = line.split(CSV_SEPARATOR);

        for (int index = 0; index < columns.length; index++) {
            if (columns[index].startsWith("dataInQuotes")) {
                columns[index] = encapsulatedDataInQuotes.get(Integer.parseInt(columns[index].split("_")[1])).replaceAll("\"", "");
            }
        }

        return columns;
    }

    private void analyzeColumnHeader(String line) {
        String[] columnStrings = line.split(CSV_SEPARATOR);

        for (int index = 0; index < columnStrings.length; index++) {
            if (SUPPORTED_COLUMNS.contains(columnStrings[index])) {
                getColumnHeaders().put(index, columnStrings[index]);
            }
        }
    }
}