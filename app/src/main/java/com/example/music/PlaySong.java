package com.example.music;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.ArrayList;

public class PlaySong extends AppCompatActivity {
    TextView textView;
    ImageView play, previous, next;
    ArrayList<File> songs;
    MediaPlayer mediaPlayer;
    String textContent;
    int position;
    SeekBar seekBar;
    Thread updateSeek;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    Handler handler;
    Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_song);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        textView = findViewById(R.id.textView2);
        play = findViewById(R.id.play);
        seekBar = findViewById(R.id.seekBar);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                }
                handler.postDelayed(this, 100); // update every 0.5 sec
            }
        };
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        sharedPreferences = getSharedPreferences("MediaPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        songs = (ArrayList) bundle.getParcelableArrayList("songList");
        textContent = intent.getStringExtra("currentSong");
        textView.setText(textContent);
        textView.setSelected(true);
        if (intent.hasExtra("position")) {
            position = intent.getIntExtra("position", 0);
        } else {
            position = sharedPreferences.getInt("position", 0);
        }
        Uri uri = Uri.fromFile(songs.get(position));
        mediaPlayer = MediaPlayer.create(this, uri);
        int lastSong = sharedPreferences.getInt("last_song", -1);
        int savedSeek = sharedPreferences.getInt("seek", 0);

        if (lastSong == position) {
            mediaPlayer.seekTo(savedSeek);
        } else {
            mediaPlayer.seekTo(0);
        }
        mediaPlayer.start();
        handler.post(runnable);
        setupCompletionListener();

        seekBar.setMax(mediaPlayer.getDuration());


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                handler.post(runnable);

            }
        });

        play.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                play.setImageResource(R.drawable.play);
                mediaPlayer.pause();
                handler.removeCallbacks(runnable);
            } else {
                play.setImageResource(R.drawable.pause);
                mediaPlayer.start();
                handler.post(runnable);
            }
            editor.putInt("position", position);
            editor.apply();
        });
        next.setOnClickListener(v -> {
            mediaPlayer.stop();
            mediaPlayer.release();

            if (position != songs.size() - 1) {
                position++;
            } else {
                position = 0;
            }

            Uri uri1 = Uri.fromFile(songs.get(position));
            mediaPlayer = MediaPlayer.create(getApplicationContext(), uri1);

            mediaPlayer.seekTo(0);
            mediaPlayer.start();

            seekBar.setMax(mediaPlayer.getDuration());

            textContent = songs.get(position).getName();
            textView.setText(textContent);

            editor.putInt("position", position);
            editor.putInt("seek", 0);
            editor.putInt("last_song", position);
            editor.apply();

            setupCompletionListener();
        });
        previous.setOnClickListener(v -> {
            mediaPlayer.stop();
            mediaPlayer.release();

            if (position != 0) {
                position--;
            } else {
                position = songs.size() - 1;
            }
            Uri uri1 = Uri.fromFile(songs.get(position));
            mediaPlayer = MediaPlayer.create(getApplicationContext(), uri1);

            mediaPlayer.seekTo(0);
            mediaPlayer.start();

            seekBar.setMax(mediaPlayer.getDuration());

            textContent = songs.get(position).getName();
            textView.setText(textContent);

            editor.putInt("position", position);
            editor.putInt("seek", 0);
            editor.putInt("last_song", position);
            editor.apply();

            setupCompletionListener();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            handler.post(runnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        editor.putInt("position", position);
        editor.putInt("last_song", position);
        editor.putInt("seek", mediaPlayer.getCurrentPosition());
        editor.apply();// 🔥 important

    }
    private void setupCompletionListener() {
        mediaPlayer.setOnCompletionListener(mp -> {

            handler.postDelayed(() -> {   // ⏳ 1 second delay

                if (position != songs.size() - 1) {
                    position++;
                } else {
                    position = 0;
                }

                mp.release();

                Uri uri1 = Uri.fromFile(songs.get(position));
                mediaPlayer = MediaPlayer.create(getApplicationContext(), uri1);

                mediaPlayer.seekTo(0);
                mediaPlayer.start();

                seekBar.setMax(mediaPlayer.getDuration());

                textContent = songs.get(position).getName();
                textView.setText(textContent);

                editor.putInt("position", position);
                editor.putInt("seek", 0);
                editor.putInt("last_song", position);
                editor.apply();

                setupCompletionListener();

            }, 1000); // ⏱ 1000 ms = 1 second
        });
    }

}