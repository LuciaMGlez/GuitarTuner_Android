package com.example.guitartuner;

/**
 * @author Lucía Miguel González
 * TFG 24/25
 */

//Enum that represents the standard tuning notes for a 6-string guitar each constant holds the note name and its fundamental frequency

public enum StandardTunningNoteRange {
    E2("E2", 82.41),
    A2("A2", 110.00),
    D3("D3", 146.83),
    G3("G3", 196.00),
    B3("B3", 246.94),
    E4("E4", 329.63);

    private final String noteName;
    private final double frequency;

    StandardTunningNoteRange(String name, double frequency) {
        this.noteName = name;
        this.frequency = frequency;
    }

    //Gets the musical note name
    public String getNoteName() {
        return noteName;
    }

    //Gets the fundamental frequency of the note
    public double getFrequency() {
        return frequency;
    }
}