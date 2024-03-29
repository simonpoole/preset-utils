package ch.poole.osm.presetutils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Generate a JOSM preset file from iD preset files in the iD repo
 * 
 * @author Simon Poole
 *
 */
public class ID2JOSM {

    private static final String DEBUG_TAG = ID2JOSM.class.getSimpleName();

    private static final Logger LOGGER = Logger.getLogger(DEBUG_TAG);

    private static final String PRESETURL_OPT_LONG       = "preseturl";
    private static final String PRESETURL_OPT_SHORT      = "p";
    private static final String FIELDSURL_OPT_LONG       = "fieldsurl";
    private static final String FIELDSURL_OPT_SHORT      = "f";
    private static final String JOSMONLY_OPT_LONG        = "josmonly";
    private static final String JOSMONLY_OPT_SHORT       = "j";
    private static final String NOTAGINFO_OPT_LONG       = "notaginfo";
    private static final String NOTAGINFO_OPT_SHORT      = "n";
    private static final String CHUNK_OPT_LONG           = "chunk";
    private static final String CHUNK_OPT_SHORT          = "c";
    private static final String OUTPUT_OPT_LONG          = "output";
    private static final String OUTPUT_OPT_SHORT         = "o";
    private static final String TRANSLATIONURL_OPT_LONG  = "translationurl";
    private static final String TRANSLATIONURL_OPT_SHORT = "t";

    private static final String DEFAULT_FIELD_URL       = "https://raw.githubusercontent.com/openstreetmap/id-tagging-schema/main/dist/fields.json";
    private static final String DEFAULT_PRESET_URL      = "https://raw.githubusercontent.com/openstreetmap/id-tagging-schema/main/dist/presets.json";
    private static final String DEFAULT_TRANSLATION_URL = "https://raw.githubusercontent.com/openstreetmap/id-tagging-schema/main/dist/translations/en.json";

    private static String fieldsUrl      = DEFAULT_FIELD_URL;
    private static String presetUrl      = DEFAULT_PRESET_URL;
    private static String translationUrl = DEFAULT_TRANSLATION_URL;

    enum Geometry {
        POINT, VERTEX, LINE, AREA, RELATION;

        public String toTagInfo() {
            switch (this) {
            case POINT:
            case VERTEX:
                return "nodes";
            case LINE:
            case AREA:
                return "ways";
            case RELATION:
                return "relations";
            }
            return "all";
        }
    }

    enum FieldType {
        TEXT, NUMBER, DATE, LOCALIZED, TEL, EMAIL, URL, COLOUR, TEXTAREA, COMBO, TYPECOMBO, MULTICOMBO, 
        NETWORKCOMBO, SEMICOMBO, MANYCOMBO, DIRECTIONALCOMBO, CHECK, DEFAULTCHECK, ONEWAYCHECK, RADIO, STRUCTURERADIO, 
        ACCESS, ADDRESS, CYCLEWAY, MAXSPEED, RESTRICTIONS, WIKIPEDIA, WIKIDATA, IDENTIFIER, ROADHEIGHT, ROADSPEED
    }

    static class Field {
        String                                 name;
        String                                 label;
        List<ValueAndDescription>              keys;
        FieldType                              fieldType;
        List<Geometry>                         geometry;
        boolean                                universal     = false;
        String                                 defaultValue;
        String                                 placeHolder;
        List<ValueAndDescription>              options;
        boolean                                caseSensitive = false;
        boolean                                snakeCase     = true;
        Map<String, List<ValueAndDescription>> cachedOptions = new HashMap<>();

        public void toJosm(PrintWriter writer, List<Geometry> currentGeoms) {
            toJosm(writer, currentGeoms, 2);
        }

