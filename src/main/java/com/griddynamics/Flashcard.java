package com.griddynamics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Supports add, remove, import, export, ask, log, hardest card and
 * reset stats operations. All console I/O is recorded into an internal
 * session log which may be saved to a file on demand.
 */
public class Flashcard {

    /**
     * Scanner used to read user input from standard input using UTF-8.
     */
    private static final Scanner SCANNER =
            new Scanner(System.in, StandardCharsets.UTF_8);

    /**
     * Map of term -> definition. LinkedHashMap is used to preserve
     * insertion order.
     */
    private static final Map<String, String> CARDS =
            new LinkedHashMap<>();

    /**
     * Map of term -> mistake count.
     */
    private static final Map<String, Integer> MISTAKES =
            new LinkedHashMap<>();

    /**
     * Internal console log that stores every printed line and every
     * user-entered line, in order.
     */
    private static final List<String> LOG = new ArrayList<>();

    /**
     * Optional file path to export flashcards when exiting.
     * Access via getter/setter to satisfy visibility rules.
     */
    private String exportFile = null;

    /**
     * Prints a message to the console and records it to the session log.
     *
     * @param message the text to print and log
     */
    private void logPrint(final String message) {
        System.out.println(message);
        LOG.add(message);
    }

    /**
     * Reads a line from standard input, logs it and returns it.
     *
     * @return the user-entered line
     */
    private String logInput() {
        final String s = SCANNER.nextLine();
        LOG.add(s);
        return s;
    }

    /**
     * Constructs the Flashcard application, processing optional CLI
     * arguments for initial import/export.
     *
     * @param args application arguments (may contain -import and -export)
     */
    public Flashcard(final String[] args) {

        String importFile = null;

        for (int i = 0; i < args.length; i++) {
            if ("-import".equals(args[i]) && i + 1 < args.length) {
                importFile = args[i + 1];
            }
            if ("-export".equals(args[i]) && i + 1 < args.length) {
                setExportFile(args[i + 1]);
            }
        }

        if (importFile != null) {
            importFromFile(importFile);
        }
    }

    /**
     * Returns the export file path (might be null).
     *
     * @return export file path or {@code null}
     */
    public String getExportFile() {
        return exportFile;
    }

    /**
     * Sets the export file path to use on exit.
     *
     * @param file export filename
     */
    public void setExportFile(final String file) {
        this.exportFile = file;
    }

    /**
     * Starts interactive loop of the application.
     */
    public void run() {

        while (true) {
            logPrint(
                    "Input the action (add, remove, import, export, ask, exit,"
                            + " log, hardest card, reset stats):");
            final String action = logInput();
            switch (action) {
                case "add" -> addCard();
                case "remove" -> removeCard();
                case "import" -> importCards();
                case "export" -> exportCards();
                case "ask" -> askCards();
                case "log" -> saveLog();
                case "hardest card" -> hardestCard();
                case "reset stats" -> resetStats();
                case "exit" -> {
                    logPrint("Bye bye!");
                    if (getExportFile() != null) {
                        exportToFile(getExportFile());
                    }
                    return;
                }
                default -> logPrint("Unknown action");
            }
        }
    }

    /**
     * Adds a card after prompting the user for term and definition.
     * Duplicate terms or definitions are rejected.
     */
    private void addCard() {
        logPrint("The card:");
        final String term = logInput();

        if (CARDS.containsKey(term)) {
            logPrint("The card \"" + term + "\" already exists.");
            return;
        }

        logPrint("The definition of the card:");
        final String definition = logInput();

        if (CARDS.containsValue(definition)) {
            logPrint("The definition \"" + definition + "\" already exists.");
            return;
        }

        CARDS.put(term, definition);
        MISTAKES.put(term, 0);
        logPrint("The pair (\"" + term + "\":\""
                + definition + "\") has been added.");
    }

    /**
     * Removes a card specified by the user.
     */
    private void removeCard() {
        logPrint("Which card?");
        final String term = logInput();

        if (CARDS.containsKey(term)) {
            CARDS.remove(term);
            MISTAKES.remove(term);
            logPrint("The card has been removed.");
        } else {
            logPrint("Can't remove \"" + term + "\": there is no such card.");
        }
    }

    /**
     * Prompts for filename and imports cards from that file.
     */
    private void importCards() {
        logPrint("File name:");
        importFromFile(logInput());
    }

