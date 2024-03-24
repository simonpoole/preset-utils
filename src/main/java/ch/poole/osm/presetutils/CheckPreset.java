package ch.poole.osm.presetutils;

import static ch.poole.osm.presetutils.PresetConstants.CHECKGROUP;
import static ch.poole.osm.presetutils.PresetConstants.CHECK_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.CHUNK;
import static ch.poole.osm.presetutils.PresetConstants.COMBO_DELIMITER;
import static ch.poole.osm.presetutils.PresetConstants.COMBO_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.DELIMITER;
import static ch.poole.osm.presetutils.PresetConstants.DISPLAY_VALUES;
import static ch.poole.osm.presetutils.PresetConstants.GROUP;
import static ch.poole.osm.presetutils.PresetConstants.ID;
import static ch.poole.osm.presetutils.PresetConstants.ITEM;
import static ch.poole.osm.presetutils.PresetConstants.KEY_ATTR;
import static ch.poole.osm.presetutils.PresetConstants.KEY_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.LABEL;
import static ch.poole.osm.presetutils.PresetConstants.LINK;
import static ch.poole.osm.presetutils.PresetConstants.LIST_ENTRY;
import static ch.poole.osm.presetutils.PresetConstants.MULTISELECT_DELIMITER;
import static ch.poole.osm.presetutils.PresetConstants.MULTISELECT_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.NAME;
import static ch.poole.osm.presetutils.PresetConstants.OPTIONAL;
import static ch.poole.osm.presetutils.PresetConstants.PRESETS;
import static ch.poole.osm.presetutils.PresetConstants.PRESET_LINK;
import static ch.poole.osm.presetutils.PresetConstants.REF;
import static ch.poole.osm.presetutils.PresetConstants.REFERENCE;
import static ch.poole.osm.presetutils.PresetConstants.ROLE;
import static ch.poole.osm.presetutils.PresetConstants.ROLES;
import static ch.poole.osm.presetutils.PresetConstants.SEPARATOR;
import static ch.poole.osm.presetutils.PresetConstants.SPACE;
import static ch.poole.osm.presetutils.PresetConstants.TEXT_FIELD;
import static ch.poole.osm.presetutils.PresetConstants.VALUE_TYPE;
import static ch.poole.osm.presetutils.PresetConstants.VALUES;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Perform some checks that are difficult to do in xmlmns
 * 
 * Check performed
 * 
 * - ensure that display_values attributes are consistent with the values - check that chunks are defined before they
 * are referenced
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class CheckPreset {

    private static final String INPUT_OPTION = "input";

    private static final Logger LOGGER = Logger.getLogger(CheckPreset.class.getName());

    private static final List<String> NO_DISPLAY_VALUES = Arrays.asList("opening_hours", "dimension_horizontal", "dimension_vertical", "integer");

    String      inputFilename;
    int         groupCount = 0;
    Set<String> chunks     = new HashSet<>();
    boolean     error      = false;

    void parseXML(@NotNull final InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        saxParser.parse(input, new DefaultHandler() {

            private Locator locator;

            @Override
            public void setDocumentLocator(Locator locator) {
                this.locator = locator;
            }

            String group  = null;
            String preset = null;
            String chunk  = null;
            String key    = null;

            private Deque<String> groupstack = new ArrayDeque<>();

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                String line = "?";
                if (locator != null) {
                    line = Integer.toString(locator.getLineNumber());
                }
                switch (name) {
                case PRESETS:
                    break;
                case GROUP:
                    groupCount++;
                    group = attr.getValue(NAME);
                    groupstack.push(group);
                    break;
                case ITEM:
                    preset = attr.getValue(NAME);
                    break;
                case CHUNK:
                    chunk = attr.getValue(ID);
                    chunks.add(chunk);
                    break;
                case SEPARATOR:
                case LABEL:
                case OPTIONAL:
                case PRESET_LINK:
                case ROLE:
                case ROLES:
                case KEY_FIELD:
                case TEXT_FIELD:
                case CHECKGROUP:
                case LINK:
                case SPACE:
                    break;
                case REFERENCE:
                    String ref = attr.getValue(REF);
                    if (!chunks.contains(ref)) {
                        LOGGER.log(Level.SEVERE, "Line {0}: chunk \"{1}\" referenced before defined", new Object[] { line, ref });
                        error = true;
                    }
                    break;
                case MULTISELECT_FIELD:
                case COMBO_FIELD:
                    key = attr.getValue(KEY_ATTR);
                    boolean multiselect = MULTISELECT_FIELD.equals(name);
                    String delimiter = attr.getValue(DELIMITER);
                    if (delimiter == null) {
                        delimiter = multiselect ? MULTISELECT_DELIMITER : COMBO_DELIMITER;
                    }
                    String values = attr.getValue(VALUES);
                    String displayValues = attr.getValue(DISPLAY_VALUES);
                    if (values != null) {
                        if (displayValues == null) {
                            if (!NO_DISPLAY_VALUES.contains(attr.getValue(VALUE_TYPE))) {
                                LOGGER.log(Level.INFO, "Line {0}: missing display_values for item \"{1}\" key {2}",
                                        new Object[] { line, preset != null ? preset : chunk, key });
                            }
                            break;
                        }
                        int displayValuesCount = displayValues.split("\\" + delimiter).length;
                        int valuesCount = values.split("\\" + delimiter).length;
                        if (valuesCount != displayValuesCount || valuesCount == 0) {
                            LOGGER.log(Level.SEVERE, "Line {0}: inconsistent display_values for item \"{1}\" key {2}",
                                    new Object[] { line, preset != null ? preset : chunk, key });
                            error = true;
                        }
                    }
                    break;
                case CHECK_FIELD:
                case LIST_ENTRY:
                    break;
                default:
                    LOGGER.log(Level.INFO, "Line {0}: unknown start tag \"{1}\"", new Object[] { line, name });
                }
            }

            @Override
            public void endElement(String uri, String localMame, String name) throws SAXException {
                String line = "?";
                if (locator != null) {
                    line = Integer.toString(locator.getLineNumber());
                }
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
                case LINK:
                case SPACE:
                case CHECK_FIELD:
                case CHECKGROUP:
                case PRESET_LINK:
                case ROLE:
                case ROLES:
                case LABEL:
                case SEPARATOR:
                case PRESETS:
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
                    LOGGER.log(Level.INFO, "Line {0}: unknown end tag \"{1}\"", new Object[] { line, name });
                }
            }

        });
    }

    private void setInputFilename(String fn) {
        inputFilename = fn;
    }

    public static void main(String[] args) {
        // set up logging
        LogManager.getLogManager().reset();
        SimpleFormatter fmt = new SimpleFormatter();
        Handler stdoutHandler = new FlushStreamHandler(System.out, fmt); // NOSONAR
        stdoutHandler.setLevel(Level.INFO);
        LOGGER.addHandler(stdoutHandler);
        Handler stderrHandler = new FlushStreamHandler(System.err, fmt); // NOSONAR
        stderrHandler.setLevel(Level.WARNING);
        LOGGER.addHandler(stderrHandler);

        // defaults
        InputStream is = System.in;
        CheckPreset p = new CheckPreset();
        p.setInputFilename("stdin");

        // arguments
        Option inputFile = Option.builder("i").longOpt(INPUT_OPTION).hasArg().desc("input preset file, default: standard in").build();

        Options options = new Options();

        options.addOption(inputFile);

        CommandLineParser parser = new DefaultParser();
        try {
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT_OPTION)) {
                    String input = line.getOptionValue(INPUT_OPTION);
                    p.setInputFilename(input);
                    is = new FileInputStream(input);
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(CheckPreset.class.getSimpleName(), options);
                return;
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.SEVERE, "File not found \"{0}\"", new Object[] { e.getMessage() });
                return;
            }

            try {
                p.parseXML(is);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // NOSONAR
            }
            if (p.error) {
                System.exit(1);
            }
        }
    }
}
