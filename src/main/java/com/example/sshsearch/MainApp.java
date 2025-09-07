package com.example.sshsearch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // Input fields
        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("22");
        TextField userField = new TextField("pratik");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Ubuntu password");
        TextField keywordField = new TextField("ERROR");

        // Buttons
        Button searchButton = new Button("🔍 Search Logs");
        Button clearButton = new Button("🧹 Clear");

        // Result area
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setFont(Font.font("Consolas", 13));
        resultArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: lime;");

        // Status bar
        Label statusLabel = new Label("Disconnected");
        statusLabel.setTextFill(Color.DARKGRAY);

        // Layout
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        form.add(new Label("Host:"), 0, 0);
        form.add(hostField, 1, 0);

        form.add(new Label("Port:"), 0, 1);
        form.add(portField, 1, 1);

        form.add(new Label("Username:"), 0, 2);
        form.add(userField, 1, 2);

        form.add(new Label("Password:"), 0, 3);
        form.add(passField, 1, 3);

        form.add(new Label("Keyword:"), 0, 4);
        form.add(keywordField, 1, 4);

        HBox buttons = new HBox(10, searchButton, clearButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        form.add(buttons, 1, 5);

        VBox layout = new VBox(10, form, resultArea, statusLabel);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 650, 500);
        stage.setScene(scene);
        stage.setTitle("🚀 SSH Log Search Tool");
        stage.show();

        // Actions
        clearButton.setOnAction(e -> resultArea.clear());

        searchButton.setOnAction(e -> {
            Platform.runLater(() -> {
                resultArea.setText("🔎 Searching...\n");
                statusLabel.setText("Connecting to SSH...");
                statusLabel.setTextFill(Color.ORANGE);
            });

            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());
            String user = userField.getText();
            String password = passField.getText();
            String keyword = keywordField.getText();

            new Thread(() -> {
                try {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(user, host, port);
                    session.setPassword(password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();

                    Platform.runLater(() -> {
                        statusLabel.setText("Connected ✔");
                        statusLabel.setTextFill(Color.GREEN);
                    });

                    ChannelExec channel = (ChannelExec) session.openChannel("exec");
                    String command = "rg --with-filename '" + keyword +
                            "' /home/pratik/testlogs --glob '*.log' || echo 'No matching logs found'";
                    channel.setCommand(command);
                    channel.setErrStream(System.err);

                    InputStream in = channel.getInputStream();
                    channel.connect();

                    StringBuilder output = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    String resultText = output.toString();
                    if (resultText.isBlank()) {
                        resultText = "⚠ No results found!";
                    }

                    final String finalResult = resultText;
                    Platform.runLater(() -> {
                        resultArea.setText(finalResult);
                        statusLabel.setText("Search complete ✅");
                        statusLabel.setTextFill(Color.LIMEGREEN);
                    });

                    channel.disconnect();
                    session.disconnect();

                } catch (Exception ex) {
                    final String errorMsg = "❌ Error: " + ex.getMessage();
                    Platform.runLater(() -> {
                        resultArea.setText(errorMsg);
                        statusLabel.setText("Connection failed ❌");
                        statusLabel.setTextFill(Color.RED);
                    });
                }
            }).start();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
