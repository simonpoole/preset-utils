package ch.poole.osm.presetutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.stream.JsonReader;

public class TagInfo {

    /** the following is hardwired in iD **/
    static final Pattern canHaveUppercase = Pattern.compile("network|taxon|genus|species|brand|grape_variety|rating|:output|_hours|_times|royal_cypher");
    static final Pattern hasPunctuation   = Pattern.compile("[;,]");

    static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

    public static boolean isASCII(String v) {
        return asciiEncoder.canEncode(v);
    }

    /**
     * Private constructor
     */
    private TagInfo() {
        // empty
    }

    /**
     * This tries to do roughly the same as iD does when encountering an "options" section in a preset field
     * 
     * @param key
     * @param filter
     * @param useWiki
     * @param minCount
     * @param maxResults
     * @param multiSelect
     * @return
     */
    public static List<ValueAndDescription> getOptionsFromTagInfo(String key, String filter, boolean useWiki, int minCount, int maxResults,
            boolean multiSelect) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        // "https://taginfo.openstreetmap.org/api/4/key/values?key=aerialway&page=1&rp=10&sortname=count_all&sortorder=desc"
        boolean allowUppercase = canHaveUppercase.matcher(key).matches();
        Set<ValueAndDescription> values = new HashSet<>();
        JsonReader reader = null;
        InputStream is = null;
        try {
            String sortValue = "count_all";
            if (filter != null) {
                sortValue = "count_" + filter;
            }
            String paging = maxResults != 0 ? "&page=1&rp=" + maxResults : "";
            URL url = new URL("https://taginfo.openstreetmap.org/api/4/key/values?" + (filter != null ? "filter=" + filter + "&" : "") + "key=" + key + paging
                    + "&sortname=" + sortValue + "&sortorder=desc");
            System.err.println(url);
            is = Utils.openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("data".equals(jsonName)) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            String value = null;
                            boolean inWiki = false;
                            int count = 0;
                            double fraction = 0.0D;
                            while (reader.hasNext()) {
                                jsonName = reader.nextName();
                                switch (jsonName) {
                                case "value":
                                    value = reader.nextString().trim();
                                    break;
                                case "in_wiki":
                                    inWiki = reader.nextBoolean();
                                    break;
                                case "count":
                                    count = reader.nextInt();
                                    break;
                                case "fraction":
                                    fraction = reader.nextDouble();
                                    break;
                                default:
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                            if (value != null && ((inWiki && useWiki) || (count >= minCount && fraction > 0.0))
                                    && (allowUppercase || (value.equals(value.toLowerCase()) && value.matches("^[^\\*\\=\\;\\?]+$"))) && isASCII(value)
                                    && !hasPunctuation.matcher(value).matches()) {
                                if (multiSelect && value.contains(";")) {
                                    // currently not used as the regexp throws out string containing ;
                                    for (String s : value.split(";")) {
                                        ValueAndDescription vad = new ValueAndDescription();
                                        vad.value = s.trim();
                                        vad.count = count;
                                        values.add(vad);
                                        System.err.println(vad.value);
                                    }
                                } else {
                                    ValueAndDescription vad = new ValueAndDescription();
                                    vad.value = value.trim();
                                    vad.count = count;
                                    values.add(vad);
                                    System.err.println(vad.value);
                                }
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
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
        return new ArrayList<>(values);
    }

    public static List<ValueAndDescription> getKeysFromTagInfo(String partialKey) {
        // "https://taginfo.openstreetmap.org/api/4/keys/all?query=communication:&page=1&rp=10&filter=in_wiki&sortname=key&sortorder=asc"
        List<ValueAndDescription> result = new ArrayList<>();
        JsonReader reader = null;
        InputStream is = null;
        try {
            URL url = new URL("https://taginfo.openstreetmap.org/api/4/keys/all?query=" + partialKey + "&sortname=count_all&sortorder=desc&page=1&rp=25");
            is = Utils.openConnection(url);
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
                                    String key = reader.nextString().trim();
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
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
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

    public static int getTagCount(String key, String value) {
        // try not to overload taginfo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        // "https://taginfo.openstreetmap.org/api/4/tag/stats?key=amenity&value=school"
        int count = 0;
        JsonReader reader = null;
        InputStream is = null;
        try {
            URL url = new URL("https://taginfo.openstreetmap.org/api/4/tag/stats?key=" + key + "&value=" + value);
            is = Utils.openConnection(url);
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
                                if ("type".equals(jsonName)) {
                                    String type = reader.nextString();
                                    if ("all".equals(type)) {
                                        while (reader.hasNext()) {
                                            jsonName = reader.nextName();
                                            if ("count".equals(jsonName)) {
                                                count = reader.nextInt();
                                            } else {
                                                reader.skipValue();
                                            }
                                        }
                                    } else { // skip the object
                                        while (reader.hasNext()) {
                                            jsonName = reader.nextName();
                                            reader.skipValue();
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
                System.err.println(e.getMessage());
            } catch (IllegalStateException e) {
                System.err.println(e.getMessage());
                System.err.println(url);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
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
        return count;
    }

    public static List<ValueAndDescription> getCombinationKeys(String key, String filter, int minCount) {
        // "https://taginfo.openstreetmap.org/api/4/key/combinations?key=highway&page=1&rp=10&sortname=together_count&sortorder=desc"
        try {
            Thread.sleep(500);
        } catch (InterruptedException e1) {
        }
        List<ValueAndDescription> result = new ArrayList<>();
        JsonReader reader = null;
        InputStream is = null;
        try {
            URL url = new URL("https://taginfo.openstreetmap.org/api/4/key/combinations?" + (filter != null ? "filter=" + filter + "&" : "") + "key=" + key
                    + "&sortname=together_count&sortorder=desc");
            is = Utils.openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    if ("data".equals(jsonName)) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            String otherKey = null;
                            int togetherCount = 0;
                            while (reader.hasNext()) {
                                jsonName = reader.nextName();
                                if ("other_key".equals(jsonName)) {
                                    otherKey = reader.nextString().trim();
                                } else if ("together_count".equals(jsonName)) {
                                    togetherCount = reader.nextInt();
                                } else {
                                    reader.skipValue();
                                }
                            }
                            if (togetherCount >= minCount) {
                                ValueAndDescription v = new ValueAndDescription();
                                v.value = otherKey;
                                result.add(v);
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
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
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
}
