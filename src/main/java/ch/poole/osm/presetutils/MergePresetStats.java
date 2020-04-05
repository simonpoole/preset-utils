package ch.poole.osm.presetutils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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
 * Merge output of PresetStats
 * 
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class MergePresetStats {

    private static final String INPUT  = "input";
    private static final String OUTPUT = "output";

    public static void main(String[] args) {
        // defaults
        OutputStreamWriter os = null;
        PrintWriter pw = null;
        String[] input = null;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            // arguments
            Option inputFile = Option.builder("i").longOpt(INPUT).hasArgs().desc("input preset file(s)").build();

            Option outputFile = Option.builder("o").longOpt(OUTPUT).hasArg().desc("output stats file, default: standard out").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(outputFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT)) {
                    input = line.getOptionValues(INPUT);
                }
                if (line.hasOption(OUTPUT)) {
                    String output = line.getOptionValue(OUTPUT);
                    os = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("MergedPresetStats", options);
                return;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                return;
            }

            Map<String, boolean[]> mergedStats = new HashMap<>();
            Map<String, Integer> mergedCounts = new HashMap<>();
            for (int i = 0; i < input.length; i++) {

                List<String> list = null;
                try {
                    list = Files.readAllLines(Paths.get(input[i]), StandardCharsets.UTF_8);
                } catch (MalformedInputException mie) {
                    System.out.println("Exception reading " + input[i]);
                    mie.printStackTrace();
                    continue;
                }
                for (String line : list) {
                    String[] v = line.split(",");
                    boolean[] stats = mergedStats.get(v[0]);
                    if (stats == null) {
                        stats = new boolean[input.length];
                        mergedStats.put(v[0], stats);
                    }
                    stats[i] = true;
                    Integer count = mergedCounts.get(v[0]);
                    try {
                        Integer countValue = Integer.valueOf(v[1]);
                        if (count == null || countValue > count) {
                            mergedCounts.put(v[0], countValue);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            pw = new PrintWriter(os);
            pw.print("Tag");
            for (String h : input) {
                pw.print(",");
                pw.print(h);
            }
            pw.print(",Count");
            pw.println();
            for (Entry<String, boolean[]> tags : mergedStats.entrySet()) {
                String key = tags.getKey();
                pw.print(key);
                for (boolean b : tags.getValue()) {
                    pw.print(",");
                    if (b) {
                        pw.print("X");
                    }
                }
                pw.print(",");
                pw.println(mergedCounts.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                // NOSONAR
            }
        }
    }
}
