package ch.poole.osm.presetutils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ch.poole.osm.presetutils.ID2JOSM.ValueAndDescription;

/**
 * Get tags from taginfo
 * 
 * Some parts of this were nicked from Vespucci and some from Apache CLI sample code.
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class TagsFromTaginfo {

    private static final String             NOSUBTAGS         = "nosubtags";
    private static final String             OUTPUT            = "output";
    private static final String             MINIMUM           = "minimum";
    /**
     * An set of tags considered 'important'. These are typically tags that define real-world objects and not properties
     * of such.
     */
    public static final Set<String>         OBJECT_KEYS       = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("aerialway", "aeroway", "amenity", "barrier", "boundary", "building", "craft", "emergency", "ford", "geological", "highway",
                    "historic", "landuse", "leisure", "man_made", "military", "natural", "office", "place", "power", "public_transport", "railway", "shop",
                    "tourism", "waterway", "type", "entrance", "pipeline", "healthcare", "playground", "attraction", "traffic_sign", "traffic_sign:forward",
                    "traffic_sign:backward", "golf", "indoor", "cemetry", "building:part", "landcover", "advertising")));
    public static final Map<String, String> SECOND_LEVEL_KEYS = new HashMap<>();
    static {
        SECOND_LEVEL_KEYS.put("vending_machine", "vending");
        SECOND_LEVEL_KEYS.put("stadium", "sport");
        SECOND_LEVEL_KEYS.put("pitch", "sport");
        SECOND_LEVEL_KEYS.put("sports_centre", "sport");
    }
    public static final Map<String, String> KEYS_FOR_SPECIFIC_ELEMENT = new HashMap<>();
    static {
        KEYS_FOR_SPECIFIC_ELEMENT.put("type", "relations");
    }
    public static final Set<String> NOT_SECOND_LEVEL_KEYS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("phone", "health", "census", "postal_code", "maxspeed", "designated", "heritage", "incline", "network",
                    "level", "motorcycle", "bicycle", "snowmobile", "organic", "fireplace", "boat", "bar", "compressed_air", "swimming_pool", "taxi", "atm",
                    "telephone", "waste_basket", "drinking_water", "restaurant", "sanitary_dump_station", "water_point", "biergarten", "bench", "give_way",
                    "access", "noexit", "outdoor_seating", "goods", "second_hand", "atv", "tobacco", "household", "ski", "ice_cream", "vacant", "car",
                    "fishing", "toilet", "shelter", "handrail", "monorail", "unisex", "private", "exit", "video", "window", "laundry", "table", "steps")));

    class TagStats {
        String tag   = null;
        int    count = 0;
    }

    List<TagStats> tags = new ArrayList<>();

    void dumpTags(PrintWriter pw, int minCount, boolean addSubTags) {
        for (String object : OBJECT_KEYS) {
            String filter = KEYS_FOR_SPECIFIC_ELEMENT.get(object); // normally == null == all elements
            List<ValueAndDescription> values = TagInfo.getOptionsFromTagInfo(object, filter, false, minCount, 0, false);
            List<ValueAndDescription> combinationsList = TagInfo.getCombinationKeys(object, minCount / 5);
            Set<String> combinations = new HashSet<>();
            if (combinationsList != null) {
                for (ValueAndDescription combination : combinationsList) {
                    combinations.add(combination.value);
                }
            }
            if (values != null && !values.isEmpty()) {
                for (ValueAndDescription value : values) {
                    TagStats stats = new TagStats();
                    stats.tag = object + "=" + value.value;
                    tags.add(stats);

                    // handle sub keys
                    String subKey = value.value;
                    if (SECOND_LEVEL_KEYS.containsKey(subKey)) {
                        subKey = SECOND_LEVEL_KEYS.get(subKey);
                    }
                    if (!combinations.contains(subKey)) { // do this after replacing subKey
                        System.out.println(subKey + " discarded because not in combinations for key " + object);
                        continue;
                    }
                    if (NOT_SECOND_LEVEL_KEYS.contains(subKey) || OBJECT_KEYS.contains(subKey)) {
                        System.out.println(subKey + " discarded because of manual discard, key " + object);
                        continue;
                    }
                    if (addSubTags) {
                        List<ValueAndDescription> subValues = TagInfo.getOptionsFromTagInfo(subKey, filter, false, minCount / 5, 0, false);
                        if (values != null) {
                            for (ValueAndDescription sub : subValues) {
                                if ("yes".equals(sub.value) || "no".equals(sub.value)) { // these are in general
                                                                                         // nonsense
                                    continue;
                                }
                                stats = new TagStats();
                                stats.tag = object + "=" + value.value + " / " + subKey + "=" + sub.value;
                                tags.add(stats);
                            }
                        }
                    }
                }
            } else {
                TagStats stats = new TagStats();
                stats.tag = object;
                tags.add(stats);
            }
        }

        for (TagStats s : tags) {
            pw.print(s.tag + ",0\n");
        }
        pw.flush();

    }

    public static void main(String[] args) {
        // defaults
        OutputStreamWriter os = null;
        int minCount = 500;
        boolean noSubTags = false;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            Option outputFile = Option.builder("o").longOpt(OUTPUT).hasArg().desc("output stats file, default: standard out").build();

            Option min = Option.builder("m").longOpt(MINIMUM).hasArg().desc("minimum occurance for values to be used, default: 500").build();

            Option noSubTagsOption = Option.builder("n").longOpt(NOSUBTAGS).desc("don't add subtags, default: false").build();

            Options options = new Options();

            options.addOption(outputFile);
            options.addOption(min);
            options.addOption(noSubTagsOption);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);

                if (line.hasOption(OUTPUT)) {
                    String output = line.getOptionValue(OUTPUT);
                    os = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
                }

                if (line.hasOption(MINIMUM)) {
                    String minCountString = line.getOptionValue(MINIMUM);
                    try {
                        minCount = Integer.valueOf(minCountString);
                    } catch (NumberFormatException e) {
                        System.err.println("Illegal option value " + minCountString);
                    }
                }
                noSubTags = line.hasOption(NOSUBTAGS);

            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("TagsFromTaginfo", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }
            TagsFromTaginfo t = new TagsFromTaginfo();
            t.dumpTags(new PrintWriter(os), minCount, !noSubTags);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
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
