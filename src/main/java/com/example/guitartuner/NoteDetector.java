package com.example.guitartuner;

/**
 * @author Lucía Miguel González
 * TFG 24/25
 */

public class NoteDetector {

    private static final double TOLERANCE = 2.0; //allowed frequency deviation
    
    public static DetectedNote detect(double frequency) {
        StandardTunningNoteRange closestNote = null;
        double minDiff = Double.MAX_VALUE;

        for (StandardTunningNoteRange note : StandardTunningNoteRange.values()) {
            double diff = Math.abs(frequency - note.getFrequency());
            if (diff < minDiff) {
                minDiff = diff;
                closestNote = note;
            }
        }

        if (closestNote == null) return null;

        double delta = frequency - closestNote.getFrequency();
        String status;

        if (Math.abs(delta) <= TOLERANCE) {
            status = "Afinada";
        } else if (delta < 0) {
            status = "Demasiado baja";
        } else {
            status = "Demasiado alta";
        }

        return new DetectedNote(closestNote.getNoteName(), closestNote.getFrequency(), frequency, status);
    }
}