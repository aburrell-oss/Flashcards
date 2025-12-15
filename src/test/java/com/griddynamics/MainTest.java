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
 * Unit tests for the Main class.
 * Tests the entry point and argument handling.
 */
class MainTest {

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

        // Save and initialize original scanner
        Field scannerField = Flashcard.class.getDeclaredField("SCANNER");
        scannerField.setAccessible(true);

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
     * Clears static fields in Flashcard class to ensure test isolation.
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
    void testMainWithNoArguments() throws Exception {
        setupInput("exit");

        Main.main(new String[]{});

        String output = outputStream.toString();
        assertTrue(output.contains("Input the action"));
        assertTrue(output.contains("Bye bye!"));
    }

    @Test
    void testMainWithExportArgument() throws Exception {
        Path exportFile = tempDir.resolve("test_export.txt");
        setupInput("add", "TestCard", "TestDefinition", "exit");

        Main.main(new String[]{"-export", exportFile.toString()});

        String output = outputStream.toString();
        assertTrue(output.contains("Bye bye!"));
        assertTrue(output.contains("1 cards have been saved."));
        assertTrue(Files.exists(exportFile));
    }

    @Test
    void testMainWithImportArgument() throws Exception {
        Path importFile = tempDir.resolve("test_import.txt");
        Files.writeString(importFile,
                "ImportedCard\nImportedDefinition\n0\n",
                StandardCharsets.UTF_8);

        setupInput("exit");

        Main.main(new String[]{"-import", importFile.toString()});

        String output = outputStream.toString();
        assertTrue(output.contains("1 cards have been loaded."));
        assertTrue(output.contains("Bye bye!"));
    }

    @Test
    void testMainWithBothImportAndExport() throws Exception {
        Path importFile = tempDir.resolve("import.txt");
        Path exportFile = tempDir.resolve("export.txt");

        Files.writeString(importFile,
                "Card1\nDefinition1\n2\n" +
                        "Card2\nDefinition2\n1\n",
                StandardCharsets.UTF_8);

        setupInput("exit");

        Main.main(new String[]{
                "-import", importFile.toString(),
                "-export", exportFile.toString()
        });

        String output = outputStream.toString();
        assertTrue(output.contains("2 cards have been loaded."));
        assertTrue(output.contains("2 cards have been saved."));
        assertTrue(Files.exists(exportFile));

        // Verify exported content
        List<String> lines = Files.readAllLines(exportFile, StandardCharsets.UTF_8);
        assertEquals(6, lines.size());
        assertEquals("Card1", lines.get(0));
        assertEquals("Definition1", lines.get(1));
        assertEquals("2", lines.get(2));
    }

    @Test
    void testStartMethod() throws Exception {
        setupInput("exit");

        Main main = new Main();
        main.start(new String[]{});

        String output = outputStream.toString();
        assertTrue(output.contains("Input the action"));
        assertTrue(output.contains("Bye bye!"));
    }

    @Test
    void testStartMethodWithArguments() throws Exception {
        Path exportFile = tempDir.resolve("start_export.txt");
        setupInput("add", "Card", "Definition", "exit");

        Main main = new Main();
        main.start(new String[]{"-export", exportFile.toString()});

        String output = outputStream.toString();
        assertTrue(output.contains("The pair (\"Card\":\"Definition\") has been added."));
        assertTrue(output.contains("1 cards have been saved."));
        assertTrue(Files.exists(exportFile));
    }

    @Test
    void testMainClassIsFinal() {
        assertTrue(java.lang.reflect.Modifier.isFinal(Main.class.getModifiers()),
                "Main class should be final");
    }

    @Test
    void testMainMethodExists() throws NoSuchMethodException {
        // Verify main method exists with correct signature
        var mainMethod = Main.class.getDeclaredMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "main method should be static");
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "main method should be public");
        assertEquals(void.class, mainMethod.getReturnType(),
                "main method should return void");
    }

    @Test
    void testStartMethodExists() throws NoSuchMethodException {
        // Verify start method exists with correct signature
        var startMethod = Main.class.getDeclaredMethod("start", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(startMethod.getModifiers()),
                "start method should be public");
        assertEquals(void.class, startMethod.getReturnType(),
                "start method should return void");
    }

    @Test
    void testMainWithMultipleOperations() throws Exception {
        setupInput(
                "add", "France", "Paris",
                "add", "Germany", "Berlin",
                "ask", "1", "Paris",
                "hardest card",
                "exit"
        );

        Main.main(new String[]{});

        String output = outputStream.toString();
        assertTrue(output.contains("The pair (\"France\":\"Paris\") has been added."));
        assertTrue(output.contains("The pair (\"Germany\":\"Berlin\") has been added."));
        assertTrue(output.contains("Correct!"));
        assertTrue(output.contains("There are no cards with errors."));
        assertTrue(output.contains("Bye bye!"));
    }

    @Test
    void testMainWithInvalidImportFile() throws Exception {
        setupInput("exit");

        Main.main(new String[]{"-import", "nonexistent_file.txt"});

        String output = outputStream.toString();
        assertTrue(output.contains("File not found."));
        assertTrue(output.contains("Bye bye!"));
    }

    @Test
    void testMainArgumentParsing() throws Exception {
        // Test that arguments are parsed correctly even with extra args
        Path exportFile = tempDir.resolve("parsed_export.txt");
        setupInput("exit");

        Main.main(new String[]{
                "random",
                "-export", exportFile.toString(),
                "another_arg"
        });

        String output = outputStream.toString();
        assertTrue(output.contains("Bye bye!"));
        // Should create empty export file since no cards were added
        assertTrue(output.contains("0 cards have been saved."));
    }

    @Test
    void testMainCreatesNewInstanceEachTime() throws Exception {
        setupInput("exit");
        Main.main(new String[]{});
        String output1 = outputStream.toString();

        // Reset output stream
        outputStream.reset();

        setupInput("exit");
        Main.main(new String[]{});
        String output2 = outputStream.toString();

        // Both should have the same output pattern
        assertTrue(output1.contains("Bye bye!"));
        assertTrue(output2.contains("Bye bye!"));
    }

    @Test
    void testStartWithComplexWorkflow() throws Exception {
        Path importFile = tempDir.resolve("complex_import.txt");
        Path exportFile = tempDir.resolve("complex_export.txt");

        // Create import file with mistakes
        Files.writeString(importFile,
                "Capital\nParis\n3\n" +
                        "Country\nFrance\n5\n",
                StandardCharsets.UTF_8);

        setupInput(
                "add", "NewCard", "NewDefinition",
                "hardest card",
                "reset stats",
                "hardest card",
                "exit"
        );

        Main main = new Main();
        main.start(new String[]{
                "-import", importFile.toString(),
                "-export", exportFile.toString()
        });

        String output = outputStream.toString();
        assertTrue(output.contains("2 cards have been loaded."));
        assertTrue(output.contains("The hardest card is \"Country\". You have 5 errors answering it."));
        assertTrue(output.contains("Card statistics have been reset."));
        assertTrue(output.contains("There are no cards with errors."));
        assertTrue(output.contains("3 cards have been saved."));
    }
}