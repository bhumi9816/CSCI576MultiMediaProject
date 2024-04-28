import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.media.*;
import javafx.util.Duration;

import java.io.*;
import java.util.List;

public class MP4Player extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Get the video file path and start time from main class
        Parameters params = getParameters();
        List<String> args = params.getRaw();
        String videoTitle = args.get(0);
        String path = "Database/MP4/" + videoTitle;
        int startTime = Integer.parseInt(args.get(1));

        // Instantiating Media class with video file path
        Media media = new Media(new File(path).toURI().toString());

        // Instantiating MediaPlayer class and set start time
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        //mediaPlayer.setStartTime(Duration.seconds(startTime));

        // Instantiating MediaView class
        MediaView mediaView = new MediaView(mediaPlayer);

        // Create play, pause, and reset buttons
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        Button goToQueryButton = new Button("Go to Query");

        // Set actions for the buttons
        playButton.setOnAction(e -> mediaPlayer.play());
        pauseButton.setOnAction(e -> mediaPlayer.pause());
        resetButton.setOnAction(e -> mediaPlayer.seek(Duration.ZERO));
        goToQueryButton.setOnAction(e -> mediaPlayer.seek(Duration.seconds(startTime)));

        // Arrange buttons horizontally
        HBox controlBox = new HBox(playButton, pauseButton, resetButton, goToQueryButton);
        controlBox.setAlignment(Pos.BASELINE_CENTER);

        // Setup layout and window properties
        BorderPane root = new BorderPane();
        root.setCenter(mediaView);
        root.setBottom(controlBox);
        int stdWidth = 352;
        int stdHeight = 288;
        Scene scene = new Scene(root,stdWidth,stdHeight + 40);
        primaryStage.setScene(scene);
        primaryStage.setTitle(videoTitle);

        // Display the video
        primaryStage.show();

    }

}
