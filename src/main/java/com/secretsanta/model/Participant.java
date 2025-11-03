package com.secretsanta.model;

public class Participant {
    private String name;
    private String email; // Assuming this would be added later for the secure reveal

    public Participant(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}