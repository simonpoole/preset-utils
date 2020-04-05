package ch.poole.osm.presetutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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

public class PresetStats {

    private static final String TAGINFO = "taginfo";
    private static final String INPUT   = "input";
    private static final String OUTPUT  = "output";

    class ItemStats {
        String                    tag        = null;
        String                    name       = null;
        int                       keyCount   = 0;
        int                       valueCount = 0;
        int                       count      = 0;
        Map<String, List<String>> chunkTags  = null;
        boolean                   isChunk    = false;
    }

    Map<String, ItemStats> items        = new HashMap<>();
    Set<String>            uniqueKeys   = new HashSet<>();
    Set<String>            uniqueValues = new HashSet<>();
    String                 inputFilename;
    MyHandler              handler;

    class MyHandler extends DefaultHandler {
        private static final String GROUP              = "group";
        private static final String ITEM               = "item";
        private static final String NAME               = "name";
        private static final String CHUNK              = "chunk";
        private static final String ID                 = "id";
        private static final String SEPARATOR          = "separator";
        private static final String LABEL              = "label";
        private static final String OPTIONAL           = "optional";
        private static final String TEXT               = "text";
        private static final String LINK               = "link";
        private static final String CHECK              = "check";
        private static final String COMBO              = "combo";
        private static final String DELIMITER          = "delimiter";
        private static final String VALUES             = "values";
        private static final String KEY                = "key";
        private static final String MULTISELECT        = "multiselect";
        private static final String ROLE               = "role";
        private static final String REFERENCE          = "reference";
        private static final String REF                = "ref";
        private static final String LIST_ENTRY         = "list_entry";
        private static final String VALUE              = "value";
        ItemStats                   current            = null;
        boolean                     keySeen            = false;
        boolean                     secondLevelKeySeen = false;
        Map<String, ItemStats>      expandedItems      = null;
        String                      tagKey             = null;
        String                      tagValue           = null;
        Map<String, ItemStats>      chunks             = new HashMap<>();

        String  comboKey;
        boolean inOptional  = false;
        boolean expandCombo = false;

        final boolean useTagInfo;