        public void toJosm(PrintWriter writer, List<Geometry> currentGeoms, int baseIndent) {
            switch (fieldType) {
            case TEXT:
            case NUMBER:
            case DATE:
            case LOCALIZED:
            case TEL:
            case EMAIL:
            case URL:
            case COLOUR:
            case TEXTAREA:
            case ADDRESS:
            case CYCLEWAY:
            case MAXSPEED:
            case RESTRICTIONS:
            case WIKIPEDIA:
            case WIKIDATA:
            case IDENTIFIER:
            case ROADHEIGHT:
            case ROADSPEED:
                if (keys != null) {
                    for (ValueAndDescription key : keys) {
                        indent(writer, baseIndent);
                        if (keys.size() == 1 && label != null) {
                            writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label)
                                    + "\"" + fieldType2Attribute(fieldType) + " />");
                        } else {
                            writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\""
                                    + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
                                    + fieldType2Attribute(fieldType) + " />");
                        }
                    }
                }
                break;
            case ACCESS:
            case COMBO:
            case TYPECOMBO:
            case SEMICOMBO:
            case NETWORKCOMBO:
            case DIRECTIONALCOMBO:
            case RADIO:
                if (keys != null) {
                    boolean resetOptions = options == null;
                    for (ValueAndDescription key : keys) {
                        boolean multiselect = FieldType.SEMICOMBO.equals(fieldType);
                        if (options == null) {
                            if (tagInfoMode) {
                                optionsComment(writer, baseIndent);
                                String taginfoFilter = null;
                                if (currentGeoms == null) {
                                    if (geometry != null && !geometry.isEmpty()) {
                                        taginfoFilter = geometry.get(0).toTagInfo(); // FIXME
                                    }
                                } else if (currentGeoms.size() == 1) {
                                    taginfoFilter = currentGeoms.get(0).toTagInfo();
                                }
                                List<ValueAndDescription> fromCache = cachedOptions.get(taginfoFilter);
                                if (fromCache != null) {
                                    options = fromCache;
                                } else {
                                    options = TagInfo.getOptionsFromTagInfo(key.value, taginfoFilter, true, 0, 25, true);
                                    cachedOptions.put(taginfoFilter, options);
                                }
                            } else {
                                return;
                            }
                        }
                        indent(writer, baseIndent);
                        String labelText = key.description != null ? key.description : (label != null && keys.size() == 1 ? label : null);
                        writer.println("<" + (multiselect ? "multiselect" : "combo") + " key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\""
                                + (labelText != null ? " text=\"" + StringEscapeUtils.escapeXml11(labelText) + "\"" : ""));
                        indent(writer, baseIndent + 1);
                        writer.print("values=\"");
                        for (int i = 0; i < options.size(); i++) {
                            writer.print(StringEscapeUtils.escapeXml11(options.get(i).value));
                            if (i < options.size() - 1) {
                                writer.print(multiselect ? ";" : ",");
                            }
                        }
                        writer.println("\"");
                        indent(writer, baseIndent + 1);
                        writer.print("display_values=\"");
                        for (int i = 0; i < options.size(); i++) {
                            ValueAndDescription v = options.get(i);
                            if (v.description != null && !"".equals(v.description)) {
                                writer.print(StringEscapeUtils.escapeXml11(v.description));
                            } else {
                                writer.print(StringEscapeUtils.escapeXml11(v.value));
                            }
                            if (i < options.size() - 1) {
                                writer.print(multiselect ? ";" : ",");
                            }
                        }
                        writer.println("\" />");
                    }
                    if (resetOptions) {
                        options = null;
                    }
                }
                break;
            case MULTICOMBO:
                if (keys != null && keys.size() == 1) {
                    ValueAndDescription key = keys.get(0);
                    if (options == null) {
                        if (tagInfoMode) {
                            optionsComment(writer, baseIndent);
                            options = TagInfo.getKeysFromTagInfo(key.value);
                        } else {
                            return;
                        }
                    }
                    for (ValueAndDescription v : options) {
                        indent(writer, baseIndent);
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value + v.value) + "\""
                                + (v.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(v.description) + "\"" : "") + " disable_off=\"true\" />");
                    }

                }
                break;
            case CHECK:
                if (keys != null && keys.size() == 1) {
                    ValueAndDescription key = keys.get(0);
                    indent(writer, baseIndent);
                    if (label != null) {
                        writer.println("<combo key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label) + "\""
                                + " values=\"yes,no\" />");
                    } else {
                        writer.println("<combo key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\""
                                + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
                                + " values=\"yes,no\" />");
                    }
                }
                break;
            case ONEWAYCHECK:
            case DEFAULTCHECK:
                if (keys != null && keys.size() == 1) {
                    ValueAndDescription key = keys.get(0);
                    indent(writer, baseIndent);
                    if (label != null) {
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label)
                                + "\" disable_off=\"true\" />");
                    } else {
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\""
                                + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
                                + " disable_off=\"true\" />");
                    }
                }
                break;
            case MANYCOMBO:
            case STRUCTURERADIO: // structureRadio should allow only one selection, JOSM doesn't support that currently
                if (options != null) {
                    for (ValueAndDescription v : options) {
                        indent(writer, baseIndent);
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(v.value) + "\""
                                + (v.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(v.description) + "\"" : "") + " disable_off=\"true\" />");
                    }
                }
                break;
            default:
                LOGGER.log(Level.WARNING, "Unhandled field: {0}", fieldType);
            }
        }

        /**
         * @param writer
         * @param baseIndent
         */
        private void optionsComment(PrintWriter writer, int baseIndent) {
            indent(writer, baseIndent);
            writer.println("<!-- no values in fields.json, retrieved these from taginfo -->");
        }

        String fieldType2Attribute(FieldType fieldType) {
            if (!josmOnlyMode) {
                switch (fieldType) {
                case LOCALIZED:
                    return " i18n=\"true\"";
                case URL:
                    return " value_type=\"website\"";
                case WIKIPEDIA:
                    return " value_type=\"wikipedia\"";
                case TEL:
                    return " value_type=\"phone\"";
                case NUMBER:
                    return " value_type=\"integer\"";
                default:
                    return "";
                }
            } else {
                return "";
            }
        }
    }

    static class Tag {
        String key;
        String value;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Tag other = (Tag) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    static class Item {
        public String  path;
        String         name;
        String         icon;
        List<Geometry> geometries;
        List<Tag>      tags;
        List<Tag>      addTags;
        List<Tag>      removeTags;
        List<Field>    fields;           // might use chunk references here
        List<Field>    moreFields;
        boolean        searchable = true;
        Tag            reference;

        public void toJosm(PrintWriter writer) {
            indent(writer, 1);
            String translatedName = presetNameTranslations.get(name);
            writer.print("<item name=\"" + StringEscapeUtils.escapeXml11(translatedName != null ? translatedName : name) + "\" ");
            if (icon != null) {
                writer.print("icon=\"" + StringEscapeUtils.escapeXml11(icon) + "\" ");
            }
            if (geometries != null) {
                List<Geometry> temp = new ArrayList<>();
                // squash vertex and node
                boolean nodeSeen = false;
                for (Geometry geom : geometries) {
                    if (geom == Geometry.POINT || geom == Geometry.VERTEX) {
                        if (nodeSeen) {
                            continue;
                        }
                        nodeSeen = true;
                    }
                    temp.add(geom);
                }
                writer.print("type=\"");
                for (int i = 0; i < temp.size(); i++) {
                    switch (temp.get(i)) {
                    case POINT:
                    case VERTEX:
                        writer.print("node");
                        break;
                    case LINE:
                        writer.print("way");
                        break;
                    case AREA:
                        writer.print("closedway,multipolygon");
                        break;
                    case RELATION:
                        writer.print("relation");
                        break;
                    }
                    if (i < temp.size() - 1) {
                        writer.print(",");
                    }
                }
                writer.print("\" ");
            }
            if (!josmOnlyMode) {
                writer.print((!searchable ? "deprecated=\"true\" " : ""));
            }
            writer.println("preset_name_label=\"true\">");

            if (reference != null && reference.key != null) {
                indent(writer, 2);
                if (reference.value != null) {
                    writer.println("<link wiki=\"Tag:" + reference.key + "=" + reference.value + "\" />");
                } else {
                    writer.println("<link wiki=\"Key:" + reference.key + "\" />");
                }
            }

            List<Tag> tempTags = (tags == null ? new ArrayList<Tag>() : new ArrayList<>(tags));

            // try to keep top level tags on top
            if (addTags != null && tags != null) {
                for (Tag tag : addTags) {
                    if (!(tags.contains(tag))) {
                        if (Tags.OBJECT_KEYS.contains(tag.key)) {
                            tempTags.add(0, tag);
                        } else {
                            tempTags.add(tag);
                        }
                    }
                }
            }

            for (Tag tag : tempTags) {
                if (tag.key != null && !"".equals(tag.key) && !tag.key.contains("*")) {
                    indent(writer, 2);
                    if (tag.value != null && !"".equals(tag.value) && !tag.value.contains("*")) {
                        writer.println(
                                "<key key=\"" + StringEscapeUtils.escapeXml11(tag.key) + "\" value=\"" + StringEscapeUtils.escapeXml11(tag.value) + "\" />");
                    } else { // generate a text field
                        writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(tag.key) + "\" />");
                    }
                }
            }

            if (fields != null) {
                for (Field field : fields) {
                    if (chunkMode) {
                        indent(writer, 2);
                        writer.println("<reference ref=\"" + StringEscapeUtils.escapeXml11(fieldKeys.get(field)) + "\" />");
                    } else {
                        field.toJosm(writer, geometries);
                    }
                }
            }

            if (moreFields != null && !moreFields.isEmpty()) {
                indent(writer, 2);
                writer.println("<optional>");
                for (Field field : moreFields) {
                    if (chunkMode) {
                        indent(writer, 3);
                        writer.println("<reference ref=\"" + StringEscapeUtils.escapeXml11(fieldKeys.get(field)) + "\" />");
                    } else {
                        field.toJosm(writer, geometries, 3);
                    }
                }
                indent(writer, 2);
                writer.println("</optional>");
            }

            indent(writer, 1);
            writer.println("</item>");
        }

        List<String> tagKeys() {
            List<String> result = new ArrayList<>();
            if (tags != null) {
                for (Tag t : tags) {
                    result.add(t.key);
                }
            }
            if (addTags != null) {
                for (Tag t : addTags) {
                    result.add(t.key);
                }
            }
            return result;
        }
    }

    static Map<String, Field> fields = new HashMap<>();

    static Map<Field, String> fieldKeys = new HashMap<>();

    static LinkedHashMap<String, Item> items = new LinkedHashMap<>();

    private static Map<String, String> presetNameTranslations = new HashMap<>();
    private static Map<String, String> fieldTranslations      = new HashMap<>();

    private static boolean chunkMode    = false;
    private static boolean tagInfoMode  = true;
    private static boolean josmOnlyMode = false;

    /**
     * @param printWriter
     * @return
     */
    static void convertId(@NotNull PrintWriter printWriter) {
        try {
            parseIdTranslation(new URL(translationUrl)); // retrieve before fields
            parseIdFields(new URL(fieldsUrl));
            parseIdPreset(new URL(presetUrl));

            // print out
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<presets xmlns=\"http://josm.openstreetmap.de/tagging-preset-1.0\"" + " shortdescription=\"iD presets\" description=\"\">");
            if (chunkMode) {
                for (String fieldName : fields.keySet()) {
                    Field field = fields.get(fieldName);
                    if (field != null) {
                        indent(printWriter, 1);
                        printWriter.println("<chunk id=\"" + fieldName + "\">");
                        try {
                            field.toJosm(printWriter, null);
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Error writing field {0}: {1}", new Object[] { fieldName, ex.getMessage() });
                            ex.printStackTrace();
                        }
                        indent(printWriter, 1);
                        printWriter.println("</chunk>");
                    }
                }
            }
            for (Item item : items.values()) {
                try {
                    item.toJosm(printWriter);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error writing item {0}: {1}", new Object[] { item.name, ex.getMessage() });
                }
            }
            printWriter.println("</presets>");
            printWriter.close();
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Invalid URL: {0}", e.getMessage());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Problem converting presets: {0}", e.getMessage());
        }
    }

    /**
     * Retrieve and parse an iD translations files
     * 
     * @param url url for the translation file
     * @throws IOException if something goes wrong
     */
    private static void parseIdTranslation(@NotNull URL url) throws IOException {
        try (InputStream is = Utils.openConnection(url); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            if (reader.hasNext()) {
                String jsonName = reader.nextName();
                reader.beginObject();
                while (reader.hasNext()) {
                    jsonName = reader.nextName();
                    if ("presets".equals(jsonName)) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            jsonName = reader.nextName();
                            if ("presets".equals(jsonName)) {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String fieldName = reader.nextName();
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        if ("name".equals(reader.nextName())) {
                                            presetNameTranslations.put(fieldName, reader.nextString());
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                }
                                reader.endObject();
                            } else if ("fields".equals(jsonName)) {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String fieldName = reader.nextName();
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        jsonName = reader.nextName();
                                        if ("label".equals(jsonName)) {
                                            fieldTranslations.put(fieldName, reader.nextString());
                                        } else if ("options".equals(jsonName)) {
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                jsonName = reader.nextName();
                                                if (reader.peek().equals(JsonToken.BEGIN_OBJECT)) {
                                                    reader.beginObject();
                                                    while (reader.hasNext()) {
                                                        if ("title".equals(reader.nextName())) {
                                                            fieldTranslations.put(optionsKey(fieldName, jsonName), reader.nextString());
                                                        } else {
                                                            reader.skipValue();
                                                        }
                                                    }
                                                    reader.endObject();
                                                } else {
                                                    fieldTranslations.put(optionsKey(fieldName, jsonName), reader.nextString());
                                                }
                                            }
                                            reader.endObject();
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                }
                                reader.endObject();
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                    reader.endObject();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading translations: {0}", e.getMessage());
            throw e;
        }
    }

    /**
     * Construct a key for a field "option"/value
     * 
     * @param fieldName the field name
     * @param optionName the option name
     * @return a hopefully unique key
     */
    private static String optionsKey(String fieldName, String optionName) {
        return fieldName + "|options|" + optionName;
    }

    /**
     * Retrieve and parse an iD preset file
     * 
     * @see <a href="https://github.com/ideditor/schema-builder/blob/main/schemas/preset.json">idpreset schema</a>
     * 
     * @param url url for the preset file
     * @throws IOException if something goes wrong
     */
    private static void parseIdPreset(@NotNull URL url) throws IOException {
        try (InputStream is = Utils.openConnection(url); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                Item current = new Item();
                current.path = jsonName;
                current.name = jsonName;
                reader.beginObject();
                boolean save = true;
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case "icon":
                        String icon = reader.nextString();
                        if (icon != null && !"".equals(icon)) {
                            current.icon = icon;
                        }
                        break;
                    case "searchable":
                        current.searchable = reader.nextBoolean();
                        break;
                    case "tags":
                        reader.beginObject();
                        current.tags = new ArrayList<>();
                        while (reader.hasNext()) {
                            Tag tag = new Tag();
                            tag.key = reader.nextName();
                            tag.value = reader.nextString();
                            current.tags.add(tag);
                            if (("name".equals(tag.key) || "brand:wikidata".equals(tag.key)) && tag.value != null && !"".equals(tag.value)) {
                                save = false; // this removes entries generated from the name suggestion
                                              // index
                            }
                        }
                        reader.endObject();
                        break;
                    case "addTags":
                        reader.beginObject();
                        current.addTags = new ArrayList<>();
                        while (reader.hasNext()) {
                            Tag tag = new Tag();
                            tag.key = reader.nextName();
                            tag.value = reader.nextString();
                            current.addTags.add(tag);
                        }
                        reader.endObject();
                        break;
                    case "removeTags":
                        reader.beginObject();
                        current.removeTags = new ArrayList<>();
                        while (reader.hasNext()) {
                            Tag tag = new Tag();
                            tag.key = reader.nextName();
                            tag.value = reader.nextString();
                            current.removeTags.add(tag);
                        }
                        reader.endObject();
                        break;
                    case "geometry":
                        reader.beginArray();
                        current.geometries = new ArrayList<>();
                        while (reader.hasNext()) {
                            current.geometries.add(Geometry.valueOf(reader.nextString().toUpperCase()));
                        }
                        reader.endArray();
                        break;
                    case "fields":
                        reader.beginArray();
                        current.fields = new ArrayList<>();
                        List<String> tagKeys = current.tagKeys();
                        while (reader.hasNext()) {
                            String fieldName = reader.nextString();
                            addFields(current.fields, tagKeys, fieldName);
                        }
                        reader.endArray();
                        break;
                    case "moreFields":
                        reader.beginArray();
                        current.moreFields = new ArrayList<>();
                        tagKeys = current.tagKeys();
                        while (reader.hasNext()) {
                            String fieldName = reader.nextString();
                            addFields(current.moreFields, tagKeys, fieldName);
                        }
                        reader.endArray();
                        break;
                    case "reference":
                        reader.beginObject();
                        current.reference = new Tag();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                            case "key":
                                current.reference.key = reader.nextString();
                                break;
                            case "value":
                                current.reference.value = reader.nextString();
                                break;
                            }
                        }
                        reader.endObject();
                        break;
                    case "matchScore":
                    case "countryCodes":
                    case "replacement":
                    case "imageURL":
                    case "terms":
                    default:
                        reader.skipValue();
                    }
                }
                if (save) {
                    items.put(current.path, current);
                    if ((current.fields == null || current.fields.isEmpty()) && (current.moreFields == null || current.moreFields.isEmpty())) {
                        // implicit inheritance
                        int lastSlash = current.path.lastIndexOf('/');
                        if (lastSlash > 0) {
                            String parentPath = current.path.substring(0, lastSlash);
                            Item parent = items.get(parentPath);
                            if (parent != null) {
                                List<String> tagKeys = current.tagKeys();
                                current.fields = new ArrayList<>();
                                if (parent.fields != null) {
                                    for (Field f : parent.fields) {
                                        addFields(current.fields, tagKeys, f.name);
                                    }
                                }
                                if (parent.moreFields != null) {
                                    current.moreFields = new ArrayList<>();
                                    for (Field f : parent.moreFields) {
                                        addFields(current.moreFields, tagKeys, f.name);
                                    }
                                }
                            }
                        }
                    }
                    // remove duplicate tags/fields, can't do this before parsing the item is finished
                    if (current.tags != null && current.fields != null) {
                        for (Tag t : new ArrayList<>(current.tags)) {
                            for (Field f : new ArrayList<>(current.fields)) {
                                if (f.keys == null) {
                                    LOGGER.log(Level.WARNING, "keys is null for field {0} item {1}", new Object[] { f.name, current.name });
                                    continue;
                                }
                                for (ValueAndDescription vad : f.keys) {
                                    if (vad.value.equals(t.key)) {
                                        if ("*".equals(t.value)) {
                                            current.tags.remove(t); // remove the tag
                                        } else {
                                            // we can't actually check against the values here as the
                                            // taginfo queries haven't run yet
                                            current.fields.remove(f); // remove field
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                reader.endObject();
            }
            reader.endObject();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading preset: {0}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve and parse an iD field definitions file
     * 
     * @see <a href="https://github.com/ideditor/schema-builder/blob/main/schemas/field.json">iD field schema</a>
     * 
     * @param url url for the field file
     * @throws IOException if something goes wrong
     */
    private static void parseIdFields(@NotNull URL url) throws IOException {
        try (InputStream is = Utils.openConnection(url); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String fieldName = reader.nextName();
                Field current = new Field();
                fields.put(fieldName, current);
                current.name = fieldName;
                fieldKeys.put(current, fieldName);
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case "label":
                        current.label = reader.nextString();
                        break;
                    case "type":
                        current.fieldType = FieldType.valueOf(reader.nextString().toUpperCase());
                        break;
                    case "default":
                        current.defaultValue = reader.nextString();
                        break;
                    case "geometry":
                        reader.beginArray();
                        current.geometry = new ArrayList<>();
                        while (reader.hasNext()) {
                            current.geometry.add(Geometry.valueOf(reader.nextString().toUpperCase()));
                        }
                        reader.endArray();
                        break;
                    case "key":
                        current.keys = new ArrayList<>();
                        ValueAndDescription key = new ValueAndDescription();
                        key.value = reader.nextString();
                        current.keys.add(key);
                        break;
                    case "keys":
                        reader.beginArray();
                        current.keys = new ArrayList<>();
                        while (reader.hasNext()) {
                            key = new ValueAndDescription();
                            key.value = reader.nextString();
                            current.keys.add(key);
                        }
                        reader.endArray();
                        break;
                    case "options":
                        reader.beginArray();
                        current.options = new ArrayList<>();
                        while (reader.hasNext()) {
                            ValueAndDescription value = new ValueAndDescription();
                            value.value = reader.nextString();
                            value.description = fieldTranslations.get(optionsKey(current.name, value.value));
                            current.options.add(value);
                        }
                        reader.endArray();
                        break;
                    case "caseSensitive":
                        current.caseSensitive = reader.nextBoolean();
                        break;
                    case "snake_case":
                        current.snakeCase = reader.nextBoolean();
                        break;
                    default:
                        reader.skipValue();
                    }
                }
                reader.endObject();
                if (current.label == null) {
                    current.label = fieldTranslations.get(fieldName);
                }
            }
            reader.endObject();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading fields: {0}", e.getMessage());
            throw e;
        }
    }

    /**
     * Add a field to a preset item
     * 
     * @param itemFields the list of fields
     * @param tagKeys keys
     * @param fieldName field name
     */
    public static void addFields(@NotNull List<Field> itemFields, @NotNull List<String> tagKeys, @NotNull String fieldName) {
        if (fieldName.charAt(0) == '{' && fieldName.charAt(fieldName.length() - 1) == '}') {
            String refItemName = fieldName.substring(1, fieldName.length() - 1);
            Item refItem = items.get(refItemName);
            if (refItem != null) {
                List<Field> refFields = refItem.fields;
                for (Field f : refFields) {
                    boolean hasKey = false;
                    for (ValueAndDescription k : f.keys) {
                        if (tagKeys.contains(k.value)) { // don't overwrite
                            hasKey = true;
                            break;
                        }
                    }
                    if (hasKey) {
                        break;
                    }
                    itemFields.add(f);
                }
            }
        } else {
            Field field = fields.get(fieldName);
            if (field != null) {
                itemFields.add(field);
            }
        }
    }

    static void indent(PrintWriter writer, int times) {
        for (int i = 0; i < times; i++) {
            writer.print("    ");
        }
    }

    public static void main(String[] args) {
        // set up logging
        LogManager.getLogManager().reset();
        SimpleFormatter fmt = new SimpleFormatter();
        Handler stderrHandler = new FlushStreamHandler(System.err, fmt); // NOSONAR
        stderrHandler.setLevel(Level.INFO);
        LOGGER.addHandler(stderrHandler);

        OutputStream os = System.out; // NOSONAR

        Option outputFile = Option.builder(OUTPUT_OPT_SHORT).longOpt(OUTPUT_OPT_LONG).hasArg().desc("output .xml file, default: standard out").build();
        Option chunk = Option.builder(CHUNK_OPT_SHORT).longOpt(CHUNK_OPT_LONG).desc("output id fields as chunks").build();
        Option noTagInfo = Option.builder(NOTAGINFO_OPT_SHORT).longOpt(NOTAGINFO_OPT_LONG).desc("don't query taginfo for keys and values").build();
        Option josmOnlyOpt = Option.builder(JOSMONLY_OPT_SHORT).longOpt(JOSMONLY_OPT_LONG).desc("don't use Vespucci extensions").build();
        Option fieldsUrlOpt = Option.builder(FIELDSURL_OPT_SHORT).longOpt(FIELDSURL_OPT_LONG).hasArg().desc("url for alternative location of field definitions")
                .build();
        Option presetUrlOpt = Option.builder(PRESETURL_OPT_SHORT).longOpt(PRESETURL_OPT_LONG).hasArg()
                .desc("url for alternative location of preset definitions").build();
        Option translationUrlOpt = Option.builder(TRANSLATIONURL_OPT_SHORT).longOpt(TRANSLATIONURL_OPT_LONG).hasArg()
                .desc("url for alternative location of preset translations").build();

        Options options = new Options();

        options.addOption(outputFile);
        options.addOption(chunk);
        options.addOption(noTagInfo);
        options.addOption(josmOnlyOpt);
        options.addOption(fieldsUrlOpt);
        options.addOption(presetUrlOpt);
        options.addOption(translationUrlOpt);

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(OUTPUT_OPT_SHORT)) {
                String output = line.getOptionValue(OUTPUT_OPT_LONG);
                os = new FileOutputStream(output);
            }
            chunkMode = line.hasOption(CHUNK_OPT_SHORT);
            tagInfoMode = !line.hasOption(NOTAGINFO_OPT_SHORT);
            josmOnlyMode = line.hasOption(JOSMONLY_OPT_SHORT);
            if (line.hasOption(FIELDSURL_OPT_SHORT)) {
                fieldsUrl = line.getOptionValue(FIELDSURL_OPT_LONG);
            }
            if (line.hasOption(PRESETURL_OPT_SHORT)) {
                presetUrl = line.getOptionValue(PRESETURL_OPT_LONG);
            }
            if (line.hasOption(TRANSLATIONURL_OPT_SHORT)) {
                translationUrl = line.getOptionValue(TRANSLATIONURL_OPT_LONG);
            }
            convertId(new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(DEBUG_TAG, options);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found: {0}", e.getMessage());
        }
    }
}
