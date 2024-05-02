import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;

public class Main {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        System.out.println("Hello world!");


        // Find best match ColorHistogram ------------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        /*
        String queryVideoPath = args[0];

        // Database pre-processing
        String databaseDirectory = "Database/MP4";
        List<VideoMetadata> database = preprocessDatabase(databaseDirectory);
        System.out.println("Database processing done");

        // Query video processing
        List<BufferedImage> queryFrames = extractFrames(queryVideoPath);
        List<double[]> queryHistograms = calculateColorHistograms(queryFrames);

        List<MatchedVideo> matchedVideos = testMatchVideos(database, queryHistograms);
        if (!matchedVideos.isEmpty()) {
            System.out.println("Top 10 matched videos:");
            for (int i = 0; i < Math.min(10, matchedVideos.size()); i++) {
                MatchedVideo matchedVideo = matchedVideos.get(i);
                System.out.println("Video Name: " + matchedVideo.getVideoName() + " at frame: " + matchedVideo.getStartTime());
                System.out.println("Similarity Score: " + matchedVideo.getSimilarity());
                System.out.println("-------------------------------------");
            }
        } else {
            System.out.println("No matched videos found.");
        }
        */

        // Find best match MotionDetection -----------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        String sampleVideoPath = args[0];
        String videoFolderPath = "Database/MP4";
        String databaseFilePath = "motiondb.txt"; // Motion db file path
        String queryFeaturesFilePath = "queryFeatures.txt";

        MotionDetection motionDetection = new MotionDetection(videoFolderPath, sampleVideoPath, databaseFilePath, queryFeaturesFilePath);

        List<String[]> motionResults = motionDetection.runMotionDetection();
        printMotionResults(motionResults);
        System.out.println();

        // Find best match AudioParser ---------------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        AudioParser audioParser = new AudioParser();

        String queryPath = args[1];
        audioParser.analyzeQuery(queryPath);
        audioParser.findBestMatch();

        String audioBestMatchVid = audioParser.currentMinVidAA;
        int audioBestMatchFrame = audioParser.currentMinFrameAA;
        String audioSecondBestMatchVid = audioParser.secondMinVidAA;
        int audioSecondBestMatchFrame = audioParser.secondMinFrameAA;

        List<String[]> audioResults = audioParser.retrieveResults();

        double startSeconds = audioBestMatchFrame / 30.0;
        double startMillis = startSeconds * 1000.0;
        String startTime = String.valueOf(startMillis);

        printAudioResults(audioResults);
        System.out.println();

        // Find the agreed best match using all three digital signatures -----------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        String[] overallBestMatch = findOverallMatch(audioResults, motionResults);
        String overallBestMatchVid = overallBestMatch[0].substring(overallBestMatch[0].indexOf("v"));
        System.out.println("The final match is " + overallBestMatchVid + " at frame " + overallBestMatch[1]);