        public MyHandler(boolean useTagInfo) {
            this.useTagInfo = useTagInfo;
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (inOptional) {
                return;
            }
            if (GROUP.equals(qName)) {
                String group = attr.getValue(NAME);
            } else if (ITEM.equals(qName)) {
                keySeen = false;
                expandedItems = null;
                tagKey = null;
                tagValue = null;
                inOptional = false;
                expandCombo = false;
                current = new ItemStats();
                current.name = attr.getValue(NAME);
            } else if (CHUNK.equals(qName)) {
                current = new ItemStats();
                current.name = attr.getValue(ID);
                current.isChunk = true;
            } else if (SEPARATOR.equals(qName)) {
            } else if (LABEL.equals(qName)) {
            } else if (OPTIONAL.equals(qName)) {
                inOptional = false; // currently doesn't make sense
            } else if (KEY.equals(qName)) {
                String key = attr.getValue(KEY);
                String value = attr.getValue(VALUE);
                uniqueKeys.add(key);
                uniqueValues.add(value);
                boolean isObjectKey = Tags.OBJECT_KEYS.contains(key);
                if (isObjectKey && Tags.NOT_OBJECT_KEY_VALUES.contains(value)) {
                    return; // these tend to be nonsense
                }
                if (current.tag == null) {
                    if (!keySeen) {
                        tagKey = key;
                        tagValue = value;
                    }
                    current.tag = key + "=" + value;
                } else {
                    if (isObjectKey && !Tags.OBJECT_KEYS.contains(tagKey)) {
                        current.tag = key + "=" + value + " / " + current.tag;
                        tagKey = key;
                        tagValue = value;
                    } else if (isObjectKey && Tags.OBJECT_KEYS.contains(tagKey)) {
                        // both keys are object keys, sort alphabetically
                        if (tagKey.compareTo(key) > 0) {
                            current.tag = key + "=" + value + " / " + current.tag;
                            tagKey = key;
                            tagValue = value;
                        } else {
                            current.tag = current.tag + " / " + key + "=" + value;
                        }
                    } else {
                        current.tag = current.tag + " / " + key + "=" + value;
                    }
                }
                keySeen = true;
                secondLevelKeySeen = key.equals(tagValue) || key.equals(Tags.SECOND_LEVEL_KEYS.get(key));
                current.keyCount++;
                current.valueCount++;
                if (useTagInfo) {
                    int tagInfoCount = TagInfo.getTagCount(key, value);
                    current.count = current.count > 0 ? Integer.min(current.count, tagInfoCount) : tagInfoCount;
                }
            } else if (TEXT.equals(qName)) {
                String key = attr.getValue(KEY);
                uniqueKeys.add(key);
                if (expandedItems != null) {
                    for (ItemStats s : expandedItems.values()) {
                        s.keyCount++;
                    }
                } else {
                    current.keyCount++;
                }
            } else if (LINK.equals(qName)) {
            } else if (CHECK.equals(qName)) {
                String key = attr.getValue(KEY);
                uniqueKeys.add(key);
                if (expandedItems != null) {
                    for (ItemStats s : expandedItems.values()) {
                        s.keyCount++;
                    }
                } else {
                    current.keyCount++;
                }
            } else if (COMBO.equals(qName) || MULTISELECT.equals(qName)) {
                String key = attr.getValue(KEY);
                comboKey = key;
                uniqueKeys.add(key);
                current.keyCount++;
                String delimiter = attr.getValue(DELIMITER);
                String valuesString = attr.getValue(VALUES);
                expandedItems = null;
                expandCombo = !secondLevelKeySeen && keySeen && (comboKey.equals(tagValue) || comboKey.equals(Tags.SECOND_LEVEL_KEYS.get(tagValue)));
                if ((!keySeen && Tags.OBJECT_KEYS.contains(comboKey)) || expandCombo) {
                    expandedItems = new HashMap<>(); // this double as a flag if the combo should be considered at all
                }
                if (valuesString != null) {
                    String[] values = valuesString.split(delimiter != null ? delimiter : (MULTISELECT.equals(qName) ? ";" : ","));
                    if (expandedItems != null) {
                        for (String v : values) {
                            if (!Tags.NOT_OBJECT_KEY_VALUES.contains(v)) {
                                ItemStats s = new ItemStats();
                                s.name = comboKey + "=" + v;
                                s.tag = s.name;
                                if (expandCombo) {
                                    s.tag = tagKey + "=" + tagValue + " / " + s.tag;
                                }
                                expandedItems.put(s.tag, s);
                                if (useTagInfo) {
                                    int tagInfoCount = TagInfo.getTagCount(key, v);
                                    s.count = current.count > 0 ? Integer.min(current.count, tagInfoCount) : tagInfoCount;
                                }
                            }
                        }
                    } else {
                        current.valueCount += values.length;
                    }
                    if (current.isChunk) {
                        if (current.chunkTags == null) {
                            current.chunkTags = new HashMap<>();
                        }
                        current.chunkTags.put(key, Arrays.asList(values));
                    }
                    uniqueValues.addAll(Arrays.asList(values));
                }
            } else if (ROLE.equals(qName)) {
            } else if (REFERENCE.equals(qName)) {
                ItemStats chunk = chunks.get(attr.getValue(REF));
                String subKey = tagValue;
                if (chunk != null) {
                    if (keySeen && expandedItems == null && chunk.chunkTags != null) {
                        List<String> chunkValues = chunk.chunkTags.get(tagValue);
                        if (chunkValues == null) {
                            String comboKey = Tags.SECOND_LEVEL_KEYS.get(tagValue);
                            chunkValues = chunk.chunkTags.get(comboKey);
                            if (chunkValues != null) {
                                subKey = comboKey;
                            }
                        }
                        if (chunkValues != null) {
                            expandedItems = new HashMap<>();
                            for (String v : chunkValues) {
                                ItemStats s = new ItemStats();
                                s.name = subKey + "=" + v;
                                s.tag = s.name;
                                s.tag = tagKey + "=" + tagValue + " / " + s.tag;
                                expandedItems.put(s.tag, s);
                                if (useTagInfo) {
                                    int tagInfoCount = TagInfo.getTagCount(tagValue, v);
                                    s.count = current.count > 0 ? Integer.min(current.count, tagInfoCount) : tagInfoCount;
                                }
                            }
                            items.putAll(expandedItems);
                            secondLevelKeySeen = true;
                        }
                    }
                    if (expandedItems != null) {
                        for (ItemStats s : expandedItems.values()) {
                            s.keyCount += chunk.keyCount;
                            s.valueCount += chunk.valueCount;
                        }
                    } else {
                        if (chunk.tag != null) {
                            String[] c = chunk.tag.split("=");
                            if (Tags.OBJECT_KEYS.contains(c[0])) { // hack alert
                                if (current.tag != null) {
                                    current.tag = chunk.tag + " / " + current.tag;
                                } else {
                                    current.tag = chunk.tag;
                                }
                            }
                        }
                        current.keyCount += chunk.keyCount;
                        current.valueCount += chunk.valueCount;
                    }
                }
            } else if (LIST_ENTRY.equals(qName)) {
                String value = attr.getValue(VALUE);
                if (expandedItems != null) {
                    ItemStats s = new ItemStats();
                    s.name = comboKey + "=" + value;
                    s.tag = s.name;
                    if (expandCombo) {
                        s.tag = tagKey + "=" + tagValue + " / " + s.tag;
                    }
                    expandedItems.put(s.name, s);
                    if (useTagInfo) {
                        int tagInfoCount = TagInfo.getTagCount(comboKey, value);
                        s.count = current.count > 0 ? Integer.min(current.count, tagInfoCount) : tagInfoCount;
                    }
                } else {
                    current.valueCount++;
                }
                if (current.isChunk) {
                    if (current.chunkTags == null) {
                        current.chunkTags = new HashMap<>();
                    }
                    List<String> l = current.chunkTags.get(comboKey);
                    if (l == null) {
                        l = new ArrayList<>();
                        current.chunkTags.put(comboKey, l);
                    }
                    l.add(value);
                }
                uniqueValues.add(value);
            } else if ("preset_link".equals(qName)) {
            }
        }

