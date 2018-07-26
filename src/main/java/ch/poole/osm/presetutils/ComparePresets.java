package ch.poole.osm.presetutils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Compare the output of PresetStats for two presets
 * 
 * 
 * Licence Apache 2.0
 * 
 * @author Simon Poole
 *
 */

public class ComparePresets {

    private static final String REFERENCE = "reference";
    private static final String INPUT     = "input";

    public static void main(String[] args) {
        // defaults
        OutputStreamWriter os = null;
        PrintWriter pw = null;
        String input = null;
        String reference = null;
        try {
            os = new OutputStreamWriter(System.out, "UTF-8");

            // arguments
            Option inputFile = Option.builder("i").longOpt(INPUT).hasArg().desc("input preset file").build();

            Option referenceFile = Option.builder("r").longOpt(REFERENCE).hasArg().desc("input reference file").build();

            Options options = new Options();

            options.addOption(inputFile);
            options.addOption(referenceFile);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption(INPUT)) {
                    input = line.getOptionValue(INPUT);
                }
                if (line.hasOption(REFERENCE)) {
                    reference = line.getOptionValue(REFERENCE);
                }
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ComparePresets", options);
                return;
            }

            List<String> referenceList = null;
            List<String> inputList = null;
            try {
                referenceList = Files.readAllLines(Paths.get(reference), StandardCharsets.UTF_8);
            } catch (MalformedInputException mie) {
                System.out.println("Exception reading " + input);
            }
            try {
                inputList = Files.readAllLines(Paths.get(input), StandardCharsets.UTF_8);
            } catch (MalformedInputException mie) {
                System.out.println("Exception reading " + input);
            }

            Set<String> referenceTags = new HashSet<>();
            for (String line : referenceList) {
                String[] v = line.split(",");
                referenceTags.add(v[0]);
            }

            Set<String> inputTags = new HashSet<>();
            for (String line : inputList) {
                String[] v = line.split(",");
                inputTags.add(v[0]);
            }

            int inReference = 0;
            int notInReference = 0;

            for (String tag : inputTags) {
                if (referenceTags.contains(tag)) {
                    inReference++;
                } else {
                    notInReference++;
                }
            }

            pw = new PrintWriter(os);
            pw.println("Total tags in reference " + referenceTags.size());
            pw.println("Total tags in input preset " + inputTags.size());
            pw.println("Tags in input preset and reference " + inReference + " " + (((float) inReference) / referenceTags.size()) * 100 + "%");
            pw.println("Tags in input preset not in reference " + notInReference);
            pw.flush();
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
