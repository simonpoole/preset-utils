package ch.poole.osm.presetutils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    private static final String NOSUBTAGS = "nosubtags";
    private static final String OUTPUT    = "output";
    private static final String MINIMUM   = "minimum";

    class TagStats {
        String tag   = null;
        int    count = 0;
    }

    List<TagStats> tags = new ArrayList<>();

    void dumpTags(PrintWriter pw, int minCount, boolean addSubTags) {
        for (String object : Tags.OBJECT_KEYS) {
            String filter = Tags.KEYS_FOR_SPECIFIC_ELEMENT.get(object); // normally == null == all elements
            List<ValueAndDescription> values = TagInfo.getOptionsFromTagInfo(object, filter, false, minCount, 0, false);
            List<ValueAndDescription> combinationsList = TagInfo.getCombinationKeys(object, filter, minCount / 5);
            Set<String> combinations = new HashSet<>();
            if (combinationsList != null) {
                for (ValueAndDescription combination : combinationsList) {
                    combinations.add(combination.value);
                }
            }
            if (values != null && !values.isEmpty()) {
                for (ValueAndDescription value : values) {
                    if (Tags.NOT_OBJECT_KEY_VALUES.contains(value.value)) { // applies to top level keys too
                        continue;
                    }
                    TagStats stats = new TagStats();
                    stats.tag = object + "=" + value.value;
                    tags.add(stats);
                    stats.count = value.count;

                    // handle sub keys
                    String subKey = value.value;
                    if (Tags.SECOND_LEVEL_KEYS.containsKey(value.value)) {
                        subKey = Tags.SECOND_LEVEL_KEYS.get(value.value);
                    }
                    if (!combinations.contains(subKey)) { // do this after replacing subKey
                        System.out.println(subKey + " discarded because not in combinations for key " + object);
                        continue;
                    }
                    boolean not2ndLevelKeysHasTag = Tags.NOT_SECOND_LEVEL_KEYS_2.containsKey(stats.tag);
                    if (Tags.NOT_SECOND_LEVEL_KEYS.contains(subKey) || Tags.OBJECT_KEYS.contains(subKey)
                            || (not2ndLevelKeysHasTag && Tags.NOT_SECOND_LEVEL_KEYS_2.get(stats.tag).contains(subKey))) {
                        System.out.println(subKey + " discarded because of manual discard, key " + object);
                        continue;
                    }
                    if (addSubTags) {
                        List<ValueAndDescription> subValues = TagInfo.getOptionsFromTagInfo(subKey, filter, false, minCount / 5, 0, false);
                        if (values != null) {
                            for (ValueAndDescription sub : subValues) {
                                if (Tags.LIFECYCLE_KEYS.contains(subKey) && !values.contains(sub)) {
                                    // the sub value may only be one of the top level values for this key
                                    // example highway=construction, construction=primary
                                    continue;
                                }
                                String subTag = subKey + "=" + sub.value;
                                if (Tags.NOT_OBJECT_KEY_VALUES.contains(sub.value)
                                        || (not2ndLevelKeysHasTag && Tags.NOT_SECOND_LEVEL_KEYS_2.get(stats.tag).contains(subTag))) {
                                    continue;
                                }
                                stats = new TagStats();
                                stats.tag = object + "=" + value.value + " / " + subTag;
                                stats.count = Integer.min(sub.count, value.count);
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
            pw.print(s.tag + "," + s.count + "\n");
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
