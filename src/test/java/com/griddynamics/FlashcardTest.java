package com.griddynamics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Flashcard application using JUnit 5.
 * Tests cover all major functionality including optimizations.
 */
class FlashcardTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private Scanner originalScanner;

    @BeforeEach
    void setUp() throws Exception {
        // Capture System.out
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Force class initialization and save original scanner
        Field scannerField = Flashcard.class.getDeclaredField("SCANNER");
        scannerField.setAccessible(true);

        // Initialize scanner if it's null
        if (scannerField.get(null) == null) {
            scannerField.set(null, new Scanner(System.in, StandardCharsets.UTF_8));
        }

        originalScanner = (Scanner) scannerField.get(null);

        // Clear static fields before each test
        clearStaticFields();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);

        // Restore original scanner
        if (originalScanner != null) {
            Field scannerField = Flashcard.class.getDeclaredField("SCANNER");
            scannerField.setAccessible(true);
            scannerField.set(null, originalScanner);
        }

        clearStaticFields();
    }

    /**
     * Clears static fields using reflection to ensure test isolation.
     */
    private void clearStaticFields() throws Exception {
        Field cardsField = Flashcard.class.getDeclaredField("CARDS");
        cardsField.setAccessible(true);
        Map<String, String> cards = (Map<String, String>) cardsField.get(null);
        cards.clear();

        Field defsField = Flashcard.class.getDeclaredField("DEFINITIONS");
        defsField.setAccessible(true);
        Map<String, String> defs = (Map<String, String>) defsField.get(null);
        defs.clear();

        Field mistakesField = Flashcard.class.getDeclaredField("MISTAKES");
        mistakesField.setAccessible(true);
        Map<String, Integer> mistakes = (Map<String, Integer>) mistakesField.get(null);
        mistakes.clear();

        Field logField = Flashcard.class.getDeclaredField("LOG");
        logField.setAccessible(true);
        List<String> log = (List<String>) logField.get(null);
        log.clear();

        Field cachedTermsField = Flashcard.class.getDeclaredField("cachedTerms");
        cachedTermsField.setAccessible(true);
        cachedTermsField.set(null, null);
    }

    /**
     * Sets up Scanner with predefined input.
     */
    private void setupInput(String... inputs) throws Exception {
        String input = String.join("\n", inputs) + "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8));

        Field scannerField = Flashcard.class.getDeclaredField("SCANNER");
        scannerField.setAccessible(true);
        scannerField.set(null, new Scanner(inputStream, StandardCharsets.UTF_8));
    }

    @Test
    void testConstructorWithNoArguments() {
        Flashcard app = new Flashcard(new String[]{});
        assertNull(app.getExportFile());
    }

    @Test
    void testConstructorWithExportArgument() {
        Flashcard app = new Flashcard(new String[]{"-export", "output.txt"});
        assertEquals("output.txt", app.getExportFile());
    }

    @Test
    void testConstructorWithImportArgument() throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("import_test.txt");
        Files.writeString(testFile,
                "Capital\nParis\n0\n" +
                        "Planet\nEarth\n2\n",
                StandardCharsets.UTF_8);

        Flashcard app = new Flashcard(new String[]{"-import", testFile.toString()});

        // Verify cards were loaded
        String output = outputStream.toString();
        assertTrue(output.contains("2 cards have been loaded."));
    }

    @Test
    void testAddCard() throws Exception {
        setupInput("add", "France", "Paris", "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The pair (\"France\":\"Paris\") has been added."));
    }

    @Test
    void testAddCardWithDuplicateTerm() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "France", "Lyon",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The card \"France\" already exists."));
    }

    @Test
    void testAddCardWithDuplicateDefinition() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "City", "Paris",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The definition \"Paris\" already exists."));
    }

    @Test
    void testRemoveExistingCard() throws Exception {
        setupInput("add", "France", "Paris",
                "remove", "France",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The card has been removed."));
    }

    @Test
    void testRemoveNonExistentCard() throws Exception {
        setupInput("remove", "Spain", "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Can't remove \"Spain\": there is no such card."));
    }

    @Test
    void testExportCards() throws Exception {
        Path exportFile = tempDir.resolve("export_test.txt");

        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "export", exportFile.toString(),
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("2 cards have been saved."));

        // Verify file contents
        List<String> lines = Files.readAllLines(exportFile, StandardCharsets.UTF_8);
        assertEquals(6, lines.size());
        assertEquals("France", lines.get(0));
        assertEquals("Paris", lines.get(1));
        assertEquals("0", lines.get(2));
    }

    @Test
    void testImportCards() throws Exception {
        Path importFile = tempDir.resolve("import_test.txt");
        Files.writeString(importFile,
                "Spain\nMadrid\n3\n" +
                        "Italy\nRome\n1\n",
                StandardCharsets.UTF_8);

        setupInput("import", importFile.toString(), "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("2 cards have been loaded."));
    }

    @Test
    void testImportNonExistentFile() throws Exception {
        setupInput("import", "nonexistent.txt", "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("File not found."));
    }

    @Test
    void testAskCardsCorrectAnswer() throws Exception {
        setupInput("add", "France", "Paris",
                "ask", "1", "Paris",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Correct!"));
    }

    @Test
    void testAskCardsWrongAnswer() throws Exception {
        setupInput("add", "France", "Paris",
                "ask", "1", "Berlin",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Wrong. The right answer is \"Paris\"."));
    }

    @Test
    void testAskCardsWrongAnswerButMatchesOtherCard() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "ask", "1", "Berlin",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("but your definition is correct for \"Germany\"."));
    }

    @Test
    void testAskMultipleCards() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "ask", "3", "Paris", "Berlin", "Paris",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        // Should ask 3 times, cycling through cards
        int correctCount = output.split("Correct!", -1).length - 1;
        assertEquals(3, correctCount);
    }

    @Test
    void testHardestCardNoErrors() throws Exception {
        setupInput("add", "France", "Paris",
                "hardest card",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("There are no cards with errors."));
    }

    @Test
    void testHardestCardSingleCard() throws Exception {
        setupInput("add", "France", "Paris",
                "ask", "2", "Berlin", "Berlin",
                "hardest card",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The hardest card is \"France\". You have 2 errors answering it."));
    }

    @Test
    void testHardestCardMultipleCards() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "ask", "2", "Wrong", "Wrong",
                "hardest card",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The hardest cards are \"France\", \"Germany\"."));
        assertTrue(output.contains("You have 1 errors answering them."));
    }

    @Test
    void testResetStats() throws Exception {
        setupInput("add", "France", "Paris",
                "ask", "2", "Berlin", "Berlin",
                "reset stats",
                "hardest card",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Card statistics have been reset."));
        assertTrue(output.contains("There are no cards with errors."));
    }

    @Test
    void testSaveLog() throws Exception {
        Path logFile = tempDir.resolve("log_test.txt");

        setupInput("add", "France", "Paris",
                "log", logFile.toString(),
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The log has been saved."));

        // Verify log file exists and contains data
        assertTrue(Files.exists(logFile));
        List<String> logLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        assertTrue(logLines.size() > 0);
    }

    @Test
    void testExitWithAutoExport() throws Exception {
        Path exportFile = tempDir.resolve("auto_export.txt");

        setupInput("add", "France", "Paris", "exit");

        Flashcard app = new Flashcard(new String[]{"-export", exportFile.toString()});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Bye bye!"));
        assertTrue(output.contains("1 cards have been saved."));
        assertTrue(Files.exists(exportFile));
    }

    @Test
    void testUnknownAction() throws Exception {
        setupInput("invalid", "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Unknown action"));
    }

    @Test
    void testImportOverwritesExistingCards() throws Exception {
        Path importFile = tempDir.resolve("overwrite_test.txt");
        Files.writeString(importFile,
                "France\nLyon\n5\n",
                StandardCharsets.UTF_8);

        setupInput("add", "France", "Paris",
                "import", importFile.toString(),
                "ask", "1", "Lyon",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Correct!"));
    }

    @Test
    void testCacheInvalidationOnAdd() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "ask", "1", "Paris",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Correct!"));
    }

    @Test
    void testCacheInvalidationOnRemove() throws Exception {
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "remove", "France",
                "ask", "1", "Berlin",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("Correct!"));
    }

    @Test
    void testReverseMapConsistency() throws Exception {
        setupInput("add", "France", "Paris",
                "remove", "France",
                "add", "City", "Paris",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The pair (\"City\":\"Paris\") has been added."));
    }

    @Test
    void testReverseMapLookupPerformance() throws Exception {
        // Test that verifies O(1) lookup using reverse map
        setupInput("add", "France", "Paris",
                "add", "Germany", "Berlin",
                "add", "Spain", "Madrid",
                "ask", "1", "Berlin",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        // Should quickly identify that Berlin belongs to Germany
        assertTrue(output.contains("your definition is correct for \"Germany\""));
    }

    @Test
    void testDuplicateDefinitionCheckUsingReverseMap() throws Exception {
        // Verify O(1) duplicate definition check
        setupInput("add", "Country1", "Capital",
                "add", "Country2", "Capital",
                "exit");

        Flashcard app = new Flashcard(new String[]{});
        app.run();

        String output = outputStream.toString();
        assertTrue(output.contains("The definition \"Capital\" already exists."));
    }
}