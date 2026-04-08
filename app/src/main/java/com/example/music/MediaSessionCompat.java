package com.example.music;

public class MediaSessionCompat {
    public MediaSessionCompat(MusicService musicService, String musicService1) {
    }

    public void setCallback(MediaSessionCompat.Callback callback) {
    }

    public void setActive(boolean b) {
    }

    public interface Callback {
        void onPlay();

        void onPause();

        void onSkipToNext();

        void onSkipToPrevious();
    }
}
