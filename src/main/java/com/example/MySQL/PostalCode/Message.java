package com.example.MySQL.PostalCode;

public class Message {
    private String content;

    // Default constructor
    public Message() {}

    // Constructor
    public Message(String content) {
        this.content = content;
    }

    // Getter and Setter
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
