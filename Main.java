import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        System.out.println("Hello World!");
        if (args.length < 2) {
            System.out.println("Usage: MyProject.exe QueryVideo.rgb QueryAudio.wav");
            return;
        }

        String queryVideoPath = args[0];

        Map<String, Mat> videoFeaturesMap = new HashMap<>();
        Map<String, Mat> audioFeaturesMap = new HashMap<>();

        // Step 2: Load database containing 40 videos
        List<String> databaseVideoPaths = loadDatabaseVideos();

        for (String videoPath : databaseVideoPaths) {
            // Step 4: Extract video and audio features for each video
            Mat videoFeatures = extractShotVideoFeatures(videoPath);

            // Step 5: Store features in data structures
            videoFeaturesMap.put(videoPath, videoFeatures);
        }

        // Step 6: Extract features from the query video
        Mat queryVideoFeatures = extractShotVideoFeatures(queryVideoPath);

        // Step 7: Match query video features with database videos and play the start of the matched video
        matchAndPlayStart(queryVideoFeatures, videoFeaturesMap);

    }

    // Load the videos from the database
    private static List<String> loadDatabaseVideos() {
        List<String> databaseVideoPaths = new ArrayList<>();
        // Replace this with code to load video paths from your database directory
        File databaseDirectory = new File("video1.mp4");
        File[] files = databaseDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    databaseVideoPaths.add(file.getAbsolutePath());
                }
            }
        }
        return databaseVideoPaths;
    }

    // Method to extract Shot boundary for each video in the database and save in hash data-structure
    private static Mat extractShotVideoFeatures(String videoPath) {
        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            System.err.println("Error: Could not open video file.");
            return null;
        }

        Mat aggregatedFeatures = new Mat();
        Mat previousFrame = new Mat();
        Mat currentFrame = new Mat();
        boolean firstFrame = true;

        while (capture.read(currentFrame)) {
            // Convert frames to grayscale for simplicity
            Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);

            if (!firstFrame) {
                // Compute absolute difference between frames
                Mat diffFrame = new Mat();
                Core.absdiff(currentFrame, previousFrame, diffFrame);

                // Calculate the average pixel intensity difference as a simple shot boundary detection
                Scalar meanDiff = Core.mean(diffFrame);

                // If the mean difference exceeds a threshold, consider it as a shot boundary
                if (meanDiff.val[0] > 10) {
                    // Extract features from the shot region
                    Mat shotFeatures = extractFeaturesFromShot(previousFrame);

                    // Concatenate the features from all shots
                    if (aggregatedFeatures.empty()) {
                        aggregatedFeatures = shotFeatures.clone();
                    } else {
                        Core.hconcat(Arrays.asList(new Mat[]{aggregatedFeatures, shotFeatures}), aggregatedFeatures);
                    }
                }
            }

            // Update previous frame
            currentFrame.copyTo(previousFrame);
            firstFrame = false;
        }

        capture.release();

        return aggregatedFeatures;
    }

    private static Mat extractFeaturesFromShot(Mat frame) {
        // Implement feature extraction from shot region here
        // You can use this method to extract features from each shot region
        // For example, you can calculate histograms, texture features, etc.
        // You can access the frame for the shot region
        // Perform feature extraction based on your requirements

        // For demonstration purposes, we'll just resize the frame
        Mat resizedFrame = new Mat();
        Imgproc.resize(frame, resizedFrame, new Size(100, 100)); // Resize to 100x100 pixels

        return resizedFrame.reshape(1, 1); // Convert to row vector
    }

    private static void matchAndPlayStart(Mat queryFeatures, Map<String, Mat> databaseFeaturesMap) {
        Map<String, Double> similarityScores = new HashMap<>();

        // Iterate over database videos and compute similarity with query features
        for (Map.Entry<String, Mat> entry : databaseFeaturesMap.entrySet()) {
            String videoName = entry.getKey();
            Mat databaseFeatures = entry.getValue();

            // Compute similarity using a distance or similarity measure
            double similarity = computeMatchSimilarity(queryFeatures, databaseFeatures);

            // Store the similarity score
            similarityScores.put(videoName, similarity);
        }

        // Rank database videos based on similarity scores
        List<Map.Entry<String, Double>> rankedVideos = new ArrayList<>(similarityScores.entrySet());
        rankedVideos.sort(Map.Entry.comparingByValue());

        // Output the top-ranked video
        if (!rankedVideos.isEmpty()) {
            String topRankedVideoPath = rankedVideos.get(0).getKey();
            System.out.println("Top-ranked video: " + topRankedVideoPath);

            // Play the start of the top-ranked video
            playVideoStart(topRankedVideoPath);
        } else {
            System.out.println("No match found in the database.");
        }
    }

    private static double computeMatchSimilarity(Mat queryFeatures, Mat databaseFeatures) {
        // Ensure both matrices have the same size
        if (queryFeatures.size().equals(databaseFeatures.size())) {
            // Compute Euclidean distance between query and database features
            Mat diff = new Mat();
            Core.absdiff(queryFeatures, databaseFeatures, diff);
            diff.convertTo(diff, CvType.CV_32F);
            double euclideanDistance = Core.norm(diff, Core.NORM_L2);

            // Return the similarity score (inverse of distance)
            return 1.0 / (1.0 + euclideanDistance);
        } else {
            // Return a default similarity score if matrices have different sizes
            return 0.0;
        }
    }

    private static void playVideoStart(String videoPath) {
        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            System.err.println("Error: Could not open video file.");
            return;
        }

        Mat frame = new Mat();
        while (capture.read(frame)) {
            HighGui.imshow("Video", frame); // Display the frame
            int key = HighGui.waitKey(30); // Delay in milliseconds

            // Break the loop if 'ESC' key is pressed
            if (key == 27) {
                break;
            }
        }

        capture.release();
        HighGui.destroyAllWindows();
    }

}

