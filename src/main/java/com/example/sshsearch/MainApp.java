package com.example.sshsearch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainApp extends Application {

    private TextField hostField, portField, userField, keywordField, pathField;
    private PasswordField passField;
    private TextArea resultArea;
    private ListView<String> dirListView;
    private Label statusLabel;

    private TableView<LogResult> logTable;
    private ObservableList<LogResult> logData;

    private SSHConnector sshConnector;
    private FileSearchService fileService;

    @Override
    public void start(Stage stage) {
        // Input fields
        hostField = new TextField("localhost");
        portField = new TextField("22");
        userField = new TextField("pratik");
        passField = new PasswordField();
        passField.setPromptText("Ubuntu password");
        keywordField = new TextField("ERROR");
        pathField = new TextField("/home/pratik/");

        // Buttons
        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        Button searchButton = new Button("🔍 Search Logs");
        Button listDirButton = new Button("📂 List Directory");
        Button clearButton = new Button("🧹 Clear");

        // Directory & file list
        dirListView = new ListView<>();
        dirListView.setPrefWidth(320);

        // Result area (for file content only)
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: lime;");

        // TableView for search results
        logTable = new TableView<>();
        logData = FXCollections.observableArrayList();
        TableColumn<LogResult, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().file));

        TableColumn<LogResult, String> lineCol = new TableColumn<>("Line");
        lineCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().line));

        TableColumn<LogResult, String> contentCol = new TableColumn<>("Content");
        contentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().content));

        TableColumn<LogResult, String> tsCol = new TableColumn<>("Timestamp");
        tsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().timestamp));
        tsCol.setPrefWidth(160);    

        fileCol.setPrefWidth(250);
        lineCol.setPrefWidth(70);
        contentCol.setPrefWidth(600);

        logTable.getColumns().addAll(fileCol, lineCol, tsCol, contentCol);
        logTable.setItems(logData);

        // Status bar
        statusLabel = new Label("Disconnected");
        statusLabel.setTextFill(Color.DARKGRAY);

        // Layout
        GridPane formGrid = new GridPane();
        formGrid.setHgap(8);
        formGrid.setVgap(8);
        formGrid.setPadding(new Insets(10));
        formGrid.add(new Label("Host:"), 0, 0);
        formGrid.add(hostField, 1, 0);
        formGrid.add(new Label("Port:"), 2, 0);
        formGrid.add(portField, 3, 0);
        formGrid.add(new Label("User:"), 0, 1);
        formGrid.add(userField, 1, 1);
        formGrid.add(new Label("Password:"), 2, 1);
        formGrid.add(passField, 3, 1);
        formGrid.add(new Label("Path:"), 0, 2);
        formGrid.add(pathField, 1, 2, 3, 1);
        formGrid.add(new Label("Keyword:"), 0, 3);
        formGrid.add(keywordField, 1, 3);

        HBox topButtons = new HBox(8, connectButton, disconnectButton, listDirButton, searchButton, clearButton);
        topButtons.setPadding(new Insets(6));

        VBox leftBox = new VBox(8, new Label("Directories / Files"), dirListView);
        leftBox.setPadding(new Insets(8));
        leftBox.setPrefWidth(360);

        VBox fileBox = new VBox(4, new Label("File Content"), resultArea);
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        VBox searchBox = new VBox(4, new Label("Search Results"), logTable);
        VBox.setVgrow(logTable, Priority.ALWAYS);

        VBox rightBox = new VBox(12, fileBox, searchBox);
        rightBox.setPadding(new Insets(8));

        // Existing UI (file explorer + search)
        SplitPane splitPane = new SplitPane(leftBox, rightBox);
        splitPane.setDividerPositions(0.35);

        BorderPane explorerRoot = new BorderPane();
        explorerRoot.setTop(new VBox(formGrid, topButtons));
        explorerRoot.setCenter(splitPane);
        explorerRoot.setBottom(statusLabel);

        Tab explorerTab = new Tab("Explorer");
        explorerTab.setContent(explorerRoot);
        explorerTab.setClosable(false);

        // New terminal tab
        Tab terminalTab = createTerminalTab();

        // TabPane with both
        TabPane tabPane = new TabPane(explorerTab, terminalTab);

        Scene scene = new Scene(tabPane, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("SSH Log Search & Browser Tool");
        stage.show();

        // Actions
        connectButton.setOnAction(e -> connect());
        disconnectButton.setOnAction(e -> disconnect());
        listDirButton.setOnAction(e -> listDirectoryContents(pathField.getText()));
        searchButton.setOnAction(e -> searchLogs());
        clearButton.setOnAction(e -> {
            resultArea.clear();
            logData.clear();
        });

        dirListView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                String selected = dirListView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                try {
                    if (selected.equals("..")) {
                        goUp();
                    } else if (selected.endsWith("/")) {
                        String newPath = normalizePath(pathField.getText()) + selected;
                        pathField.setText(newPath);
                        listDirectoryContents(newPath);
                    } else {
                        String filePath = normalizePath(pathField.getText()) + selected;
                        showFileContent(filePath);
                    }
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage(), Color.RED);
                }
            }
        });

        setStatus("Disconnected", Color.DARKGRAY);
    }

    private void connect() {
        setStatus("Connecting...", Color.ORANGE);
        resultArea.setText("");
        new Thread(() -> {
            try {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                String user = userField.getText().trim();
                String pass = passField.getText();

                sshConnector = new SSHConnector(host, port, user, pass);
                sshConnector.runCommand("echo connected");
                fileService = new FileSearchService(sshConnector);

                Platform.runLater(() -> setStatus("Connected ✔", Color.LIMEGREEN));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Connection failed: " + ex.getMessage(), Color.RED));
                if (sshConnector != null) sshConnector.disconnect();
            }
        }).start();
    }

    private void disconnect() {
        setStatus("Disconnecting...", Color.ORANGE);
        new Thread(() -> {
            try {
                if (sshConnector != null) sshConnector.disconnect();
            } finally {
                sshConnector = null;
                fileService = null;
                Platform.runLater(() -> setStatus("Disconnected", Color.DARKGRAY));
            }
        }).start();
    }

    private void listDirectoryContents(String path) {
        if (fileService == null) {
            setStatus("Not connected", Color.RED);
            return;
        }

        setStatus("Fetching directory contents...", Color.ORANGE);
        resultArea.setText("");
        new Thread(() -> {
            try {
                String normalized = normalizePath(path);

                String dirOut = fileService.listDirectories(normalized);
                String fileOut = fileService.listFiles(normalized);

                List<String> dirs = dirOut.isBlank() ? new ArrayList<>() : Arrays.asList(dirOut.split("\n"));
                List<String> files = fileOut.isBlank() ? new ArrayList<>() : Arrays.asList(fileOut.split("\n"));

                List<String> items = new ArrayList<>();
                if (!normalized.equals("/")) items.add("..");
                dirs.stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(items::add);
                files.stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(items::add);

                Platform.runLater(() -> {
                    dirListView.getItems().setAll(items);
                    setStatus("Directory contents listed ✅", Color.LIMEGREEN);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultArea.setText("❌ Error: " + ex.getMessage());
                    setStatus("Failed to list directory", Color.RED);
                });
            }
        }).start();
    }

    private void searchLogs() {
        if (fileService == null) {
            setStatus("Not connected", Color.RED);
            return;
        }
        String keyword = keywordField.getText().trim();
        String path = normalizePath(pathField.getText());
        setStatus("Searching logs...", Color.ORANGE);
        logData.clear();

        new Thread(() -> {
            try {
                String out = fileService.searchLogs(keyword, path);
                String[] lines = out.split("\n");
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", 4);
                    if (parts.length == 4) {
                        logData.add(new LogResult(parts[0], parts[1], parts[2], parts[3]));
                    }
                }
                Platform.runLater(() -> setStatus("Search complete ✅", Color.LIMEGREEN));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Search failed ❌", Color.RED));
            }
        }).start();
    }

    private void showFileContent(String filePath) {
        if (fileService == null) {
            setStatus("Not connected", Color.RED);
            return;
        }
        setStatus("Loading file...", Color.ORANGE);
        resultArea.setText("Loading " + filePath + " ...\n");
        new Thread(() -> {
            try {
                String content = fileService.readFileContent(filePath);
                Platform.runLater(() -> {
                    resultArea.setText(content);
                    setStatus("File loaded ✅", Color.LIMEGREEN);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultArea.setText("❌ Error: " + ex.getMessage());
                    setStatus("Failed to load file ❌", Color.RED);
                });
            }
        }).start();
    }

    private void goUp() {
        String current = pathField.getText().trim();
        if (current.equals("/")) return;
        if (current.endsWith("/")) current = current.substring(0, current.length() - 1);
        int idx = current.lastIndexOf('/');
        String parent = idx > 0 ? current.substring(0, idx) : "/";
        if (!parent.endsWith("/")) parent = parent + "/";
        pathField.setText(parent);
        listDirectoryContents(parent);
    }

    private String normalizePath(String p) {
        if (p == null || p.isBlank()) return "/";
        String path = p.trim();
        if (!path.endsWith("/")) path = path + "/";
        return path;
    }

    private void setStatus(String text, Color color) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setTextFill(color);
        });
    }

    public static class LogResult {
        String file;
        String line;
        String timestamp;
        String content;

        LogResult(String file, String line, String timestamp, String content) {
            this.file = file;
            this.line = line;
            this.timestamp = timestamp;
            this.content = content;
        }
    }

    private Tab createTerminalTab() {
        Tab terminalTab = new Tab("Terminal");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextArea terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);

        TextField commandInput = new TextField();
        commandInput.setPromptText("Enter command and press Enter...");

        // When user presses Enter, execute command
        commandInput.setOnAction(e -> {
            String command = commandInput.getText().trim();
            if (!command.isEmpty() && sshConnector != null && sshConnector.isConnected()) {
                try {
                    String output = sshConnector.runCommand(command);
                    terminalOutput.appendText("\n$ " + command + "\n" + output + "\n");
                } catch (Exception ex) {
                    terminalOutput.appendText("\n[ERROR] " + ex.getMessage() + "\n");
                }
                commandInput.clear();
            } else {
                terminalOutput.appendText("\n[!] Not connected to server.\n");
            }
        });

        VBox.setVgrow(terminalOutput, Priority.ALWAYS); // allow output to expand
        layout.getChildren().addAll(new Label("Remote Terminal:"), terminalOutput, commandInput);

        terminalTab.setContent(layout);
        terminalTab.setClosable(false);

        return terminalTab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
