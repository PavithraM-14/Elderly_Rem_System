package com.reminder;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

public class ReminderUIFX extends Application {

    private ObservableList<String> reminders; // List of reminders
    private TextArea logsArea;                 // Area to display logs
    private final String FILE_PATH = "reminders.txt"; // File to persist reminders

    @Override
    public void start(Stage stage) {
        stage.setTitle("Elderly Reminder System");

        // Initialize reminders list and load from file
        reminders = FXCollections.observableArrayList();
        loadRemindersFromFile();

        // --- Tab 1: Add Reminder ---
        GridPane addReminderPane = new GridPane();
        addReminderPane.setPadding(new Insets(10));
        addReminderPane.setVgap(10);
        addReminderPane.setHgap(10);

        Label msgLabel = new Label("Message:");
        TextField msgField = new TextField();
        Label timeLabel = new Label("Time (HH:mm):");
        TextField timeField = new TextField();

        Button saveBtn = new Button("Save Reminder");
        Label statusLabel = new Label();

        // Add controls to grid
        addReminderPane.add(msgLabel, 0, 0);
        addReminderPane.add(msgField, 1, 0);
        addReminderPane.add(timeLabel, 0, 1);
        addReminderPane.add(timeField, 1, 1);
        addReminderPane.add(saveBtn, 1, 2);
        addReminderPane.add(statusLabel, 1, 3);

        // Save button action
        saveBtn.setOnAction(e -> {
            String msg = msgField.getText().trim();
            String time = timeField.getText().trim();

            if (msg.isEmpty()) {
                statusLabel.setText("⚠ Please enter the Reminder Message.");
            } else if (time.isEmpty()) {
                statusLabel.setText("⚠ Please enter the Reminder Time.");
            } else if (!isValidTime(time)) {
                statusLabel.setText("⚠ Invalid time format. Use HH:mm.");
            } else {
                String reminderEntry = time + " - " + msg;
                if (!reminders.contains(reminderEntry)) { // prevent duplicates
                    reminders.add(reminderEntry);
                    saveRemindersToFile(); // save to file
                    statusLabel.setText("✅ Reminder saved!");
                } else {
                    statusLabel.setText("⚠ Reminder already exists.");
                }
                msgField.clear();
                timeField.clear();
            }
        });

        // --- Tab 2: View Reminders ---
        ListView<String> reminderListView = new ListView<>(reminders);
        VBox viewPane = new VBox(10, reminderListView);
        viewPane.setPadding(new Insets(10));

        // --- Tab 3: Logs ---
        logsArea = new TextArea();
        logsArea.setEditable(false);
        logsArea.setPromptText("Logs will appear here...");
        VBox logsPane = new VBox(10, logsArea);
        logsPane.setPadding(new Insets(10));

        // --- Tabs ---
        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Add Reminder", addReminderPane);
        Tab tab2 = new Tab("View Reminders", viewPane);
        Tab tab3 = new Tab("Logs", logsPane);
        tab1.setClosable(false);
        tab2.setClosable(false);
        tab3.setClosable(false);
        tabPane.getTabs().addAll(tab1, tab2, tab3);

        Scene scene = new Scene(tabPane, 600, 450);
        stage.setScene(scene);
        stage.show();

        // --- Start Reminder Checking using Timeline ---
        startReminderChecker();
    }

    // --- Validate time format HH:mm ---
    private boolean isValidTime(String time) {
        try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // --- Load reminders from file ---
    private void loadRemindersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                reminders.add(line);
            }
        } catch (IOException ex) {
            // File may not exist initially; ignore
        }
    }

    // --- Save reminders to file ---
    private void saveRemindersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String r : reminders) {
                writer.write(r);
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // --- Check reminders every second using Timeline (JavaFX safe) ---
    private void startReminderChecker() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        Set<String> triggered = new HashSet<>(); // track triggered reminders

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            String currentTime = LocalTime.now().format(dtf);

            // Use a snapshot copy to avoid concurrent modification
            for (String reminder : FXCollections.observableArrayList(reminders)) {
                String[] parts = reminder.split(" - ", 2);
                if (parts.length < 2) continue;

                String time = parts[0];
                String msg = parts[1];

                if (currentTime.equals(time) && !triggered.contains(reminder)) {
                    triggered.add(reminder);

                    // beep alert
                    java.awt.Toolkit.getDefaultToolkit().beep();

                    // update logs and show non-blocking alert
                    logsArea.appendText("⏰ Reminder Triggered: " + msg + " at " + currentTime + "\n");

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Reminder Alert");
                    alert.setHeaderText("It's Time!");
                    alert.setContentText(msg);
                    alert.show(); // non-blocking
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public static void main(String[] args) {
        launch();
    }
}
