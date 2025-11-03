package com.secretsanta.model;

public class MatchResult {
    private String gifter;
    private String recipient;

    public MatchResult(String gifter, String recipient) {
        this.gifter = gifter;
        this.recipient = recipient;
    }

    public String getGifter() { return gifter; }
    public void setGifter(String gifter) { this.gifter = gifter; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
}