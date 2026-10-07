package com.example.guitartuner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author Lucía Miguel González
 * TFG 24/25
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView noteTv, freqTv;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private Button s1,s2, s3, s4, s5, s6, selBut;
    private StandardTunningNoteRange noteRange;
    private ImageView pointer;
    private AudioAnalyzer audioAnalyzer;
    private Thread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        noteTv = findViewById(R.id.tvNote);
        freqTv = findViewById(R.id.tvFreq);
        s1 = findViewById(R.id.btns1);
        s2 = findViewById(R.id.btns2);
        s3 = findViewById(R.id.btns3);
        s4 = findViewById(R.id.btns4);
        s5 = findViewById(R.id.btns5);
        s6 = findViewById(R.id.btns6);
        pointer = findViewById(R.id.pointer);

        noteRange = null;
        noteTv.setText("");
        freqTv.setText(" ");

        for (Button button : Arrays.asList(s1, s2, s3, s4, s5, s6)){
            button.setOnClickListener(this);
        }

        requestRecordAudioPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permissionToRecordAccepted) {
                startAudio();
            }
        }
    }

    private void requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
            startAudio();
        }
    }

    private void startAudio() {
        try {
            audioAnalyzer = new AudioAnalyzer();
            audioAnalyzer.setPitchListener((frequency, intensity) -> {
                runOnUiThread(() -> {
                    if (noteRange == null) return;

                    freqTv.setText(String.format(Locale.getDefault(), "%.2f Hz", frequency));
                    noteTv.setText(noteRange.getNoteName());

                    DetectedNote detectedNote = NoteDetector.detect(frequency);
                    if (detectedNote != null && detectedNote.noteName.equals(noteRange.getNoteName())) {
                        if ("Afinada".equals(detectedNote.status)) {
                            freqTv.setTextColor(Color.GREEN);
                        }
                        else{
                            freqTv.setTextColor(Color.RED);
                        }
                    }

                    double delta = frequency - noteRange.getFrequency();
                    refreshPointer(delta);
                });
            });

            audioAnalyzer.launch();
            audioThread = new Thread(audioAnalyzer);
            audioThread.start();

        } catch (SecurityException e) {
            e.getMessage();
        }
    }

    private void playStringSound(int id) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, id);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    private void refreshPointer(double pitchOffset) {
        ConstraintLayout displayArea = findViewById(R.id.main);
        int maxShiftPixels = 600;
        double upperPitchBound = 50.0;
        double lowerPitchBound = -50.0;

        double pitchRange = upperPitchBound - lowerPitchBound;
        double calculatedShift = (pitchOffset - lowerPitchBound) / pitchRange * (2 * maxShiftPixels) - maxShiftPixels;

        int displayWidth = displayArea.getWidth();
        int needleWidth = pointer.getWidth();
        int maxAllowedShift = displayWidth - needleWidth;

        calculatedShift = Math.max(-maxAllowedShift / 2.0, Math.min(maxAllowedShift / 2.0, calculatedShift));
        float centerPositionX = displayArea.getX() + displayWidth / 2f;
        float finalX = centerPositionX + (float) calculatedShift;

        pointer.setTranslationX(finalX);
        pointer.animate()
                .translationX((float) calculatedShift)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(0.5f))
                .start();
    }

    @SuppressLint("ResourceType")
    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            for (Button b : Arrays.asList(s1, s2, s3, s4, s5, s6)) {
                b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
            }
            selBut = (Button) v;
            selBut.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ed94ff")));

            int sound = 0;

            if(selBut == s1){
                noteRange = StandardTunningNoteRange.E2;
                sound = R.raw.e2;
            } else if (selBut == s2){
                noteRange = StandardTunningNoteRange.A2;
                sound = R.raw.a;
            } else if (selBut == s3){
                noteRange = StandardTunningNoteRange.D3;
                sound = R.raw.d;
            } else if (selBut == s4) {
                noteRange = StandardTunningNoteRange.G3;
                sound = R.raw.g;
            } else if (selBut == s5){
                noteRange = StandardTunningNoteRange.B3;
                sound = R.raw.b;
            } else if (selBut == s6){
                noteRange = StandardTunningNoteRange.E4;
                sound = R.raw.e4;
            }

            noteTv.setText(noteRange.getNoteName());
            if(sound != 0){
                playStringSound(sound);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioAnalyzer != null) {
            audioAnalyzer.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionToRecordAccepted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startAudio();
            }
        }
    }
}