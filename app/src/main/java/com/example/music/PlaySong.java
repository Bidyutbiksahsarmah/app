package com.example.music;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PlaySong extends AppCompatActivity {
    TextView textView;
    ImageView play, previous, next;
    SeekBar seekBar;
    
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private Handler handler = new Handler();
    private Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };

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

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class),
                connectionCallbacks,
                null);

        play.setOnClickListener(v -> {
            if (mediaController != null) {
                PlaybackStateCompat state = mediaController.getPlaybackState();
                if (state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.getTransportControls().pause();
                } else {
                    mediaController.getTransportControls().play();
                }
            }
        });

        next.setOnClickListener(v -> {
            if (mediaController != null) mediaController.getTransportControls().skipToNext();
        });

        previous.setOnClickListener(v -> {
            if (mediaController != null) mediaController.getTransportControls().skipToPrevious();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    mediaController.getTransportControls().seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(PlaySong.this) != null) {
            MediaControllerCompat.getMediaController(PlaySong.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
        handler.removeCallbacks(updateProgressAction);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mediaController = new MediaControllerCompat(PlaySong.this, mediaBrowser.getSessionToken());
                    MediaControllerCompat.setMediaController(PlaySong.this, mediaController);
                    mediaController.registerCallback(controllerCallback);

                    // If we came from MainActivity with a specific song, play it
                    Intent intent = getIntent();
                    if (intent != null && intent.hasExtra("position")) {
                        int position = intent.getIntExtra("position", 0);
                        mediaController.getTransportControls().playFromMediaId(String.valueOf(position), null);
                        intent.removeExtra("position"); // Prevent re-playing when activity is recreated or reconnected
                    }

                    // Sync initial UI state
                    updateUI(mediaController.getMetadata());
                    updatePlaybackState(mediaController.getPlaybackState());
                }
            };

    private final MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    updateUI(metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updatePlaybackState(state);
                }
            };

    private void updateUI(MediaMetadataCompat metadata) {
        if (metadata == null) return;
        textView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        seekBar.setMax(duration);
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) return;
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            play.setImageResource(R.drawable.pause);
            handler.post(updateProgressAction);
        } else {
            play.setImageResource(R.drawable.play);
            handler.removeCallbacks(updateProgressAction);
        }
    }

    private void updateProgress() {
        if (mediaController != null && mediaController.getPlaybackState() != null) {
            long position = mediaController.getPlaybackState().getPosition();
            seekBar.setProgress((int) position);
        }
    }
}
