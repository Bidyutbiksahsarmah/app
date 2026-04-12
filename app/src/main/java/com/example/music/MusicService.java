package com.example.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaPlayer mediaPlayer;
    private ArrayList<File> mySongs;
    private int currentPosition = 0;
    private boolean isMediaPlayerPrepared = false;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();

        // 1. Scan for songs
        mySongs = findSong(Environment.getExternalStorageDirectory());
        Collections.sort(mySongs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // 2. Initialize Media Session
        mediaSession = new MediaSessionCompat(this, "MusicService");
        
        // Define which actions the car can perform
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);

        mediaSession.setCallback(new MySessionCallback());
        setSessionToken(mediaSession.getSessionToken());

        // 3. Initialize Media Player
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        mediaPlayer.setOnCompletionListener(mp -> {
            mediaSession.getController().getTransportControls().skipToNext();
        });

        // Ensure the session is active so the car sees the app
        mediaSession.setActive(true);
        
        // Set initial state without calling mediaPlayer.getCurrentPosition() to avoid crash
        stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());

        // Pre-load metadata of the first song so the car screen isn't empty on connect
        if (mySongs != null && !mySongs.isEmpty()) {
            updateMetadata(0);
        }
    }

    private void updateMetadata(int position) {
        if (mySongs == null || position >= mySongs.size()) return;
        
        File song = mySongs.get(position);
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(position))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getName().replace(".mp3", ""))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Local Music");
        
        if (isMediaPlayerPrepared) {
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
        }
        
        mediaSession.setMetadata(metadataBuilder.build());
    }

    private boolean requestAudioFocus() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build();
            }
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaSession.getController().getTransportControls().pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                mediaSession.getController().getTransportControls().play();
                break;
        }
    };

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification() {
        MediaMetadataCompat metadata = mediaSession.getController().getMetadata();
        String title = (metadata != null) ? metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) : "Unknown";

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.music)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        builder.addAction(new NotificationCompat.Action(R.drawable.previous, "Prev",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

        if (mediaPlayer.isPlaying()) {
            builder.addAction(new NotificationCompat.Action(R.drawable.pause, "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(R.drawable.play, "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)));
        }

        builder.addAction(new NotificationCompat.Action(R.drawable.next, "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        return builder.build();
    }

    private ArrayList<File> findSong(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findSong(singleFile));
                } else if (singleFile.getName().endsWith(".mp3") && !singleFile.getName().startsWith(".")) {
                    arrayList.add(singleFile);
                }
            }
        }
        return arrayList;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (int i = 0; i < mySongs.size(); i++) {
            File song = mySongs.get(i);
            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(i))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getName().replace(".mp3", ""))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Local Music")
                    .build();
            mediaItems.add(new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(mediaItems);
    }

    private final class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (requestAudioFocus()) {
                if (isMediaPlayerPrepared) {
                    mediaPlayer.start();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    startForeground(NOTIFICATION_ID, getNotification());
                } else {
                    playSong(currentPosition);
                }
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            currentPosition = Integer.parseInt(mediaId);
            playSong(currentPosition);
        }

        @Override
        public void onPause() {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                stopForeground(false);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotification());
            }
        }

        @Override
        public void onSkipToNext() {
            currentPosition = (currentPosition + 1) % mySongs.size();
            playSong(currentPosition);
        }

        @Override
        public void onSkipToPrevious() {
            currentPosition = (currentPosition - 1 + mySongs.size()) % mySongs.size();
            playSong(currentPosition);
        }

        @Override
        public void onStop() {
            mediaPlayer.stop();
            isMediaPlayerPrepared = false;
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
            stopForeground(true);
            stopSelf();
        }

        @Override
        public void onSeekTo(long pos) {
            mediaPlayer.seekTo((int) pos);
            updatePlaybackState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        }
    }

    private void playSong(int position) {
        if (mySongs == null || mySongs.isEmpty()) return;
        if (!requestAudioFocus()) return;

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.fromFile(mySongs.get(position)));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isMediaPlayerPrepared = true;
            
            updateMetadata(position);
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            startForeground(NOTIFICATION_ID, getNotification());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePlaybackState(int state) {
        long pos = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (isMediaPlayerPrepared) {
            try { pos = mediaPlayer.getCurrentPosition(); } catch (Exception ignored) {}
        }

        stateBuilder.setState(state, pos, 1.0f);
        
        // Explicitly set actions again to ensure they are synced with the state
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
        mediaSession.release();
    }
}
