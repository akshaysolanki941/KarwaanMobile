package com.example.karwaan.Models;

import java.util.ArrayList;

public class SongModel {

    private String url, songName;
    private ArrayList<String> artists;

    public SongModel() {
    }

    public SongModel(String url, String songName, ArrayList<String> artists) {
        this.url = url;
        this.songName = songName;
        this.artists = artists;
    }

    public String getUrl() {
        return url;
    }

    public String getSongName() {
        return songName;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }
}