    /**
     * Imports flashcards and mistake counts from a file encoded in UTF-8.
     * Each card is stored as three lines: term, definition, mistakes.
     *
     * @param fileName the file to import from
     */
    private void importFromFile(final String fileName) {
        try (Scanner fileScanner =
                     new Scanner(new File(fileName), StandardCharsets.UTF_8)) {
            int count = 0;
            while (fileScanner.hasNextLine()) {
                final String term = fileScanner.nextLine();
                final String definition = fileScanner.nextLine();
                final int mistakeCount =
                        Integer.parseInt(fileScanner.nextLine());

                CARDS.put(term, definition);
                MISTAKES.put(term, mistakeCount);
                count++;
            }
            logPrint(count + " cards have been loaded.");
        } catch (final IOException e) {
            logPrint("File not found.");
        }
    }

    /**
     * Prompts for filename and exports cards to that file.
     */
    private void exportCards() {
        logPrint("File name:");
        exportToFile(logInput());
    }

    /**
     * Writes all cards and mistake counts to the given file using UTF-8.
     *
     * @param fileName destination filename
     */
    private void exportToFile(final String fileName) {
        final Path path = Path.of(fileName);
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {

            for (final Map.Entry<String, String> entry : CARDS.entrySet()) {
                final String term = entry.getKey();
                final String definition = entry.getValue();
                writer.println(term);
                writer.println(definition);
                writer.println(MISTAKES.getOrDefault(term, 0));
            }
            logPrint(CARDS.size() + " cards have been saved.");
        } catch (final IOException e) {
            logPrint("Error writing file.");
        }
    }

    /**
     * Quiz the user about definitions for some number of cards.
     * Increase mistake count for wrong answers.
     */
    private void askCards() {
        logPrint("How many times to ask?");
        final int times = Integer.parseInt(logInput());
        final List<String> terms = new ArrayList<>(CARDS.keySet());

        for (int i = 0; i < times; i++) {
            final String term = terms.get(i % terms.size());
            final String correctDefinition = CARDS.get(term);

            logPrint("Print the definition of \"" + term + "\":");
            final String answer = logInput();

            if (answer.equals(correctDefinition)) {
                logPrint("Correct!");
            } else {
                final String otherTerm = findTermByDefinition(answer);
                MISTAKES.put(term, MISTAKES.getOrDefault(term, 0) + 1);

                if (otherTerm != null) {
                    logPrint("Wrong. The right answer is \""
                            + correctDefinition
                            + "\", but your definition is correct for \""
                            + otherTerm + "\".");
                } else {
                    logPrint("Wrong. The right answer is \""
                            + correctDefinition + "\".");
                }
            }
        }
    }

    /**
     * Finds the term that has the given definition.
     *
     * @param def definition to search for
     * @return term whose definition equals {@code def}, or {@code null}
     */
    private String findTermByDefinition(final String def) {
        for (final Map.Entry<String, String> entry : CARDS.entrySet()) {
            if (entry.getValue().equals(def)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Prompts for a filename and saves the in-memory console log to it
     * encoded in UTF-8.
     */
    private void saveLog() {
        logPrint("File name:");
        final String filename = logInput();
        final Path path = Path.of(filename);

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            for (final String line : LOG) {
                writer.println(line);
            }
            logPrint("The log has been saved.");
        } catch (final IOException e) {
            logPrint("Error saving log.");
        }
    }

    /**
     * Prints the card(s) with the highest mistake counts.
     * If no mistakes were recorded, prints a message accordingly.
     */
    private void hardestCard() {
        int max = 0;
        for (final int v : MISTAKES.values()) {
            max = Math.max(max, v);
        }

        if (max == 0) {
            logPrint("There are no cards with errors.");
            return;
        }

        final List<String> hardest = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : MISTAKES.entrySet()) {
            if (entry.getValue() == max) {
                hardest.add(entry.getKey());
            }
        }

        if (hardest.size() == 1) {
            logPrint("The hardest card is \"" + hardest.get(0)
                    + "\". You have " + max + " errors answering it.");
        } else {
            logPrint("The hardest cards are " + formatTerms(hardest)
                    + ". You have " + max + " errors answering them.");
        }
    }

    /**
     * Formats a list of terms as a quoted and comma-separated string.
     *
     * @param terms list of terms to format
     * @return formatted string like: "t1", "t2"
     */
    private String formatTerms(final List<String> terms) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            sb.append("\"").append(terms.get(i)).append("\"");
            if (i < terms.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Resets all recorded mistake counters to zero.
     */
    private void resetStats() {
        MISTAKES.replaceAll((k, v) -> 0);
        logPrint("Card statistics have been reset.");
    }
}
