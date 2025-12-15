package com.griddynamics;

public final class Main {
    /**
     * A console-based flashcard application that supports adding, removing,
     * importing, exporting, quizzing, logging, and tracking error statistics.
     * @param args
     */
    public void start(final String[] args) {
        new Flashcard(args).run();
    }

    /**
     * Main entry point of the program.
     * @param args
     */
    public static void main(final String[] args) {
        new Main().start(args);
    }
}
