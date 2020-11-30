package ch.poole.osm.presetutils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Generate map from preset icon tags to actual JOSM presets
 * 
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class CreateJosmPresetIconMap {

    private static final String JOSM   = "josm";
    private static final String INPUT  = "input";
    private static final String OUTPUT = "output";

    public static void main(String[] args) {
        // defaults
        OutputStreamWriter os = null;
        InputStreamReader inputJosm = null;
        InputStreamReader inputTag = null;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            // arguments
            Option inputJosmFile = Option.builder("j").longOpt(JOSM).hasArg().desc("input JOSM icon list").build();

            Option inputTagFile = Option.builder("i").longOpt(INPUT).hasArg().desc("input tag icon list").build();

            Option outputFile = Option.builder("o").longOpt(OUTPUT).hasArg().desc("output stats file, default: standard out").build();

            Options options = new Options();

            options.addOption(inputJosmFile);
            options.addOption(inputTagFile);
            options.addOption(outputFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(JOSM)) {
                    inputJosm = new InputStreamReader(new FileInputStream(line.getOptionValue(JOSM)));
                }
                if (line.hasOption(INPUT)) {
                    inputTag = new InputStreamReader(new FileInputStream(line.getOptionValue(INPUT)));
                }
                if (line.hasOption(OUTPUT)) {
                    String output = line.getOptionValue(OUTPUT);
                    os = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("CreateJosmPresetList", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            Map<String, String> tagMap = new LinkedHashMap<>();
            readMap(inputTag, tagMap);

            Map<String, String> josmMap = new HashMap<>();
            readMap(inputJosm, josmMap);

            int mapped = 0;
            try (PrintWriter pw = new PrintWriter(os)) {
                for (Entry<String, String> entry : tagMap.entrySet()) {
                    String path = entry.getKey();
                    String tag = entry.getValue();
                    String josmIcon = josmMap.get(path);
                    if (josmIcon != null) {
                        pw.println(tag + "=" + josmIcon);
                        mapped++;
                    } else {
                        pw.println(tag + "=");
                    }
                }
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }

    }

    private static void readMap(InputStreamReader input, Map<String, String> map) throws IOException {
        try (BufferedReader reader = new BufferedReader(input)) {
            String line = reader.readLine();
            while (line != null) {
                String[] bits = line.split("\t");
                if (bits.length == 2) {
                    map.put(bits[0], bits[1]);
                } else {
                    map.put(bits[0], null);
                }
                line = reader.readLine();
            }
        }
    }
}
