package com.example.guitartuner;
/**
 * @author Lucía Miguel González
 * TFG 24/25
 */

public class DetectedNote {
    public final String noteName;
    public final double targetFrequency;
    public final double actualFrequency;
    public final String status;

    public DetectedNote(String noteName, double targetFrequency, double actualFrequency, String status) {
        this.noteName = noteName;
        this.targetFrequency = targetFrequency;
        this.actualFrequency = actualFrequency;
        this.status = status;
    }
}