        // Play the best match MP4 -------------------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        //Application.launch(MP4Player.class, audioParser.currentMinVidAA, startTime);


    }

    private static void printMotionResults(List<String[]> results) { // Change method signature
        System.out.println("Motion detection top matches:");
        for (int i = 0; i < results.size(); i++) {
            String[] match = results.get(i); // Get the match at index i
            String videoName = match[0];
            String startFrame = match[1];
            System.out.println("Match " + (i + 1) + ": Video: " + videoName + ", Start Frame: " + startFrame);
        }
    }

    private static void printAudioResults(List<String[]> results) {
        System.out.println("Audio detection top matches:");
        for (int i = 0; i < results.size(); i++) {
            String[] match = results.get(i);
            String videoName = match[0];
            String startFrame = match[1];
            System.out.println("Match " + (i + 1) + ": Video: " + videoName + ", Start Frame: " + startFrame);
        }
    }

    private static String[] findOverallMatch(List<String[]> audioResults, List<String[]> motionResults) {
        String[] bestMatch = null;
    
        // Define weights for motion and audio results
        double motionWeight = 0.3; // Weight for motion results (lower weight for lower priority)
        double audioWeight = 0.7;  // Weight for audio results (higher weight for higher priority)
    
        // Perform confidence check for motion
        boolean motionMatchConfident = motionResults.size() >= 1 && "TRUE".equals(motionResults.get(0)[3]);
    
        // Perform confidence check for audio
        boolean audioMatchConfident = audioResults.size() >= 1 && "TRUE".equals(audioResults.get(0)[2]);
    
        // Calculate scores based on confidence and weights
        double motionScore = motionMatchConfident ? 1.0 : 0.0; // If confident, score is 1, else 0
        double audioScore = audioMatchConfident ? 1.0 : 0.0;   // If confident, score is 1, else 0
    
        // If neither motion nor audio results are confident, prioritize motion result
        if (!motionMatchConfident && !audioMatchConfident) {
            if (!motionResults.isEmpty()) {
                motionScore = 0.5; // Assign a default score if neither is confident
            } else if (!audioResults.isEmpty()) {
                audioScore = 0.5;  // Assign a default score if neither is confident
            }
        }
    
        // Calculate weighted average score
        double weightedMotionScore = motionScore * motionWeight;
        double weightedAudioScore = audioScore * audioWeight;
    
        // Choose the best match based on weighted scores
        if (weightedMotionScore < weightedAudioScore) { // Prioritizing audio over motion
            bestMatch = audioResults.get(0);
        } else if (weightedAudioScore < weightedMotionScore) {
            bestMatch = motionResults.get(0);
        } else { // If scores are equal, choose based on the order of preference (audio > motion)
            if (!audioResults.isEmpty()) {
                bestMatch = audioResults.get(0);
            } else if (!motionResults.isEmpty()) {
                bestMatch = motionResults.get(0);
            }
        }
    
        return bestMatch;
    }    

    private static List<VideoMetadata> preprocessDatabase(String databaseDirectory) {
        List<VideoMetadata> videoMetadataList = new ArrayList<>();

        // Get list of files in the database directory
        File[] videoFiles = new File(databaseDirectory).listFiles();
        if(videoFiles == null) {
            System.out.println("No video file is processed");
        }

        if (videoFiles != null) {
            // Iterate through each video file
            for (File videoFile : videoFiles) {
                if (videoFile.isFile()) {
                    // Extract metadata for the current video file
                    String filePath = videoFile.getAbsolutePath();
                    double duration = getVideoDuration(filePath); // Implement a method to get video duration
                    System.out.println("Video duration: " + duration + " seconds");

                    List<BufferedImage> frames = extractFrames(filePath); // Implement a method to extract frames
                    List<double[]> histograms = calculateColorHistograms(frames);

                    // Create a VideoMetadata instance and add it to the list
                    VideoMetadata videoMetadata = new VideoMetadata(filePath, duration, frames, histograms);
                    videoMetadataList.add(videoMetadata);
                }
            }
        }

        return videoMetadataList;
    }

    private static List<double[]> calculateColorHistograms(List<BufferedImage> frames) {
        List<double[]> histograms = new ArrayList<>();
        for (BufferedImage frame : frames) {
            double[] histogram = calculateHistogram(frame);
            histograms.add(histogram);
        }
        return histograms;
    }

    private static double[] calculateHistogram(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int numBins = 512;
        double[] histogram = new double[numBins * 3];

        // Iterate over each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = frame.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                histogram[red]++;
                histogram[green + numBins]++;
                histogram[blue + 2 * numBins]++;
            }
        }

        int totalPixels = width * height;
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

    private static double getVideoDuration(String filePath) {
        try {
            // Build the ffmpeg command to get the duration
            String[] command = {"ffmpeg-master-latest-win64-gpl/bin/ffmpeg.exe", "-i", filePath};

            // Execute the command
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            // Read the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration")) {
                    // Parse the duration from the output line
                    String[] parts = line.split(",")[0].split(":");
                    int hours = Integer.parseInt(parts[1].trim());
                    int minutes = Integer.parseInt(parts[2].trim());
                    String[] secondsParts = parts[3].trim().split("\\.");
                    int seconds = Integer.parseInt(secondsParts[0]);
                    int milliseconds = Integer.parseInt(secondsParts[1]);
                    System.out.print("The duration " + milliseconds);
                    // Calculate the total duration in seconds
                    return hours * 3600 + minutes * 60 + seconds + milliseconds / 1000.0;
                }
            }


            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return 0.0;
    }

    private static List<BufferedImage> extractFrames(String videoFilePath) {
        System.out.println("Extracting database/query video frames " + videoFilePath);
        List<BufferedImage> frames = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg-master-latest-win64-gpl/bin/ffmpeg.exe", "-i", videoFilePath, "-vf", "fps=1", "-f", "image2pipe", "-pix_fmt", "rgb24", "-")
                    .redirectErrorStream(true);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                BufferedImage frame = ImageIO.read(byteArrayInputStream);
                if (frame != null && !isBMP(frame)) {
                    frames.add(frame);
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return frames;
    }

    private static boolean isBMP(BufferedImage image) {
        return image.getColorModel().toString().contains("BMP");
    }

    private static double calculateTemporalHistogramSimilarity(List<double[]> queryHistograms, List<double[]> databaseHistograms) {
        int querySize = queryHistograms.size();
        int databaseSize = databaseHistograms.size();

        int minLength = Math.min(querySize, databaseSize);

        double totalSimilarity = 0.0;

        int windowSize = Math.min(10, minLength);


        for (int i = 0; i <= minLength - windowSize; i++) {
            List<double[]> queryWindow = queryHistograms.subList(i, i + windowSize);
            List<double[]> databaseWindow = databaseHistograms.subList(i, i + windowSize);

            double windowSimilarity = calculateColorHistogramSimilarity(queryWindow, databaseWindow);
            totalSimilarity += windowSimilarity;
        }

        return totalSimilarity / (minLength - windowSize + 1);
    }

    private static List<MatchedVideo> testMatchVideos(List<VideoMetadata> database, List<double[]> queryHistograms) {

        PriorityQueue<MatchedVideo> topMatches = new PriorityQueue<>(Comparator.comparingDouble(MatchedVideo::getSimilarity));

        for (VideoMetadata videoMetadata : database) {

            String videoName = new File(videoMetadata.getFilePath()).getName();
            List<double[]> databaseHistograms = videoMetadata.getHistograms();

            double colorHistogramSimilarity = calculateColorHistogramSimilarity(queryHistograms, databaseHistograms);

            double sequenceSimilarity = calculateTemporalHistogramSimilarity(queryHistograms, databaseHistograms);

            double combinedSimilarity = colorHistogramSimilarity * sequenceSimilarity;
            System.out.print("Similarity score is " + combinedSimilarity);

            MatchedVideo matchedVideo = new MatchedVideo(videoName, 0.0, combinedSimilarity);

            topMatches.offer(matchedVideo);

            if (topMatches.size() > 10) {
                topMatches.poll();
            }
        }

        List<MatchedVideo> matchedVideos = new ArrayList<>(topMatches);

        matchedVideos.sort(Comparator.comparingDouble(MatchedVideo::getSimilarity).reversed());

        return matchedVideos;

    }

    private static double calculateColorHistogramSimilarity(List<double[]> queryHistograms, List<double[]> databaseHistograms) {
        int minSize = Math.min(queryHistograms.size(), databaseHistograms.size());
        double totalSimilarity = 0.0;

        // Calculate similarity for each pair of histograms
        for (int i = 0; i < minSize; i++) {
            double[] queryHistogram = queryHistograms.get(i);
            double[] databaseHistogram = databaseHistograms.get(i);
            double histogramSimilarity = calculateHistogramSimilarity(queryHistogram, databaseHistogram);
            totalSimilarity += histogramSimilarity;
        }

        // Average the similarities
        return totalSimilarity / minSize;
    }

    private static double calculateHistogramSimilarity(double[] histogram1, double[] histogram2) {
        double chiSquareDistance = 0.0;
        for (int j = 0; j < histogram1.length; j++) {
            if (histogram1[j] + histogram2[j] != 0) {
                chiSquareDistance += Math.pow(histogram1[j] - histogram2[j], 2) / (histogram1[j] + histogram2[j]);
            }
        }
        return 1.0 / (1.0 + chiSquareDistance);

    }
}

class MatchedVideo {
    private String videoName;
    private double startTime;
    private double similarity;

    public MatchedVideo(String videoName, double startTime, double similarity) {
        this.videoName = videoName;
        this.startTime = startTime;
        this.similarity = similarity;
    }

    public String getVideoName() {
        return videoName;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getSimilarity() {
        return similarity;
    }
}

class VideoMetadata {
    private String filePath;
    private double duration;
    private List<BufferedImage> frames;
    private List<double[]> histograms;

    public VideoMetadata(String filePath, double duration, List<BufferedImage> frames, List<double[]> histograms) {
        this.filePath = filePath;
        this.duration = duration;
        this.frames = frames;
        this.histograms = histograms;
    }

    public String getFilePath() {
        return filePath;
    }

    public double getDuration() {
        return duration;
    }

    public List<BufferedImage> getFrames() {
        return frames;
    }

    public List<double[]> getHistograms() {
        return histograms;
    }

}
