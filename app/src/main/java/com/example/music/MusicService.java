package com.example.music;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MusicService extends Service {

    MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, "MusicService");

        mediaSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {

                // play song
            }

            @Override
            public void onPause() {
                // pause song
            }

            @Override
            public void onSkipToNext() {
                // next song
            }

            @Override
            public void onSkipToPrevious() {
                // previous song
            }
        });

        mediaSession.setActive(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}




