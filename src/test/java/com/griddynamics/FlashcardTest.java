package com.griddynamics;

import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardTest {

    private ByteArrayOutputStream out;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setup() throws Exception {
        originalOut = System.out;
        originalIn = System.in;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        clearStaticState();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    /* =========================================================
       Constructor & CLI args
       ========================================================= */

    @Test
    void exportFileIsSetFromArguments() {
        Flashcard flashcard = new Flashcard(new String[]{
                "-export", "cards.txt"
        });

        assertEquals("cards.txt", flashcard.getExportFile());
    }

    @Test
    void importFromFileLoadsCards() throws IOException {
        Path file = Files.createTempFile("cards", ".txt");
        Files.writeString(file, """
                Java
                Programming language
                2
                """);

        new Flashcard(new String[]{"-import", file.toString()});

        String output = out.toString();
        assertTrue(output.contains("1 cards have been loaded."));
    }

    /* =========================================================
       Add / Remove cards via run loop
       ========================================================= */

    @Test
    void addAndRemoveCard() {
        provideInput("""
                add
                Java
                Language
                remove
                Java
                exit
                """);

        new Flashcard(new String[]{}).run();

        String output = out.toString();
        assertTrue(output.contains("has been added"));
        assertTrue(output.contains("has been removed"));
    }

    @Test
    void duplicateTermIsRejected() {
        provideInput("""
                add
                Java
                Lang
                add
                Java
                Something
                exit
                """);

        new Flashcard(new String[]{}).run();

        assertTrue(out.toString().contains("already exists"));
    }

    /* =========================================================
       Ask / mistakes / hardest card
       ========================================================= */

    @Test
    void mistakesAreRecordedAndHardestCardReported() {
        provideInput("""
                add
                Java
                Lang
                ask
                1
                wrong
                hardest card
                exit
                """);

        new Flashcard(new String[]{}).run();

        String output = out.toString();
        assertTrue(output.contains("Wrong."));
        assertTrue(output.contains("hardest card is \"Java\""));
    }

    @Test
    void resetStatsClearsMistakes() {
        provideInput("""
                add
                Java
                Lang
                ask
                1
                wrong
                reset stats
                hardest card
                exit
                """);

        new Flashcard(new String[]{}).run();

        assertTrue(out.toString().contains("There are no cards with errors."));
    }

    /* =========================================================
       Export / Import
       ========================================================= */

    @Test
    void exportAndImportCards() throws Exception {
        Path file = Files.createTempFile("cards", ".txt");

        provideInput("""
                add
                Java
                Lang
                export
                %s
                exit
                """.formatted(file));

        new Flashcard(new String[]{}).run();

        assertTrue(Files.readString(file).contains("Java"));

        clearStaticState();
        out.reset();

        new Flashcard(new String[]{"-import", file.toString()});
        assertTrue(out.toString().contains("1 cards have been loaded."));
    }

    /* =========================================================
       Log saving
       ========================================================= */

    @Test
    void logIsSavedToFile() throws IOException {
        Path logFile = Files.createTempFile("log", ".txt");

        provideInput("""
                add
                Java
                Lang
                log
                %s
                exit
                """.formatted(logFile));

        new Flashcard(new String[]{}).run();

        String log = Files.readString(logFile);
        assertTrue(log.contains("Java"));
        assertTrue(log.contains("The log has been saved."));
    }

    /* =========================================================
       Helpers
       ========================================================= */

    private void provideInput(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes()));
    }

    private void clearStaticState() throws Exception {
        clearMap("CARDS");
        clearMap("MISTAKES");
        clearList();
    }

    private void clearMap(String fieldName) throws Exception {
        Field field = Flashcard.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    private void clearList() throws Exception {
        Field field = Flashcard.class.getDeclaredField("LOG");
        field.setAccessible(true);
        ((java.util.List<?>) field.get(null)).clear();
    }
}
