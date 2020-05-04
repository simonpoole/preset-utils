package ch.poole.osm.presetutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

import com.google.gson.stream.JsonReader;

/**
 * Retrieve synoyms from the iD repo
 * 
 * @author simon
 *
 */
public class Synonyms {

    static final String DEBUG_TAG = "Synonyms";

    private static final int TIMEOUT = 20;

    private static List<String> excludes;

    private static String base;

    /**
     * @param printWriter PrintWriter for the file to write to
     */
    static void getSynonyms(String lang, PrintWriter printWriter) {
        // Log.d("DiscardedTags","Parsing configuration file");

        // https://raw.githubusercontent.com/openstreetmap/iD/master/dist/locales/de.json
        InputStream is = null;
        JsonReader reader = null;
        try {
            if (!base.endsWith("/")) {
                base = base + "/";
            }
            URL url = new URL(base + lang + ".json");
            is = openConnection(url);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                if (reader.hasNext()) {
                    String jsonName = reader.nextName();
                    reader.beginObject();

                    while (reader.hasNext()) {
                        jsonName = reader.nextName();
                        if ("presets".equals(jsonName)) {
                            reader.beginObject();
                            printWriter.println("{");
                            while (reader.hasNext()) {
                                if ("presets".equals(reader.nextName())) {
                                    boolean first = true;
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String name = null;
                                        String terms = null;
                                        String presetName = reader.nextName();
                                        if (excludes != null) { // skip anything that is in excludes
                                            String[] parts = presetName.split("/");
                                            if (parts.length > 0) {
                                                boolean found = false;
                                                for (String part : parts) {
                                                    if (excludes.contains(part)) {
                                                        reader.skipValue();
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                if (found) {
                                                    continue;
                                                }
                                            }
                                        }
                                        reader.beginObject();
                                        jsonName = reader.nextName();
                                        if ("name".equals(jsonName)) { // FIXME make order independent
                                            name = reader.nextString();
                                        } else if ("terms".equals(jsonName)) { // in some cases name seems to be missing
                                            terms = reader.nextString();
                                        }
                                        if (reader.hasNext()) {
                                            jsonName = reader.nextName();
                                            if ("terms".equals(jsonName)) {
                                                terms = reader.nextString();
                                            }
                                        }
                                        reader.endObject();

                                        Set<String> set = new HashSet<>();
                                        if (terms != null && !"".equals(terms) && !terms.startsWith("<")) {
                                            String[] termsArray = terms.split("\\s*,\\s*");
                                            set.addAll(Arrays.asList(termsArray));
                                        }
                                        if (name != null && !"".equals(name)) {
                                            set.add(name);
                                        }
                                        if (set.size() > 0) {
                                            // output
                                            if (!first) {
                                                printWriter.write(",\n");
                                            }
                                            printWriter.write("\"" + presetName + "\":[\n");
                                            Iterator<String> iter = set.iterator();
                                            while (iter.hasNext()) {
                                                printWriter.write("\"" + iter.next() + "\"");
                                                if (iter.hasNext()) {
                                                    printWriter.write(",");
                                                }
                                                printWriter.write("\n");
                                            }
                                            printWriter.write("]");
                                            first = false;
                                        }
                                    }
                                    reader.endObject();
                                } else {
                                    reader.skipValue();
                                }
                            }
                            printWriter.println("\n}");
                            printWriter.flush();
                            reader.endObject();
                        } else {
                            reader.skipValue();
                        }

                    }
                    reader.endObject();
                }
                reader.endObject();
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
        con.setRequestProperty("User-Agent", Synonyms.class.getCanonicalName());
        con.setInstanceFollowRedirects(true);

        // --Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Synonyms got " + con.getResponseMessage());
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());
        } else {
            return con.getInputStream();
        }
    }

    public static void main(String[] args) {
        OutputStream os = System.out;
        String lang = "de";
        String output = null;
        // arguments
        Option baseUrl = Option.builder("u").longOpt("url").hasArg().desc("base url for the input files, required").required().build();

        Option inputFile = Option.builder("l").longOpt("lang").hasArg().desc("language to retrieve synonyms for").build();

        Option outputFile = Option.builder("o").longOpt("output").hasArg().desc("output .html file, default: standard out").build();

        Option exclude = Option.builder("x").longOpt("exclude").hasArgs().desc("one or more terms that should be excluded").build();
        
        Option remove = Option.builder("r").longOpt("remove").desc("remove empty output files").build();

        Options options = new Options();

        options.addOption(baseUrl);
        options.addOption(inputFile);
        options.addOption(outputFile);
        options.addOption(exclude);
        options.addOption(remove);

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("u")) {
                base = line.getOptionValue("url");
            }
            if (line.hasOption("l")) {
                lang = line.getOptionValue("lang");
            }
            if (line.hasOption("o")) {
                output = line.getOptionValue("output");
                os = new FileOutputStream(output);
            }
            if (line.hasOption("x")) {
                String[] tempExcludes = line.getOptionValues("exclude");
                if (tempExcludes != null) {
                    excludes = new ArrayList<>(Arrays.asList(tempExcludes));
                }
            }
            getSynonyms(lang, new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
            os.close();
            if (output != null && line.hasOption("r")) {
                File o = new File(output);
                if (o.length() == 0 && o.delete()) {
                    System.err.println("Nothing written to " + output + ", deleted.");
                }
            }
        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Synonyms", options);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage()); // NOSONAR
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