        @Override
        public void endElement(String uri, String localMame, String qName) throws SAXException {
            if (GROUP.equals(qName)) {
            } else if (OPTIONAL.equals(qName)) {
                inOptional = false;
            } else if (!inOptional) {
                if (ITEM.equals(qName)) {
                    items.put(current.tag, current);
                    current = null;
                    expandedItems = null;
                } else if (CHUNK.equals(qName)) {
                    chunks.put(current.name, current);
                    current = null;
                    expandedItems = null;
                } else if (COMBO.equals(qName) || MULTISELECT.equals(qName)) {
                    if (expandedItems != null) {
                        items.putAll(expandedItems);
                    }
                    if (expandCombo) {
                        secondLevelKeySeen = true;
                    }
                    comboKey = null;
                }
            }
        }
    }

    void parseXML(boolean useTagInfo, InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        handler = new MyHandler(useTagInfo);

        saxParser.parse(input, handler);
    }

    void dumpStats(PrintWriter pw) {
        int itemCount = items.size();
        int keyCount = 0;
        int valueCount = 0;
        for (ItemStats s : items.values()) {
            keyCount += s.keyCount;
            valueCount += s.valueCount;
            pw.print(s.tag + "," + s.count + "\n");
        }
        pw.flush();
        // print stats to standard out
        System.out.print("Total items " + itemCount + "\n");
        System.out.print("Unique keys " + uniqueKeys.size() + "\n");
        System.out.print("Unique values " + uniqueValues.size() + "\n");
        System.out.print("Total key count " + keyCount + "\n");
        System.out.print("Total value count " + valueCount + "\n");
        System.out.print("Keys per item " + keyCount / itemCount + "\n");
        System.out.print("Values per item " + valueCount / itemCount + "\n");
        System.out.flush();

    }

    private void setInputFilename(String fn) {
        inputFilename = fn;
    }

    public static void main(String[] args) {
        // defaults
        InputStream is = System.in;
        OutputStreamWriter os = null;
        boolean useTagInfo = false;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            PresetStats p = new PresetStats();
            p.setInputFilename("stdin");

            // arguments
            Option inputFile = Option.builder("i").longOpt(INPUT).hasArg().desc("input preset file, default: standard in").build();

            Option outputFile = Option.builder("o").longOpt(OUTPUT).hasArg().desc("output stats file, default: standard out").build();

            Option tagInfo = Option.builder("t").longOpt(TAGINFO).desc("query taginfo for stats, default: false").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(outputFile);
            options.addOption(tagInfo);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT)) {
                    // initialise the member variable
                    String input = line.getOptionValue(INPUT);
                    p.setInputFilename(input);
                    is = new FileInputStream(input);
                }
                if (line.hasOption(OUTPUT)) {
                    String output = line.getOptionValue(OUTPUT);
                    os = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
                }
                useTagInfo = line.hasOption(TAGINFO);
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("PresetStats", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            try {
                p.parseXML(useTagInfo, is);
                p.dumpStats(new PrintWriter(os));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
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
