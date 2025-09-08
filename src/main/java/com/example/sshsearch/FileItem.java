package com.example.sshsearch;

public class FileItem {
    private String name;
    private String size;
    private String type;

    public FileItem(String name, String size, String type) {
        this.name = name;
        this.size = size;
        this.type = type;
    }

    public String getName() { return name; }
    public String getSize() { return size; }
    public String getType() { return type; }
}
