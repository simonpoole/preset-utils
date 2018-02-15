package ch.poole.osm.presetutils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringEscapeUtils;
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

    static final String DEBUG_TAG = "ID2JOSM";

    private static final int TIMEOUT = 20;

    enum Geometry {
        POINT, VERTEX, LINE, AREA, RELATION
    };

    enum FieldType {
        TEXT, NUMBER, LOCALIZED, TEL, EMAIL, URL, TEXTAREA, COMBO, TYPECOMBO, MULTICOMBO, NETWORKCOMBO, SEMICOMBO, CHECK, DEFAULTCHECK, ONEWAYCHECK, RADIO, STRUCTURERADIO, ACCESS, ADDRESS, CYCLEWAY, MAXSPEED, RESTRICTIONS, WIKIPEDIA
    };

    static class ValueAndDescription {
        String value;
        String description;
    }

    static class Field {
        String                    label;
        List<ValueAndDescription> keys;
        FieldType                 fieldType;
        Geometry                  geometry;
        boolean                   universal     = false;
        String                    defaultValue;
        String                    placeHolder;
        List<ValueAndDescription> options;
        boolean                   caseSensitive = false;
        boolean                   snakeCase     = true;

        public void toJosm(PrintWriter writer) {
            int baseIndent = 2;
            switch (fieldType) {
            case TEXT:
            case NUMBER:
            case LOCALIZED:
            case TEL:
            case EMAIL:
            case URL:
            case TEXTAREA:
            case ACCESS:
            case ADDRESS:
            case CYCLEWAY:
            case MAXSPEED:
            case RESTRICTIONS:
            case WIKIPEDIA:
                if (keys != null) {
                    for (ValueAndDescription key : keys) {
                        indent(writer, baseIndent);
                        if (keys.size() == 1 && label != null) {
                            writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label) + "\"" + fieldType2Attribute(fieldType) + " />");
                        } else {
                            writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\"" + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
                                    + fieldType2Attribute(fieldType) + " />");
                        }
                    }
                }
                break;
            case COMBO:
            case TYPECOMBO:
            case SEMICOMBO:
            case NETWORKCOMBO:
                if (keys != null) {
                    for (ValueAndDescription key : keys) {
                        if (options == null) {
                            optionsComment(writer, baseIndent);
                            options = getOptionsFromTagInfo(key.value);
                        }
                        indent(writer, baseIndent);
                        String labelText = key.description != null ? key.description : (label != null && keys.size() == 1 ? label : null);
                        writer.println("<" + (FieldType.SEMICOMBO.equals(fieldType) ? "multiselect" : "combo") + " key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\""
                                + (labelText != null ? " text=\"" + StringEscapeUtils.escapeXml11(labelText) + "\"" : ""));
                        indent(writer, baseIndent + 1);
                        writer.print("values=\"");
                        for (int i = 0; i < options.size(); i++) {
                            writer.print(StringEscapeUtils.escapeXml11(options.get(i).value));
                            if (i < options.size() - 1) {
                                writer.print(",");
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
                                writer.print(",");
                            }
                        }
                        writer.println("\" />");
                    }
                }
                break;
            case MULTICOMBO:
                if (keys != null && keys.size() == 1) {
                    ValueAndDescription key = keys.get(0);
                    if (options == null) {
                        optionsComment(writer, baseIndent);
                        options = getKeysFromTagInfo(key.value);
                    }
                    for (ValueAndDescription v : options) {
                        indent(writer, baseIndent);
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value + v.value) + "\"" + (v.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(v.description) + "\"" : "")
                                + " disable_off=\"true\" />");
                    }

                }
                break;
            case CHECK:
                if (keys != null && keys.size() == 1) {
                    ValueAndDescription key = keys.get(0);
                    indent(writer, baseIndent);
                    if (label != null) {
                        writer.println("<combo key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label) + "\"" + " values=\"yes,no\" />");
                    } else {
                        writer.println("<combo key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\"" + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
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
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\" text=\"" + StringEscapeUtils.escapeXml11(label) + "\" disable_off=\"true\" />");
                    } else {
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(key.value) + "\"" + (key.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(key.description) + "\"" : "")
                                + " disable_off=\"true\" />");
                    }
                }
                break;
            case RADIO:
            case STRUCTURERADIO:
                if (options != null) {
                    for (ValueAndDescription v : options) {
                        indent(writer, baseIndent);
                        writer.println("<check key=\"" + StringEscapeUtils.escapeXml11(v.value) + "\"" + (v.description != null ? " text=\"" + StringEscapeUtils.escapeXml11(v.description) + "\"" : "")
                                + " disable_off=\"true\" />");
                    }
                }
                break;
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
        }
    }

    static class Tag {
        String key;
        String value;
    }

    static class Item {
        String         name;
        List<Geometry> geometries;
        List<Tag>      tags;
        List<Tag>      addTags;
        List<Tag>      removeTags;
        List<Field>    fields;           // might use chunk references here
        boolean        searchable = true;

        public void toJosm(PrintWriter writer) {
            indent(writer, 1);
            writer.print("<item name=\"" + name + "\" ");
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
            writer.println((!searchable ? "deprecated=\"true\" " : "") + "preset_name_label=\"true\">");
            if (tags != null) {
                for (Tag tag : tags) {
                    if (tag.key != null && !"".equals(tag.key) && !tag.key.contains("*")) {
                        indent(writer, 2);
                        if (tag.value != null && !"".equals(tag.value) && !tag.value.contains("*")) {
                            writer.println("<key key=\"" + StringEscapeUtils.escapeXml11(tag.key) + "\" value=\"" + StringEscapeUtils.escapeXml11(tag.value) + "\" />");
                        } else { // generate a text field
                            writer.println("<text key=\"" + StringEscapeUtils.escapeXml11(tag.key) + "\" />");
                        }
                    }
                }
            }
            if (fields != null) {
                for (Field field : fields) {
                    if (chunkMode) {
                        indent(writer, 2);
                        writer.println("<reference ref=\"" + StringEscapeUtils.escapeXml11(fieldKeys.get(field)) + "\" />");
                    } else {
                        field.toJosm(writer);
                    }
                }
            }
            indent(writer, 1);
            writer.println("</item>");
        }
    }

    static Map<String, Field> fields = new HashMap<>();

    static Map<Field, String> fieldKeys = new HashMap<>();

    static List<Item> items = new ArrayList<>();

    private static boolean chunkMode = false;

    /**
     * @param printWriter
     * @return
     */
    static void convertId(PrintWriter printWriter) {
        // Log.d("DiscardedTags","Parsing configuration file");

        InputStream is = null;
        JsonReader reader = null;
        try {
            // field definitions read 1st
            // https://raw.githubusercontent.com/openstreetmap/iD/master/data/presets/fields.json
            URL url = new URL("https://raw.githubusercontent.com/openstreetmap/iD/master/data/presets/fields.json");
            is = openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                if (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("fields".equals(jsonName)) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            jsonName = reader.nextName();
                            Field current = new Field();
                            fields.put(jsonName, current);
                            fieldKeys.put(current, jsonName);
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
                                    current.geometry = Geometry.valueOf(reader.nextString().toUpperCase());
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
                                case "strings":
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        jsonName = reader.nextName();
                                        if ("options".equals(jsonName)) {
                                            current.options = new ArrayList<>();
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                String value = reader.nextName();
                                                ValueAndDescription v = new ValueAndDescription();
                                                v.value = value;
                                                JsonToken t = reader.peek();
                                                if (JsonToken.STRING.equals(t)) {
                                                    v.description = reader.nextString();
                                                } else if (JsonToken.BEGIN_OBJECT.equals(t)) {
                                                    reader.beginObject();
                                                    while (reader.hasNext()) {
                                                        jsonName = reader.nextName();
                                                        if ("title".equals(jsonName)) {
                                                            v.description = reader.nextString();
                                                        } else {
                                                            reader.skipValue();
                                                        }
                                                    }
                                                    reader.endObject();
                                                } else {
                                                    reader.skipValue();
                                                    continue;
                                                }
                                                current.options.add(v);
                                            }
                                            reader.endObject();
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                    break;
                                default:
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
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            // presets def
            // "https://raw.githubusercontent.com/openstreetmap/iD/master/data/presets/presets.json"
            url = new URL("https://raw.githubusercontent.com/openstreetmap/iD/master/data/presets/presets.json");
            is = openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                if (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("presets".equals(jsonName)) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            jsonName = reader.nextName();
                            Item current = new Item();
                            current.name = jsonName;
                            reader.beginObject();
                            boolean save = true;
                            while (reader.hasNext()) {
                                switch (reader.nextName()) {
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
                                        if ("name".equals(tag.key) && tag.value != null && !"".equals(tag.value)) {
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
                                    current.fields = new ArrayList<Field>();
                                    while (reader.hasNext()) {
                                        Field field = fields.get(reader.nextString());
                                        if (field != null) {
                                            current.fields.add(field);
                                        }
                                    }
                                    reader.endArray();
                                    break;
                                default:
                                    reader.skipValue();
                                }
                            }
                            if (save) {
                                items.add(current);
                            }
                            reader.endObject();
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();

                // print out
                printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                printWriter
                        .println("<presets xmlns=\"http://josm.openstreetmap.de/tagging-preset-1.0\"" + " shortdescription=\"iD presets\" description=\"\">");
                if (chunkMode) {
                    for (String fieldName : fields.keySet()) {
                        Field field = fields.get(fieldName);
                        if (field != null) {
                            indent(printWriter, 1);
                            printWriter.println("<chunk id=\"" + fieldName + "\">");
                            try {
                                field.toJosm(printWriter);
                            } catch (Exception ex) {
                                System.out.println("Exception for " + fieldName);
                                ex.printStackTrace();
                            }
                            indent(printWriter, 1);
                            printWriter.println("</chunk>");
                        }
                    }
                }
                for (Item item : items) {
                    try {
                        item.toJosm(printWriter);
                    } catch (Exception ex) {
                        System.out.println("Exception for " + item.name);
                        ex.printStackTrace();
                    }
                }
                printWriter.println("</presets>");
                printWriter.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioex) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioex) {
            }
        }
    }

    public static List<ValueAndDescription> getOptionsFromTagInfo(String key) {
        // "https://taginfo.openstreetmap.org/api/4/key/values?key=aerialway&page=1&rp=10&sortname=count_all&sortorder=desc"
        List<ValueAndDescription> result = new ArrayList<>();
        JsonReader reader = null;
        InputStream is = null;
        try {
            URL url = new URL("https://taginfo.openstreetmap.org/api/4/key/values?key=" + key + "&page=1&rp=20&sortname=count_all&sortorder=desc");
            is = openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("data".equals(jsonName)) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                jsonName = reader.nextName();
                                if ("value".equals(jsonName)) {
                                    ValueAndDescription v = new ValueAndDescription();
                                    v.value = reader.nextString();
                                    result.add(v);
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioex) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioex) {
            }
        }
        return result;
    }

    public static List<ValueAndDescription> getKeysFromTagInfo(String partialKey) {
        // "https://taginfo.openstreetmap.org/api/4/keys/all?query=communication:&page=1&rp=10&filter=in_wiki&sortname=key&sortorder=asc"
        List<ValueAndDescription> result = new ArrayList<>();
        JsonReader reader = null;
        InputStream is = null;
        try {
            URL url = new URL(
                    "https://taginfo.openstreetmap.org/api/4/keys/all?query=" + partialKey + "&sortname=count_all&sortorder=desc");
            is = openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("data".equals(jsonName)) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                jsonName = reader.nextName();
                                if ("key".equals(jsonName)) {
                                    String key = reader.nextString();
                                    if (key.startsWith(partialKey)) {
                                        String value = key.replaceFirst(partialKey, "");
                                        if (!value.contains(":")) {
                                            ValueAndDescription v = new ValueAndDescription();
                                            v.value = value;
                                            result.add(v);
                                        }
                                    }
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioex) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioex) {
            }
        }
        return result;
    }

    static void indent(PrintWriter writer, int times) {
        for (int i = 0; i < times; i++) {
            writer.print("    ");
        }
    }

    /**
     * Given an URL open the connection and return the InputStream
     * 
     * @param url the URL
     * @return the InputStream
     * @throws IOException
     */
    private static InputStream openConnection(@NotNull URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        boolean isServerGzipEnabled;

        // Log.d(DEBUG_TAG, "get input stream for " + url.toString());

        // --Start: header not yet send
        con.setReadTimeout(TIMEOUT * 1000);
        con.setConnectTimeout(TIMEOUT * 1000);
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", ID2JOSM.class.getCanonicalName());
        con.setInstanceFollowRedirects(true);

        // --Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Got " + con.getResponseMessage());
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());
        } else {
            return con.getInputStream();
        }
    }

    public static void main(String[] args) {
        OutputStream os = System.out;

        Option outputFile = Option.builder("o").longOpt("output").hasArg().desc("output .xml file, default: standard out").build();

        Option chunk = Option.builder("c").longOpt("chunk").desc("output id fields as chunks").build();

        Options options = new Options();

        options.addOption(outputFile);
        options.addOption(chunk);

        CommandLineParser parser = new DefaultParser();
        try {
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption("o")) {
                    String output = line.getOptionValue("output");
                    os = new FileOutputStream(output);
                }
                chunkMode = line.hasOption("c");
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ID2JOSM", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }
            convertId(new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // NOSONAR
            }
        }

    }
}
