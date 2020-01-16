package com.example.karwaan.Models;

import java.io.Serializable;
import java.util.ArrayList;

public class SongModel implements Serializable {

    private String url, songName, movie;
    private ArrayList<String> artists;

    public SongModel() {
    }

    public SongModel(String url, String songName, String movie, ArrayList<String> artists) {
        this.url = url;
        this.songName = songName;
        this.movie = movie;
        this.artists = artists;
    }

    public String getUrl() {
        return url;
    }

    public String getSongName() {
        return songName;
    }

    public String getMovie() {
        return movie;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }
}
