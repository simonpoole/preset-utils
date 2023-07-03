package ch.poole.osm.presetutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
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
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Small utility to extract strings from a JOSM style preset file that should be translated, error handling is
 * essentially crashing and burning when something goes wrong. Some parts of this were nicked from Vespucci and some
 * from Apache CLI sample code.
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class Preset2Pot {

    private static final String OUTPUT_OPT   = "output";
    private static final String INPUT_OPT    = "input";
    private static final String EMPTY_MSGSTR = "msgstr \"\"\n";

    LinkedHashMap<String, MultiHashMap<String, String>> msgs = new LinkedHashMap<>();

    String    inputFilename;
    MyHandler handler;

    @Nullable
    public Locator getLocator() {
        return handler.locator;
    }

    class MyHandler extends DefaultHandler {

        private static final String SHORT_DESCRIPTIONS            = "short_descriptions";
        private static final String DISPLAY_VALUES                = "display_values";
        private static final String VALUE_TEMPLATE                = "value_template";
        private static final String NAME_TEMPLATE                 = "name_template";
        private static final String PRESET_LINK                   = "preset_link";
        private static final String LIST_ENTRY                    = "list_entry";
        private static final String REFERENCE                     = "reference";
        private static final String ROLE                          = "role";
        private static final String CHECK                         = "check";
        private static final String CHECKGROUP                    = "checkgroup";
        private static final String LINK                          = "link";
        private static final String LABEL                         = "label";
        private static final String SEPARATOR                     = "separator";
        private static final String MULTISELECT                   = "multiselect";
        private static final String COMBO                         = "combo";
        private static final String CHUNK_ELEMENT                 = "chunk";
        private static final String ITEM_ELEMENT                  = "item";
        private static final String OPTIONAL                      = "optional";
        private static final String GROUP_ELEMENT                 = "group";
        private static final String DISPLAY_VALUE                 = "display_value";
        private static final String SHORT_DESCRIPTION             = "short_description";
        private static final String DEFAULT_MULTISELECT_DELIMITER = ";";
        private static final String DEFAULT_COMBO_DELIMITER       = ",";
        private static final String VALUE                         = "value";
        private static final String KEY                           = "key";
        private static final String LONG_TEXT                     = "long_text";
        private static final String TEXT                          = "text";
        private static final String NAME                          = "name";
        private static final String DELIMITER                     = "delimiter";
        private static final String TEXT_CONTEXT                  = "text_context";
        private static final String NAME_CONTEXT                  = "name_context";
        private static final String VALUES_CONTEXT                = "values_context";

        Locator    locator  = null;
        String     group    = null;
        String     preset   = null;
        Attributes mainAttr = null;

        String presetContext() {
            return (group != null ? "|group:" + group.replace(' ', '_') : "") + (preset != null ? "|preset:" + preset.replace(' ', '_') : "");
        }

        void addMsg(String tag, Attributes attr, String keyName, String attrName, Attributes mainAttr) {
            String context = null;
            if (mainAttr == null) {
                context = attr.getValue(TEXT_CONTEXT);
                if (context == null) {
                    context = attr.getValue(NAME_CONTEXT);
                }
                if (context == null) {
                    context = attr.getValue(VALUES_CONTEXT);
                }
            } else {
                // special case for list_entry
                context = mainAttr.getValue(VALUES_CONTEXT);
            }
            if (!msgs.containsKey(context)) {
                msgs.put(context, new MultiHashMap<>(true));
            }
            String key = null;
            if (keyName != null) {
                key = attr.getValue(keyName);
            }
            String value = attr.getValue(attrName);
            if (value != null && !"".equals(value)) {
                msgs.get(context).add(value, inputFilename + ":" + (locator != null ? locator.getLineNumber() : 0) + "(" + tag + ":" + attrName
                        + presetContext() + (key != null ? "|" + keyName + ":" + key : "") + ")");
            }
        }

        void addValues(String keyName, String valueAttr, String tag, Attributes attr, String defaultDelimiter) {
            String displayValues = attr.getValue(valueAttr);
            if (displayValues != null) {
                String delimiter = attr.getValue(DELIMITER);
                if (delimiter == null) {
                    delimiter = defaultDelimiter;
                }
                String context = attr.getValue(VALUES_CONTEXT);

                if (!msgs.containsKey(context)) {
                    msgs.put(context, new MultiHashMap<>(true));
                }
                String key = null;
                if (keyName != null) {
                    key = attr.getValue(keyName);
                }
                for (String s : displayValues.split(Pattern.quote(delimiter))) {
                    if (s != null && !"".equals(s)) {
                        msgs.get(context).add(s, inputFilename + ":" + (locator != null ? locator.getLineNumber() : 0) + "(" + tag + ":" + valueAttr
                                + presetContext() + (key != null ? "|" + keyName + ":" + key : "") + ")");
                    }
                }
            }
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            switch (qName) {
            case GROUP_ELEMENT:
                group = attr.getValue(NAME);
                addMsg(qName, attr, null, NAME, null);
                break;
            case ITEM_ELEMENT:
                preset = attr.getValue(NAME);
                addMsg(qName, attr, null, NAME, null);
                addMsg(qName, attr, null, NAME_TEMPLATE, null);
                mainAttr = null;
                break;
            case CHUNK_ELEMENT:
                mainAttr = null;
                break;
            case SEPARATOR:
                break;
            case LABEL:
                addMsg(qName, attr, null, TEXT, null);
                break;
            case OPTIONAL:
                addMsg(qName, attr, null, TEXT, null);
                break;
            case KEY:
                addMsg(qName, attr, KEY, TEXT, null);
                addMsg(qName, attr, KEY, LONG_TEXT, null);
                mainAttr = null;
                break;
            case TEXT:
            case CHECK:
                addMsg(qName, attr, KEY, TEXT, null);
                addMsg(qName, attr, KEY, LONG_TEXT, null);
                addMsg(qName, attr, null, VALUE_TEMPLATE, null);
                mainAttr = null;
                break;
            case LINK:
                break;
            case CHECKGROUP:
                addMsg(qName, attr, null, TEXT, null);
                break;
            case COMBO:
                handleCombo(qName, attr, DEFAULT_COMBO_DELIMITER);
                break;
            case MULTISELECT:
                handleCombo(qName, attr, DEFAULT_MULTISELECT_DELIMITER);
                break;
            case ROLE:
                addMsg(qName, attr, KEY, TEXT, null);
                addMsg(qName, attr, KEY, LONG_TEXT, null);
                break;
            case REFERENCE:
                break;
            case LIST_ENTRY:
                addMsg(qName, attr, VALUE, SHORT_DESCRIPTION, mainAttr);
                addMsg(qName, attr, VALUE, DISPLAY_VALUE, mainAttr);
                break;
            case PRESET_LINK:
            default:
            }
        }

        /**
         * @param qName
         * @param attr
         */
        private void handleCombo(String qName, Attributes attr, String defaultDelimiter) {
            addMsg(qName, attr, KEY, TEXT, null);
            addMsg(qName, attr, KEY, LONG_TEXT, null);
            String delimiter = attr.getValue(DELIMITER);
            addValues(KEY, DISPLAY_VALUES, qName, attr, delimiter != null ? delimiter : defaultDelimiter);
            addValues(KEY, SHORT_DESCRIPTIONS, qName, attr, delimiter != null ? delimiter : defaultDelimiter);
            mainAttr = new AttributesImpl(attr);
        }

        @Override
        public void endElement(String uri, String localMame, String qName) throws SAXException {
            switch (qName) {
            case GROUP_ELEMENT:
                group = null;
                break;
            case OPTIONAL:
                break;
            case ITEM_ELEMENT:
                preset = null;
                break;
            case CHUNK_ELEMENT:
            case COMBO:
            case MULTISELECT:
            default:
            }
        }
    }

    void parseXML(InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        handler = new MyHandler();
        saxParser.parse(input, handler);
    }

    void dump2Pot(PrintWriter pw) {
        // print a header
        pw.print("msgid \"\"\n");
        pw.print(EMPTY_MSGSTR);
        pw.print("\"Project-Id-Version: PACKAGE VERSION\\n\"\n");
        // "POT-Creation-Date: 2015-11-02 23:02+0100"
        String date = (new SimpleDateFormat("yyyy-MM-dd HH:mmZ", Locale.US)).format(new Date());
        pw.print("\"POT-Creation-Date: " + date + "\\n\"\n");
        pw.print("\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n");
        pw.print("\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n");
        pw.print("\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n");
        pw.print("\"MIME-Version: 1.0\\n\"\n");
        pw.print("\"Content-Type: text/plain; charset=UTF-8\\n\"\n");
        pw.print("\"Content-Transfer-Encoding: 8bit\\n\"\n");
        pw.print("\n");
        // dump the strings
        for (Entry<String, MultiHashMap<String, String>> entry : msgs.entrySet()) {
            String context = entry.getKey();
            MultiHashMap<String, String> map = entry.getValue();
            for (String msgId : map.getKeys()) {
                // output locations
                pw.print("#:");
                for (String loc : map.get(msgId)) {
                    pw.print(" " + loc);
                }
                pw.print("\n");
                if (context != null && !"".equals(context)) {
                    pw.print("msgctxt \"" + context + "\"\n");
                }
                pw.print("msgid \"" + msgId + "\"\n");
                pw.print(EMPTY_MSGSTR);
                pw.print("\n");
            }
        }
        // trailer
        pw.print("#. Put one translator per line, in the form of NAME <EMAIL>, YEAR1, YEAR2\n");
        pw.print("#: " + inputFilename + ":0(None)\n");
        pw.print("msgid \"translator-credits\"\n");
        pw.print(EMPTY_MSGSTR);
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
        try { // NOSONAR
            os = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);

            Preset2Pot p = new Preset2Pot();
            p.setInputFilename("stdin");

            // arguments
            Option inputFile = Option.builder("i").longOpt(INPUT_OPT).hasArg().desc("input preset file, default: standard in").build();

            Option outputFile = Option.builder("o").longOpt(OUTPUT_OPT).hasArg().desc("output .pot file, default: standard out").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(outputFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT_OPT)) {
                    // initialise the member variable
                    String input = line.getOptionValue(INPUT_OPT);
                    p.setInputFilename(input);
                    is = new FileInputStream(input);
                }
                if (line.hasOption(OUTPUT_OPT)) {
                    String output = line.getOptionValue(OUTPUT_OPT);
                    os = new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8);
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(Preset2Pot.class.getSimpleName(), options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            try {
                p.parseXML(is);
                p.dump2Pot(new PrintWriter(os));
            } catch (FileNotFoundException | ParserConfigurationException | UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                System.err.println("Error at line " + p.getLocator().getLineNumber());
                e.printStackTrace();
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
