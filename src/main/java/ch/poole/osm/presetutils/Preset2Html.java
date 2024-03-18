package ch.poole.osm.presetutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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
 * Make a (semi-)nice HTML page out of the presets
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */
public class Preset2Html {

    private static final String TRUE                       = "true";
    private static final String LIST_ENTRY_ELEMENT         = "list_entry";
    private static final String REFERENCE_ELEMENT          = "reference";
    private static final String ROLE_ELEMENT               = "role";
    private static final String PRESET_LINK_ELEMENT        = "preset_link";
    private static final String TEXT_ELEMENT               = "text";
    private static final String CHECK_ELEMENT              = "check";
    private static final String COMBO_ELEMENT              = "combo";
    private static final String MULTISELECT_ELEMENT        = "multiselect";
    private static final String KEY_ELEMENT                = "key";
    private static final String OPTIONAL_ELEMENT           = "optional";
    private static final String LABEL_ELEMENT              = "label";
    private static final String SEPARATOR_ELEMENT          = "separator";
    private static final String PRESETS_ELEMENT            = "presets";
    private static final String GROUP_ELEMENT              = "group";
    private static final String CHUNK_ELEMENT              = "chunk";
    private static final String ITEM_ELEMENT               = "item";
    private static final String DEPRECATED_ATTRIBUTE       = "deprecated";
    private static final String SHORTDESCRIPTION_ATTRIBUTE = "shortdescription";
    private static final String ID_ATTRIBUTE               = "id";
    private static final String ICON_ATTRIBUTE             = "icon";
    private static final String REF_ATTRIBUTE              = "ref";
    private static final String KEY_ATTRIBUTE              = "key";
    private static final String NAME_ATTRIBUTE             = "name";
    private static final String PRESET_NAME_ATTRIBUTE      = "preset_name";
    private static final String VALUE_ATTRIBUTE            = "value";
    private static final String REGIONS_ATTRIBUTE          = "regions";
    private static final String EXCLUDE_REGIONS_ATTRIBUTE  = "exclude_regions";

    private static final int GROUP_INDENT = 30;

    HashMap<String, MultiHashMap<String, String>> msgs = new HashMap<>();

    String inputFilename;
    String vespucciLink = null;
    String josmLink     = null;
    int    groupCount   = 0;

