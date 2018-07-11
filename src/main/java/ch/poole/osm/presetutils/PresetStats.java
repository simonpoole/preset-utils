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
import java.util.Collections;
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
 * Some parts of this were nicked from Vespucci and some from Apache CLI sample code.
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class PresetStats {

    /**
     * An set of tags considered 'important'. These are typically tags that define real-world objects and not properties
     * of such.
     */
    public static final Set<String> OBJECT_KEYS       = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("highway", "barrier", "waterway", "railway", "aeroway", "aerialway", "power", "man_made", "building", "leisure", "amenity", "office",
                    "shop", "craft", "emergency", "tourism", "historic", "landuse", "military", "natural", "boundary", "place", "type", "entrance", "pipeline",
                    "healthcare", "playground", "attraction", "public_transport", "traffic_sign", "traffic_sign:forward", "traffic_sign:backward", "golf")));
    public static final Set<String> SECOND_LEVEL_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("vending")));

    class ItemStats {
        String tag;
        String name;
        int    keyCount   = 0;
        int    valueCount = 0;
    }

    Map<String, ItemStats> items        = new HashMap<>();
    Set<String>            uniqueKeys   = new HashSet<>();
    Set<String>            uniqueValues = new HashSet<>();
    String                 inputFilename;
    MyHandler              handler;

    class MyHandler extends DefaultHandler {
        private static final String GROUP         = "group";
        private static final String ITEM          = "item";
        private static final String NAME          = "name";
        private static final String CHUNK         = "chunk";
        private static final String ID            = "id";
        private static final String SEPARATOR     = "separator";
        private static final String LABEL         = "label";
        private static final String OPTIONAL      = "optional";
        private static final String TEXT          = "text";
        private static final String LINK          = "link";
        private static final String CHECK         = "check";
        private static final String COMBO         = "combo";
        private static final String DELIMITER2    = "delimiter";
        private static final String VALUES        = "values";
        private static final String KEY           = "key";
        private static final String MULTISELECT   = "multiselect";
        private static final String ROLE          = "role";
        private static final String REFERENCE     = "reference";
        private static final String REF           = "ref";
        private static final String LIST_ENTRY    = "list_entry";
        private static final String VALUE         = "value";
        ItemStats                   current       = null;
        boolean                     keySeen       = false;
        Map<String, ItemStats>      expandedItems = null;
        String                      tagKey        = null;
        String                      tagValue      = null;
        Map<String, ItemStats>      chunks        = new HashMap<>();

        String comboKey;

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (GROUP.equals(qName)) {
                String group = attr.getValue(NAME);
            } else if (ITEM.equals(qName)) {
                keySeen = false;
                expandedItems = null;
                tagKey = null;
                tagValue = null;
                current = new ItemStats();
                current.name = attr.getValue(NAME);
            } else if (CHUNK.equals(qName)) {
                current = new ItemStats();
                current.name = attr.getValue(ID);
            } else if (SEPARATOR.equals(qName)) {
            } else if (LABEL.equals(qName)) {
            } else if (OPTIONAL.equals(qName)) {
            } else if (KEY.equals(qName)) {
                String key = attr.getValue(KEY);
                String value = attr.getValue(VALUE);
                uniqueKeys.add(key);
                uniqueValues.add(value);
                if (current.tag == null) {
                    if (!keySeen) {
                        tagKey = key;
                        tagValue = value;
                    }
                    current.tag = key + "=" + value;
                } else {
                    current.tag = current.tag + " / " + key + "=" + value;
                }
                keySeen = true;
                current.keyCount++;
                current.valueCount++;
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
                uniqueKeys.add(key);
                current.keyCount++;
                String delimiter = attr.getValue(DELIMITER2);
                String valuesString = attr.getValue(VALUES);
                comboKey = attr.getValue(KEY);
                expandedItems = null;
                boolean expandCombo = comboKey.equals(tagValue) || SECOND_LEVEL_KEYS.contains(comboKey);
                if ((!keySeen && OBJECT_KEYS.contains(comboKey)) || expandCombo) {
                    expandedItems = new HashMap<>();
                }
                if (valuesString != null) {
                    String[] values = valuesString.split(delimiter != null ? delimiter : (MULTISELECT.equals(qName) ? ";" : ","));
                    if (expandedItems != null) {
                        for (String v : values) {
                            ItemStats s = new ItemStats();
                            s.name = comboKey + "=" + v;
                            s.tag = s.name;
                            if (expandCombo) {
                                s.tag = tagKey + "=" + tagValue + " / " + s.tag;
                            }
                            expandedItems.put(s.name, s);
                        }
                    } else {
                        current.valueCount += values.length;
                    }
                    uniqueValues.addAll(Arrays.asList(values));
                }
            } else if (ROLE.equals(qName)) {
            } else if (REFERENCE.equals(qName)) {
                ItemStats chunk = chunks.get(attr.getValue(REF));
                if (chunk != null) {
                    if (expandedItems != null) {
                        for (ItemStats s : expandedItems.values()) {
                            s.keyCount += chunk.keyCount;
                            s.valueCount += chunk.valueCount;
                        }
                    } else {
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
                    expandedItems.put(s.name, s);
                } else {
                    current.valueCount++;
                }
                uniqueValues.add(value);
            } else if ("preset_link".equals(qName)) {
            }
        }

        @Override
        public void endElement(String uri, String localMame, String qName) throws SAXException {
            if (GROUP.equals(qName)) {
            } else if (OPTIONAL.equals(qName)) {
            } else if (ITEM.equals(qName)) {
                if (expandedItems != null) {
                    items.putAll(expandedItems);
                } else {
                    items.put(current.tag, current);
                }
                current = null;
                expandedItems = null;
            } else if (CHUNK.equals(qName)) {
                chunks.put(current.name, current);
                current = null;
                expandedItems = null;
            } else if (COMBO.equals(qName) || MULTISELECT.equals(qName)) {
                comboKey = null;
            }
        }
    }

    void parseXML(InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        handler = new MyHandler();

        saxParser.parse(input, handler);
    }

    void dumpStats(PrintWriter pw) {
        int itemCount = items.size();
        int keyCount = 0;
        int valueCount = 0;
        for (ItemStats s : items.values()) {
            keyCount += s.keyCount;
            valueCount += s.valueCount;
            pw.print(s.tag + "\n");
        }
        pw.print("\n");
        // print a header
        pw.print("Total items " + itemCount + "\n");
        pw.print("Unique keys " + uniqueKeys.size() + "\n");
        pw.print("Unique values " + uniqueValues.size() + "\n");
        pw.print("Total key count " + keyCount + "\n");
        pw.print("Total value count " + valueCount + "\n");
        pw.print("Keys per item " + keyCount / itemCount + "\n");
        pw.print("Values per item " + valueCount / itemCount + "\n");

        pw.print("\n");
        pw.flush();
    }

    private void setInputFilename(String fn) {
        inputFilename = fn;
    }

    public static void main(String[] args) {
        // defaults
        InputStream is = System.in;
        OutputStreamWriter os = null;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            PresetStats p = new PresetStats();
            p.setInputFilename("stdin");

            // arguments
            Option inputFile = Option.builder("i").longOpt("input").hasArg().desc("input preset file, default: standard in").build();

            Option outputFile = Option.builder("o").longOpt("output").hasArg().desc("output stats file, default: standard out").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(outputFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption("input")) {
                    // initialise the member variable
                    String input = line.getOptionValue("input");
                    p.setInputFilename(input);
                    is = new FileInputStream(input);
                }
                if (line.hasOption("output")) {
                    String output = line.getOptionValue("output");
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

            try {
                p.parseXML(is);
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
