package com.example.karwaan.Models;

public class SongModel {

    private String url, songName, artist;

    public SongModel() {
    }

    public SongModel(String url, String songName, String artist) {
        this.url = url;
        this.songName = songName;
        this.artist = artist;
    }

    public String getUrl() {
        return url;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtist() {
        return artist;
    }
}
