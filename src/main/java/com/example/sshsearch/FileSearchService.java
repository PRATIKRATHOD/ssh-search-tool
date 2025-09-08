package com.example.sshsearch;

public class FileSearchService {

    private final SSHConnector connector;

    public FileSearchService(SSHConnector connector) {
        this.connector = connector;
    }

    /**
     * Search keyword inside .log files under given path (non-recursive recursion depends on grep flags).
     */
    public String searchLogs(String keyword, String path) throws Exception {
        // Use grep recursively for portability. If ripgrep (rg) is installed you can replace this with rg.
        String safeKeyword = keyword.replace("'", "'\"'\"'"); // basic escaping of single quote
        String command = "grep -R --line-number --include='*.log' '" + safeKeyword + "' '" + path + "' 2>/dev/null || echo 'No matching logs found'";
        return connector.runCommand(command);
    }

    /**
     * Return newline-separated directories (with trailing slash).
     */
    public String listDirectories(String path) throws Exception {
        // ls -1p will append / to dirs, grep '/$' keeps only directories
        String command = "ls -1p '" + path + "' 2>/dev/null | grep '/$' || true";
        return connector.runCommand(command);
    }

    /**
     * Return newline-separated files (no trailing slash).
     */
    public String listFiles(String path) throws Exception {
        // ls -1p lists items; grep -v '/$' removes directories
        String command = "ls -1p '" + path + "' 2>/dev/null | grep -v '/$' || true";
        return connector.runCommand(command);
    }

    /**
     * Read first N lines of a file to avoid freezing for huge files.
     */
    public String readFileContent(String filePath) throws Exception {
        // limit to first 2000 lines (adjust if needed)
        String command = "head -n 2000 '" + filePath + "' 2>/dev/null || echo 'Unable to read file or file too large'";
        return connector.runCommand(command);
    }
}
