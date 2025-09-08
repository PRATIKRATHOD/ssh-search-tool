package com.example.sshsearch;

public class LogEntry {
    private String file;
    private int line;
    private String timestamp;
    private String level;
    private String message;

    public LogEntry(String file, int line, String timestamp, String level, String message) {
        this.file = file;
        this.line = line;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getFile() { return file; }
    public int getLine() { return line; }
    public String getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
}
