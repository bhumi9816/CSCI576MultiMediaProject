import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.Comparator;

public class MotionDetection {
    private String videoFolderPath;
    private String sampleVideoPath;
    private String databaseFilePath;
    private String queryFeaturesFilePath;

    public MotionDetection(String videoFolderPath, String sampleVideoPath, String databaseFilePath, String queryFeaturesFilePath) {
        this.videoFolderPath = videoFolderPath;
        this.sampleVideoPath = sampleVideoPath;
        this.databaseFilePath = databaseFilePath;
        this.queryFeaturesFilePath = queryFeaturesFilePath;
    }

    public List<String[]> runMotionDetection() throws IOException, InterruptedException, ExecutionException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Build video database.
        //buildVideoDatabase(videoFolderPath, databaseFilePath);

        //System.out.println("\nQuerying the database with a sample query video...");
        List<String[]> topMatches = queryVideoDatabase(sampleVideoPath, databaseFilePath, queryFeaturesFilePath);

        return topMatches;
    }

    private static void buildVideoDatabase(String videoFolderPath, String databaseFilePath) throws IOException {
        System.out.println("Building video database...");
        FileWriter writer = new FileWriter(databaseFilePath);

        File folder = new File(videoFolderPath);
        File[] files = folder.listFiles();

        // Sort the list of files by name
        Arrays.sort(files, (file1, file2) -> file1.getName().compareTo(file2.getName()));

        if (files != null) {
            int batchSize = 2; // Adjust batch size as needed
            List<File> batch = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp4")) {
                    batch.add(file);
                    if (batch.size() >= batchSize) {
                        processVideoBatch(batch, writer);
                        batch.clear();
                    }
                }
            }
            // Process remaining videos (if any) in the last batch
            if (!batch.isEmpty()) {
                processVideoBatch(batch, writer);
            }
        }

        writer.close();
        System.out.println("Video database built successfully.");
    }

    private static void processVideoBatch(List<File> batch, FileWriter writer) {
        for (File file : batch) {
            String videoFilePath = file.getAbsolutePath();
            try {
                System.out.println("Processing video file: " + videoFilePath);
                List<double[]> videoFeatures = extractFeatures(videoFilePath);
                System.out.println("Writing features to the database for video: " + videoFilePath);
                writeFeaturesToDatabase(writer, videoFilePath, videoFeatures);
            } catch (Exception e) {
                System.err.println("Error processing video file: " + videoFilePath + ". Skipping...");
                e.printStackTrace();
            }
        }
    }

    private static List<double[]> extractFeatures(String videoFilePath) {
        //System.out.println("Extracting features from video: " + videoFilePath);
        VideoCapture capture = new VideoCapture(videoFilePath);
        List<double[]> videoFeatures = new ArrayList<>();
        int frameNumber = -2;
    
        try {
            if (!capture.isOpened()) {
                throw new IOException("Failed to open the video file: " + videoFilePath);
            }
    
            Mat frame = new Mat();
            Mat prevFrame = new Mat();
            Mat motionMask = new Mat();
            while (capture.read(frame)) {
                frameNumber++;
                if (!prevFrame.empty()) {
                    // Resize the previous frame to match the size of the current frame (512x512)
                    Imgproc.resize(prevFrame, prevFrame, new Size(512, 512));
    
                    // Resize the current frame to 512x512 pixels
                    Imgproc.resize(frame, frame, new Size(512, 512));
    
                    Core.absdiff(frame, prevFrame, motionMask);

                    // Apply morphological operations to improve the motion mask
                    Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)); // Kernel for morphological operations
                    Imgproc.morphologyEx(motionMask, motionMask, Imgproc.MORPH_CLOSE, morphKernel); // Closing operation to fill small gaps
                    Imgproc.morphologyEx(motionMask, motionMask, Imgproc.MORPH_OPEN, morphKernel); // Opening operation to remove noise
    
                    Mat grayMotionMask = new Mat();
                    Imgproc.cvtColor(motionMask, grayMotionMask, Imgproc.COLOR_BGR2GRAY);
    
                    Mat binaryMotionMask = new Mat();
                    Imgproc.threshold(grayMotionMask, binaryMotionMask, 30, 255, Imgproc.THRESH_BINARY);
    
                    double motionPercentage = Core.countNonZero(binaryMotionMask) / (frame.rows() * frame.cols() * 1.0);
    
                    videoFeatures.add(new double[]{frameNumber, motionPercentage});
                }
                prevFrame = frame.clone();
            }
        } catch (Exception e) {
            System.err.println("Error during feature extraction from video " + videoFilePath + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            capture.release();
        }
    
        //System.out.println("Feature extraction complete for video: " + videoFilePath);
    
        return videoFeatures;
    }

    private static void writeFeaturesToDatabase(FileWriter writer, String videoFilePath, List<double[]> features) throws IOException {
        writer.write("Video: " + videoFilePath + "\n");
        for (double[] feature : features) {
            writer.write("Frame " + (int) feature[0] + ": ");
            writer.write(feature[1] + "\n");
        }
        System.out.println("Features written to the database for video: " + videoFilePath);
    
        features.clear(); // Clear the list
        System.gc(); // Explicitly call garbage collector to release any remaining memory
    }
    
    private List<String[]> queryVideoDatabase(String sampleVideoPath, String databaseFilePath, String queryFeaturesFilePath) {
        //System.out.println("Querying the database with the query video: " + sampleVideoPath);
    
        // Extract features from the query video
        List<double[]> queryFeatures = extractFeatures(sampleVideoPath);
        //System.out.println("Features extracted successfully from the query video.");

        //System.out.println("Writing features to the sample features file...");
        writeFeaturesToFile(queryFeaturesFilePath, queryFeatures);
    
        // Read features from the database file
        List<List<double[]>> databaseFeatures = readFeaturesFromDatabase(databaseFilePath);
    
        // Get the list of video file paths from the database file
        List<String> videoFilePaths = extractVideoFilePaths(databaseFilePath);
    
        // Find the best match in the database
        //System.out.println("Finding the best match in the database...");
        List<String[]> bestMatchInfo = findBestMatch(queryFeatures, databaseFeatures, videoFilePaths);
    
        // Output best match information
        //System.out.println("Best match video: " + bestMatchInfo[0]);
        //System.out.println("Start frame: " + bestMatchInfo[1]);

        return bestMatchInfo; 
    }

    private static void writeFeaturesToFile(String filePath, List<double[]> features) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (double[] feature : features) {
                if (feature.length >= 2) { // Check if the feature array has at least two elements
                    writer.write("Frame " + (int) feature[0] + ": ");
                    writer.write(feature[1] + "\n");
                } else {
                    System.err.println("Invalid feature array: " + Arrays.toString(feature));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static List<String> extractVideoFilePaths(String databaseFilePath) {
        List<String> videoFilePaths = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(databaseFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Video:")) {
                    // Extract the video file path from the "Video:" line
                    videoFilePaths.add(line.substring("Video: ".length()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return videoFilePaths;
    }
    
    private static List<List<double[]>> readFeaturesFromDatabase(String databaseFilePath) {
        //System.out.println("Reading features from the database file: " + databaseFilePath);
        List<List<double[]>> databaseFeatures = new ArrayList<>();
    
        try (BufferedReader reader = new BufferedReader(new FileReader(databaseFilePath))) {
            String line;
            List<double[]> videoFeatures = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Video:")) {
                    if (videoFeatures != null) {
                        databaseFeatures.add(videoFeatures);
                    }
                    videoFeatures = new ArrayList<>();
                } else {
                    String[] parts = line.split(": ");
                    if (parts.length >= 2) { // Check if parts array has at least 2 elements
                        int frameNumber = Integer.parseInt(parts[0].split(" ")[1]);
                        try {
                            double featureValue = Double.parseDouble(parts[1]);
                            videoFeatures.add(new double[]{frameNumber, featureValue});
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid feature value format in database file: " + parts[1]);
                        }
                    } else {
                        System.err.println("Invalid format in database file: " + line);
                    }
                }
            }
            if (videoFeatures != null) {
                databaseFeatures.add(videoFeatures);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        //System.out.println("Database features read successfully.");
        return databaseFeatures;
    }
    
    private static List<String[]> findBestMatch(List<double[]> queryFeatures, List<List<double[]>> databaseFeatures, List<String> videoFilePaths) {
        List<String[]> topMatches = new ArrayList<>();
        int numMatchesToFind = 3; // Number of top matches to find
    
        // Iterate through all videos in the database
        for (int i = 0; i < databaseFeatures.size(); i++) {
            List<double[]> videoFeatures = databaseFeatures.get(i);
            int databaseVideoLength = videoFeatures.size();
    
            // Iterate through all possible starting points for the sample video in the database video
            for (int startFrameIndex = 0; startFrameIndex <= databaseVideoLength - queryFeatures.size(); startFrameIndex++) {
                double totalDifference = 0;
    
                // Compare the features of each frame in the query video with the corresponding frames in the database video
                for (int j = 0; j < queryFeatures.size(); j++) {
                    double[] queryFeature = queryFeatures.get(j);
                    double[] databaseFeature = videoFeatures.get(startFrameIndex + j);
                    // Calculate the absolute difference between feature values
                    double featureDifference = Math.abs(queryFeature[1] - databaseFeature[1]);
    
                    // Penalize abrupt changes in feature values across frames
                    if (j > 0) {
                        double prevFeatureDifference = Math.abs(queryFeatures.get(j - 1)[1] - videoFeatures.get(startFrameIndex + j - 1)[1]);
                        // Add a penalty if the current feature difference is significantly larger than the previous one
                        featureDifference += Math.max(0, featureDifference - prevFeatureDifference);
                    }
    
                    totalDifference += featureDifference;
                }
    
                // Add the match to the list of top matches if it's within the top matches found so far
                if (topMatches.size() < numMatchesToFind || totalDifference < Double.parseDouble(topMatches.get(topMatches.size() - 1)[2])) {
                    // Create match information array
                    String[] matchInfo = new String[4];
                    matchInfo[0] = videoFilePaths.get(i); // Video name
                    matchInfo[1] = String.valueOf((int) videoFeatures.get(startFrameIndex)[0]); // Start frame
                    matchInfo[2] = String.valueOf(totalDifference); // Match score
                    matchInfo[3] = "";
    
                    // Add the match to the list of top matches
                    topMatches.add(matchInfo);
    
                    // Sort the top matches based on match score
                    topMatches.sort(Comparator.comparingDouble(match -> Double.parseDouble(match[2])));
    
                    // Remove any excess matches beyond the top 3
                    if (topMatches.size() > numMatchesToFind) {
                        topMatches.remove(topMatches.size() - 1);
                    }
                }
            }
        }

        String[] bestMatch = topMatches.get(0);
        int bestFrame = Integer.parseInt(bestMatch[1]);
        String[] secondMatch = topMatches.get(1);
        int secondFrame = Integer.parseInt(secondMatch[1]);

        // Confidence check for motion detection
        if (bestMatch[0].equals(secondMatch[0])) {

            if (bestFrame == secondFrame + 1 || bestFrame == secondFrame - 1) {

                bestMatch[3] = "TRUE";
                topMatches.removeFirst();
                topMatches.addFirst(bestMatch);

            }

        }

        return topMatches;
    }
}