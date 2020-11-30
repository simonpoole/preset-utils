package ch.poole.osm.presetutils;

import static ch.poole.osm.presetutils.PresetConstants.CHECK_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.CHUNK;
import static ch.poole.osm.presetutils.PresetConstants.COMBO_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.GROUP;
import static ch.poole.osm.presetutils.PresetConstants.ICON;
import static ch.poole.osm.presetutils.PresetConstants.ID;
import static ch.poole.osm.presetutils.PresetConstants.ITEM;
import static ch.poole.osm.presetutils.PresetConstants.KEY_ATTR;
import static ch.poole.osm.presetutils.PresetConstants.KEY_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.LABEL;
import static ch.poole.osm.presetutils.PresetConstants.LIST_ENTRY;
import static ch.poole.osm.presetutils.PresetConstants.MULTISELECT_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.NAME;
import static ch.poole.osm.presetutils.PresetConstants.OPTIONAL;
import static ch.poole.osm.presetutils.PresetConstants.PRESETS;
import static ch.poole.osm.presetutils.PresetConstants.PRESET_LINK;
import static ch.poole.osm.presetutils.PresetConstants.REFERENCE;
import static ch.poole.osm.presetutils.PresetConstants.ROLE;
import static ch.poole.osm.presetutils.PresetConstants.SEPARATOR;
import static ch.poole.osm.presetutils.PresetConstants.TEXT_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.VALUE;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * Generate a list of icons in use in the preset
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class ListIcons {

    private static final String OUTPUT_OPTION     = "output";
    private static final String INPUT_OPTION      = "input";
    private static final String MAP_OUTPUT_OPTION = "map";

    private static final Pattern vespucciIconPattern1 = Pattern.compile("^\\$\\{ICONPATH\\}(.+)(_[0-9]+)\\.\\$\\{ICONTYPE\\}$");
    private static final Pattern vespucciIconPattern2 = Pattern.compile("^\\$\\{ICONPATH\\}(.+)\\.\\$\\{ICONTYPE\\}$");
    private static final Pattern josmIconPattern      = Pattern.compile("^(.*\\/)(.+)\\.(.+)$");

    HashMap<String, MultiHashMap<String, String>> msgs = new HashMap<>();

    String inputFilename;
    int    groupCount = 0;

    void parseXML(@NotNull final InputStream input, @NotNull final PrintWriter pw, @Nullable final PrintWriter mapOutput) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        saxParser.parse(input, new DefaultHandler() {

            String group     = null;
            String preset    = null;
            String chunk     = null;
            String valueIcon = null;
            String key       = null;

            private Deque<String> groupstack = new ArrayDeque<>();

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                switch (name) {
                case PRESETS:
                    break;
                case GROUP:
                    groupCount++;
                    group = attr.getValue(NAME);
                    groupstack.push(group);
                    writeEntry(pw, mapOutput, groupPath(), attr.getValue(ICON));
                    break;
                case ITEM:
                    preset = attr.getValue(NAME);
                    writeEntry(pw, mapOutput, groupPath() + preset, attr.getValue(ICON));
                    break;
                case CHUNK:
                    chunk = attr.getValue(ID);
                    break;
                case SEPARATOR:
                case LABEL:
                case OPTIONAL:
                case PRESET_LINK:
                case ROLE:
                case KEY_FIELD:
                case TEXT_FIELD:
                case REFERENCE:
                    break;
                case MULTISELECT_FIELD:
                case COMBO_FIELD:
                    key = attr.getValue(KEY_ATTR);
                    break;
                case CHECK_FIELD:
                case LIST_ENTRY:
                    valueIcon = attr.getValue(ICON);
                    String value = attr.getValue(VALUE);
                    if (valueIcon != null && !"".equals(valueIcon)) {
                        writeEntry(pw, mapOutput, groupPath() + (preset != null ? preset : chunk) + "|" + key + (LIST_ENTRY.equals(name) ? "|" + value : ""), valueIcon);
                    }
                    break;
                default:
                    System.out.println("Unknown start tag " + name);
                }
            }

            /**
             * Generate a list entry
             * 
             * @param pw the PRintWriter to use
             * @param mapOutput optional output for mapping info
             * @param path the "path" inside the preset
             * @param icon the icon file location/name
             */
            private void writeEntry(@NotNull PrintWriter pw, @Nullable PrintWriter mapOutput, @NotNull String path, @Nullable String icon) {
                String tag = "";
                if (icon != null) {
                    Matcher matcher = vespucciIconPattern1.matcher(icon);
                    if (matcher.matches()) {
                        tag = matcher.group(1);
                    } else {
                        matcher = vespucciIconPattern2.matcher(icon);
                        if (matcher.matches()) {
                            tag = matcher.group(1);
                        } else {
                            matcher = josmIconPattern.matcher(icon);
                            if (matcher.matches()) {
                                tag = matcher.group(2);
                            }
                        }
                    }
                }
                pw.println(path + "\t" + tag + "\t" + (icon != null ? icon : ""));
                if (mapOutput != null) {
                    mapOutput.println(tag + "=" + (icon != null ? icon : ""));
                }
            }

            @Override
            public void endElement(String uri, String localMame, String name) throws SAXException {
                switch (name) {
                case GROUP:
                    group = null;
                    groupstack.pop();
                    groupCount--;
                    break;
                case KEY_FIELD:
                case REFERENCE:
                case OPTIONAL:
                case LIST_ENTRY:
                case TEXT_FIELD:
                    break;
                case CHUNK:
                    chunk = null;
                    break;
                case ITEM:
                    preset = null;
                    break;
                case COMBO_FIELD:
                case MULTISELECT_FIELD:
                    key = null;
                    break;
                default:
                    System.out.println("Unknown end tag " + name);
                }
            }

            @NotNull
            private String groupPath() {
                StringBuilder builder = new StringBuilder();

                for (String g : groupstack) {
                    builder.append(g);
                    builder.append("|");
                }

                return builder.toString();
            }

            @Override
            public void endDocument() {
                pw.flush();
                if (mapOutput != null) {
                    mapOutput.flush();
                }
            }

        });
    }

    private void setInputFilename(String fn) {
        inputFilename = fn;
    }

    public static void main(String[] args) {
        // defaults
        InputStream is = System.in;
        OutputStream os = System.out;
        OutputStream mapOutput = null;
        ListIcons p = new ListIcons();
        p.setInputFilename("stdin");

        // arguments
        Option inputFile = Option.builder("i").longOpt(INPUT_OPTION).hasArg().desc("input preset file, default: standard in").build();
        Option outputFile = Option.builder("o").longOpt(OUTPUT_OPTION).hasArg().desc("output file, default: standard out").build();
        Option mapOutputFile = Option.builder("m").longOpt(MAP_OUTPUT_OPTION).hasArg().desc("map output file, default: none").build();

        Options options = new Options();

        options.addOption(inputFile);
        options.addOption(outputFile);
        options.addOption(mapOutputFile);

        CommandLineParser parser = new DefaultParser();
        try {
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT_OPTION)) {
                    // initialise the member variable
                    String input = line.getOptionValue(INPUT_OPTION);
                    p.setInputFilename(input);
                    is = new FileInputStream(input);
                }
                if (line.hasOption(OUTPUT_OPTION)) {
                    String output = line.getOptionValue(OUTPUT_OPTION);
                    os = new FileOutputStream(output);
                }
                if (line.hasOption(MAP_OUTPUT_OPTION)) {
                    String output = line.getOptionValue(MAP_OUTPUT_OPTION);
                    mapOutput = new FileOutputStream(output);
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ListIcons", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            try {
                p.parseXML(is, new PrintWriter(os), mapOutput != null ? new PrintWriter(mapOutput) : null);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // NOSONAR
            }
            try {
                os.close();
            } catch (IOException e) {
                // NOSONAR
            }
            if (mapOutput != null) {
                try {
                    mapOutput.close();
                } catch (IOException e) {
                    // NOSONAR
                }
            }
        }
    }
}