    void parseXML(final InputStream input, final PrintWriter pw) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        pw.write(
                "<?xml version='1.0' encoding='utf-8' ?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        pw.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        pw.write("<head>");
        pw.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>");
        pw.write("<link rel=\"stylesheet\" href=\"website/preset.css\" type=\"text/css\" />");
        pw.write("</head><body>");

        saxParser.parse(input, new DefaultHandler() {

            String              group             = null;
            String              preset            = null;
            String              regions           = null;
            String              chunk             = null;
            Map<String, String> chunkKeys         = new HashMap<>();
            Map<String, String> chunkOptionalKeys = new HashMap<>();
            Map<String, String> chunkLinks        = new HashMap<>();
            Map<String, String> chunkRoles        = new HashMap<>();
            String              icon              = null;
            String              icon2             = null;
            String              keys              = null;
            String              optionalKeys      = null;
            String              links             = null;
            String              roles             = null;
            boolean             optional          = false;
            StringBuilder       buffer            = new StringBuilder();
            boolean             deprecated        = false;
            boolean             separator         = false;

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                switch (name) {
                case PRESETS_ELEMENT:
                    String shortdescription = attr.getValue(SHORTDESCRIPTION_ATTRIBUTE);
                    if (shortdescription == null) {
                        pw.write("<h1>Presets from File " + inputFilename + "</h1>\n");
                    } else {
                        pw.write("<h1>" + shortdescription + "</h1>\n");
                    }
                    if (vespucciLink != null) {
                        try {
                            pw.write("<div class=\"download\"><a href=\"vespucci:/preset?preseturl=" + URLEncoder.encode(vespucciLink, "UTF-8")
                                    + (shortdescription != null ? "&presetname=" + shortdescription : "") + "\">Download link for Vespucci</a><br>\n");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    if (josmLink != null) {
                        pw.write("<div class=\"download\"><a href=\"" + josmLink + "\">Download link for JOSM</a></div>\n");
                    }
                    pw.write("<p />");
                    break;
                case GROUP_ELEMENT:
                    groupCount++;
                    group = attr.getValue(NAME_ATTRIBUTE);
                    buffer.append("<div class=\"group\"><div style=\"margin-left: " + ((groupCount - 1) * GROUP_INDENT) + "px\"><h" + (groupCount + 1) + ">");
                    String groupIcon = attr.getValue(ICON_ATTRIBUTE);
                    if (groupIcon != null && !"".equals(groupIcon)) {
                        String groupIcon2 = groupIcon.replace("${ICONPATH}", "icons/png/");
                        groupIcon2 = groupIcon2.replace("${ICONTYPE}", "png");
                        if (!groupIcon.equals(groupIcon2)) {
                            buffer.append("<img src=\"" + groupIcon2 + "\" style=\"vertical-align:middle\"> ");
                        }
                    }
                    buffer.append("<a name=\"" + group + "\"></a>" + group);
                    String groupRegions = getRegions(attr, true);
                    if (groupRegions != null) {
                        buffer.append(" " + groupRegions);
                    }
                    buffer.append("</h" + (groupCount + 1) + ">\n");
                    if (groupCount == 1) {
                        pw.write("<a href=\"#" + group + "\">" + group + getRegions(attr, true) + "</a> ");
                    }
                    break;
                case ITEM_ELEMENT:
                    preset = attr.getValue(NAME_ATTRIBUTE);
                    regions = getRegions(attr, true);
                    String deprecatedStr = attr.getValue(DEPRECATED_ATTRIBUTE);
                    deprecated = deprecatedStr != null && TRUE.equals(deprecatedStr);
                    icon = attr.getValue(ICON_ATTRIBUTE);
                    if (icon != null && !"".equals(icon)) {
                        icon2 = icon.replace("${ICONPATH}", "icons/png/");
                        icon2 = icon2.replace("${ICONTYPE}", "png");
                    }
                    break;
                case CHUNK_ELEMENT:
                    chunk = attr.getValue(ID_ATTRIBUTE);
                    keys = "";
                    break;
                case SEPARATOR_ELEMENT:
                    break;
                case LABEL_ELEMENT:
                    break;
                case OPTIONAL_ELEMENT:
                    optional = true;
                    break;
                case KEY_ELEMENT:
                case MULTISELECT_ELEMENT:
                case COMBO_ELEMENT:
                case CHECK_ELEMENT:
                case TEXT_ELEMENT:
                    if (!optional) {
                        keys = addTags(keys, attr);
                    } else {
                        optionalKeys = addTags(optionalKeys, attr);
                    }
                    break;
                case PRESET_LINK_ELEMENT:
                    String link = attr.getValue(PRESET_NAME_ATTRIBUTE);
                    if (link != null) {
                        if (links == null) {
                            links = link;
                        } else {
                            links = links + "<BR>" + link;
                        }
                    }
                    break;
                case ROLE_ELEMENT:
                    String role = attr.getValue(KEY_ATTRIBUTE);
                    if (role != null && !"".equals(role)) {
                        if (roles == null) {
                            roles = role;
                        } else {
                            roles = roles + "<BR>" + role;
                        }
                    }
                    break;
                case REFERENCE_ELEMENT:
                    String ref = attr.getValue(REF_ATTRIBUTE);
                    String refKeys = chunkKeys.get(ref);
                    if (refKeys != null) {
                        if (!optional) {
                            if (keys != null) {
                                keys = keys + refKeys;
                            } else {
                                keys = refKeys;
                            }
                        } else {
                            if (optionalKeys != null) {
                                optionalKeys = optionalKeys + refKeys;
                            } else {
                                optionalKeys = refKeys;
                            }
                        }
                    }
                    String refOptionalKeys = chunkOptionalKeys.get(ref);
                    if (refOptionalKeys != null) {
                        if (optionalKeys != null) {
                            optionalKeys = optionalKeys + refOptionalKeys;
                        } else {
                            optionalKeys = refOptionalKeys;
                        }
                    }

                    String refLinks = chunkLinks.get(ref);
                    if (refLinks != null) {
                        if (links != null) {
                            links = links + refLinks;
                        } else {
                            links = refLinks;
                        }
                    }
                    String refRoles = chunkRoles.get(ref);
                    if (refRoles != null) {
                        if (roles != null) {
                            roles = roles + refLinks;
                        } else {
                            roles = refRoles;
                        }
                    }

                    if ((refKeys == null || "".equals(refKeys)) && (refOptionalKeys == null || "".equals(refOptionalKeys))
                            && (refLinks == null || "".equals(refLinks)) && (refRoles == null || "".equals(refRoles))) {
                        System.err.println(ref + " was not found for preset " + preset);
                    }
                    break;
                case LIST_ENTRY_ELEMENT:
                default:
                    // nothing
                }
            }

            private String addTags(String result, Attributes attr) {
                String key = attr.getValue(KEY_ATTRIBUTE);
                String value = attr.getValue(VALUE_ATTRIBUTE);
                if (key != null && !"".equals(key)) {
                    String deprecatedStr = attr.getValue(DEPRECATED_ATTRIBUTE);
                    boolean fieldDeprecated = deprecatedStr != null && TRUE.equals(deprecatedStr);
                    String tag = "<div class=\"no_break\">" + key + "=" + (value != null ? value : "*") + getRegions(attr, false)
                            + (fieldDeprecated ? " <i>deprecated</i>" : "") + "</div>";
                    if (result != null) {
                        result = result + tag;
                    } else {
                        result = tag;
                    }
                }
                return result;
            }

            private String getRegions(Attributes attr, boolean withDiv) {
                String reg = attr.getValue(REGIONS_ATTRIBUTE);
                if (reg != null) {
                    String result = "[" + reg + "]";
                    if (TRUE.equals(attr.getValue(EXCLUDE_REGIONS_ATTRIBUTE))) {
                        result = "!" + result;
                    }
                    if (withDiv) {
                        return " <div class=\"no_break\">" + result + "</div>";
                    }
                    return " " + result;
                }
                return "";
            }

            @Override
            public void endElement(String uri, String localMame, String name) throws SAXException {
                switch (name) {
                case GROUP_ELEMENT:
                    group = null;
                    buffer.append("</div></div>\n");
                    groupCount--;
                    break;
                case OPTIONAL_ELEMENT:
                    optional = false;
                    break;
                case ITEM_ELEMENT:
                    if (preset != null) {
                        if (separator) {
                            buffer.append("<div style=\"clear: both\" class=\"container\">");
                            separator = false;
                        } else {
                            buffer.append("<div class=\"container\">");
                        }
                        if (icon != null && !"".equals(icon)) {
                            if (!icon2.equals(icon)) {
                                buffer.append("<div class=\"preset\"><img src=\"" + icon2 + "\"><br>" + preset.replace("/", " / "));
                            } else {
                                buffer.append("<div class=\"preset\">" + preset.replace("/", " / "));
                            }
                        } else {
                            buffer.append("<div class=\"preset\">" + preset.replace("/", " / "));
                        }
                        buffer.append(" " + regions);
                        buffer.append(isDeprecated());
                        buffer.append("</div>");
                        appendKeys();
                        buffer.append("</div>");
                        preset = null;
                    }
                    keys = null;
                    optionalKeys = null;
                    links = null;
                    break;
                case CHUNK_ELEMENT:
                    if (chunk != null) {
                        if (keys != null) {
                            chunkKeys.put(chunk, keys);
                        }
                        if (optionalKeys != null) {
                            chunkOptionalKeys.put(chunk, optionalKeys);
                        }
                        if (links != null) {
                            chunkLinks.put(chunk, links);
                        }
                        if (roles != null) {
                            chunkRoles.put(chunk, roles);
                        }
                    } else {
                        System.err.println("chunk null");
                    }
                    keys = null;
                    optionalKeys = null;
                    chunk = null;
                    links = null;
                    break;
                case SEPARATOR_ELEMENT:
                    separator = true;
                    break;
                case COMBO_ELEMENT:
                case MULTISELECT_ELEMENT:
                default:
                    // nothing
                }
            }

            private String isDeprecated() {
                return deprecated ? "<p><i>deprecated</i>" : "";
            }

            private void appendKeys() {
                if (keys != null) {
                    buffer.append("<div class=\"popup\" \">" + keys);
                    if (optionalKeys != null) {
                        buffer.append("<P/><B>Optional:</B><BR>");
                        buffer.append(optionalKeys);
                    }
                    if (links != null) {
                        buffer.append("<P/><B>Links:</B><BR>");
                        buffer.append(links);
                    }
                    buffer.append("</div>");
                }
            }

            @Override
            public void endDocument() {
                pw.write(buffer.toString());
                pw.write("<div class=\"footer\">Page generated by Preset2Html, Simon Poole</body>");
                pw.flush();
            }
        });
    }

    private void setVespucciLink(String optionValue) {
        vespucciLink = optionValue;
    }

    private void setJosmLink(String optionValue) {
        josmLink = optionValue;
    }

    private void setInputFilename(String fn) {
        inputFilename = fn;
    }

    public static void main(String[] args) {
        // defaults
        InputStream is = System.in;
        OutputStream os = System.out;
        Preset2Html p = new Preset2Html();
        p.setInputFilename("stdin");

        // arguments
        Option inputFile = Option.builder("i").longOpt("input").hasArg().desc("input preset file, default: standard in").build();

        Option outputFile = Option.builder("o").longOpt("output").hasArg().desc("output .html file, default: standard out").build();

        Option vespucciLink = Option.builder("v").longOpt("vespucci").hasArg().desc("download link vespucci format, default: none").build();

        Option josmLink = Option.builder("j").longOpt("josm").hasArg().desc("download link JOSM format, default: none").build();

        Options options = new Options();

        options.addOption(inputFile);
        options.addOption(outputFile);
        options.addOption(vespucciLink);
        options.addOption(josmLink);

        CommandLineParser parser = new DefaultParser();
        try {
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
                    os = new FileOutputStream(output);
                }
                if (line.hasOption("vespucci")) {
                    p.setVespucciLink(line.getOptionValue("vespucci"));
                }
                if (line.hasOption("josm")) {
                    p.setJosmLink(line.getOptionValue("josm"));
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Preset2Html", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            try {
                p.parseXML(is, new PrintWriter(os));
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
        }
    }
}
