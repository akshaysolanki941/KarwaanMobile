package com.example.karwaan.Models;

public class RadioModel {

    private String radioName, radioUrl;

    RadioModel() {
    }

    public RadioModel(String radioName, String radioUrl) {
        this.radioName = radioName;
        this.radioUrl = radioUrl;
    }

    public String getRadioName() {
        return radioName;
    }

    public String getRadioUrl() {
        return radioUrl;
    }
}
