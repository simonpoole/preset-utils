package ch.poole.osm.presetutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Parse a JOSM format preset file and generate some stats
 * 
 * There is an underlying assumption that preset items will contain the most important / top-level tags first
 * 
 * Some parts of this were nicked from Vespucci and some from Apache CLI sample code.
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class NsiStats {

    private static final String INPUT  = "input";
    private static final String OUTPUT = "output";

    private static final String WERID_WHOLE_WORLD_NSI_VALUE = "001";

    private static final String TAGS_FIELD         = "tags";
    private static final String EXCLUDE_FIELD      = "exclude";
    private static final String INCLUDE_FIELD      = "include";
    private static final String LOCATION_SET_FIELD = "locationSet";
    private static final String DISPLAY_NAME_FIELD = "displayName";
    private static final String ITEMS_FIELD        = "items";
    private static final String PROPERTIES_FIELD   = "properties";
    private static final String NSI_FIELD          = "nsi";

    private static final String NSI_FILE = "name-suggestions.min.json";

    public class TagMap extends TreeMap<String, String> {

        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : this.entrySet()) {
                builder.append(entry.getKey().replace("|", " ") + "=" + entry.getValue() + "|");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }

    /**
     * Container class for a name and the associated tags
     * 
     * @author simon
     *
     */
    public class NameAndTags implements Comparable<NameAndTags> {
        private final String       name;
        private final int          count;
        private final List<String> includeRegions;
        private final List<String> excludeRegions;
        final TagMap               tags;

        /**
         * Construct a new instance
         * 
         * @param name the value for the name tag
         * @param tags associated tags
         * @param count the times this establishment was found, works as a proxy for importance, NSI V6 doesn't support
         *            this anymore
         * @param includeRegions regions this is applicable to
         * @param excludeRegions regions this is not applicable to
         */
        public NameAndTags(@NotNull String name, @NotNull TagMap tags, @NotNull int count, @Nullable List<String> includeRegions,
                @Nullable List<String> excludeRegions) {
            this.name = name;
            this.tags = tags;
            this.count = count;
            this.includeRegions = includeRegions;
            this.excludeRegions = excludeRegions;
        }

        @Override
        public String toString() {
            return getName() + " (" + tags.toString() + ")";
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the tags
         */
        public TagMap getTags() {
            return tags;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * Check if this entry is in use or not in use in a specific region
         * 
         * @param currentRegions the list of regions to check for, null == any region
         * @return true if the entry is appropriate for the region
         */
        public boolean inUseIn(@Nullable List<String> currentRegions) {
            if (currentRegions != null) {
                boolean inUse = false;
                if (includeRegions != null) {
                    for (String current : currentRegions) {
                        if (includeRegions.contains(current)) {
                            inUse = true;
                            break;
                        }
                    }
                } else {
                    inUse = true;
                }
                if (excludeRegions != null) {
                    for (String current : currentRegions) {
                        if (excludeRegions.contains(current)) {
                            inUse = false;
                            break;
                        }
                    }
                }
                return inUse;
            }
            return true;
        }

        @Override
        public int compareTo(@NotNull NameAndTags another) {
            if (another.name.equals(name)) {
                if (getCount() > another.getCount()) {
                    return +1;
                } else if (getCount() < another.getCount()) {
                    return -1;
                }
                // more tags is better
                if (tags.size() > another.tags.size()) {
                    return +1;
                } else if (tags.size() < another.tags.size()) {
                    return -1;
                }
                return 0;
            }
            return name.compareTo(another.name);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NameAndTags)) {
                return false;
            }
            return name.equals(((NameAndTags) obj).name) && tags.equals(((NameAndTags) obj).tags);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + (name == null ? 0 : name.hashCode());
            result = 37 * result + (tags == null ? 0 : tags.hashCode());
            return result;
        }
    }

    private static MultiHashMap<String, NameAndTags> nameList       = new MultiHashMap<>(false); // names -> multiple
                                                                                                 // entries
    private static MultiHashMap<String, NameAndTags> tags2namesList = new MultiHashMap<>(false); // tagmap -A multiple

    /**
     * Read the NSI configuration from assets
     * 
     * @param is InputStream to read from
     */
    private void readNSI(@NotNull InputStream is) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            reader.beginObject(); // top level
            while (reader.hasNext()) {
                if (NSI_FIELD.equals(reader.nextName())) {
                    reader.beginObject(); // entries
                    while (reader.hasNext()) {
                        reader.nextName();
                        reader.beginObject(); // entry
                        while (reader.hasNext()) {
                            String jsonName = reader.nextName();
                            switch (jsonName) {
                            case PROPERTIES_FIELD:
                                reader.skipValue();
                                break;
                            case ITEMS_FIELD:
                                reader.beginArray(); // item
                                while (reader.hasNext()) {
                                    reader.beginObject();
                                    String name = null;
                                    List<String> includeRegions = null;
                                    List<String> excludeRegions = null;
                                    TagMap tags = new TagMap();
                                    while (reader.hasNext()) {
                                        String field = reader.nextName();
                                        switch (field) {
                                        case DISPLAY_NAME_FIELD:
                                            name = reader.nextString();
                                            break;
                                        case LOCATION_SET_FIELD:
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                switch (reader.nextName()) {
                                                case INCLUDE_FIELD:
                                                    includeRegions = readStringArray(reader);
                                                    break;
                                                case EXCLUDE_FIELD:
                                                    excludeRegions = readStringArray(reader);
                                                    break;
                                                default:
                                                    reader.skipValue();
                                                }
                                            }
                                            reader.endObject();
                                            break;
                                        case TAGS_FIELD:
                                            readTags(reader, tags);
                                            break;
                                        default:
                                            reader.skipValue();
                                            break;
                                        }
                                    } // item
                                    reader.endObject();
                                    if (name != null) {
                                        NameAndTags entry = new NameAndTags(name, tags, 1, includeRegions, excludeRegions);
                                        nameList.add(name, entry);
                                        tags2namesList.add(tags.toString(), entry);
                                    }
                                } // items
                                reader.endArray();
                                break;
                            default:
                                reader.skipValue();
                                break;
                            }
                        }
                        reader.endObject(); // entry
                    }
                    reader.endObject(); // entries
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject(); // top level
        } catch (IOException | IllegalStateException e) {
            System.out.println("Got exception reading " + NSI_FILE + " " + e.getMessage());
        }
    }

    /**
     * Read and filter tags
     * 
     * @param reader the JsonReader
     * @param tags the map to save the tags in
     * @throws IOException if reading or parsing fails
     */
    private void readTags(@NotNull JsonReader reader, @NotNull TagMap tags) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String k = reader.nextName();
            tags.put(k, reader.nextString());
        }
        reader.endObject(); // tags

    }

    /**
     * Read a JsonArray of string in to a String[]
     * 
     * @param reader the JsonReader
     * @return a String[] with the JSON strings
     * @throws IOException on IO and parse errors
     */
    @Nullable
    private List<String> readStringArray(@NotNull JsonReader reader) throws IOException {
        boolean valid = true;
        List<String> result = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) { // FIXME weird location stuff
                String code = reader.nextString().toUpperCase(Locale.US);
                if (WERID_WHOLE_WORLD_NSI_VALUE.equals(code)) {
                    valid = false;
                } else {
                    result.add(code);
                }
            } else {
                reader.skipValue();
                valid = false;
            }
        }
        reader.endArray();
        return valid ? result : null;
    }

    void dumpStats(PrintWriter pw) {
        Map<String, Integer> stats = new HashMap<>();
        Map<String, Integer> statsName = new HashMap<>();
        Map<String, Integer> statsNameBrand = new HashMap<>();
        Map<String, String> weirdStuff = new HashMap<>();
        int itemCount = nameList.getValues().size();
        for (NameAndTags nt : nameList.getValues()) {
            String statsKey = null;
            for (Entry<String, String> e : nt.getTags().entrySet()) {
                String key = e.getKey();
                if (Tags.OBJECT_KEYS.contains(key) || "route".equals(key)) {
                    statsKey = key + "=" + e.getValue();
                    intCount(stats, statsKey);
                    break;
                }
            }
            if (statsKey != null) {
                String name = nt.getTags().get("name");
                if (name != null) {
                    intCount(statsName, statsKey);
                    if (name.equals(nt.getTags().get("brand"))) {
                        intCount(statsNameBrand, statsKey);
                    }
                }
            } else {
                weirdStuff.put(nt.getName(), nt.getTags().toString());
            }            
        }
        for (Entry<String, Integer> e : stats.entrySet()) {
            String key = e.getKey();
            Integer nameCount = statsName.get(key);
            Integer nameBrandCount = statsNameBrand.get(key);
            pw.print(key + "\t" + e.getValue() + "\t" + (nameCount != null ? nameCount : 0) + "\t" + (nameBrandCount != null ? nameBrandCount : 0) + "\n");
        }
        for (Entry<String,String> e : weirdStuff.entrySet()) {
            pw.print(e.getKey() + "\t" + e.getValue() + "\n");
        }
        pw.flush();
        // print stats to standard out
        System.out.print("Total items " + itemCount + "\n");

        System.out.flush();

    }

    /**
     * Increment the count value in a map
     * 
     * @param stats the Map hodling the stats
     * @param statsKey the key
     */
    public void intCount(@NotNull Map<String, Integer> stats, @NotNull String statsKey) {
        Integer count = stats.get(statsKey);
        if (count == null) {
            count = 0;
        }
        count++;
        stats.put(statsKey, count);
    }

    public static void main(String[] args) {
        // defaults
        InputStream is = System.in;
        OutputStreamWriter os = null;

        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            NsiStats nsiStats = new NsiStats();

            // arguments
            Option inputFile = Option.builder("i").longOpt(INPUT).hasArg().desc("input preset file, default: standard in").build();
            Option outputFile = Option.builder("o").longOpt(OUTPUT).hasArg().desc("output stats file, default: standard out").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(outputFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT)) {
                    // initialise the member variable
                    String input = line.getOptionValue(INPUT);
                    is = new FileInputStream(input);
                }
                if (line.hasOption(OUTPUT)) {
                    String output = line.getOptionValue(OUTPUT);
                    os = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("PresetStats", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            nsiStats.readNSI(is);
            nsiStats.dumpStats(new PrintWriter(os));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // NOSONAR
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                // NOSONAR
            }
        }
    }
}
