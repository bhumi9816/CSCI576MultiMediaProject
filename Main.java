import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final double HISTOGRAM_THRESHOLD = 0.5;
    private static final double SHOT_BOUNDARY_THRESHOLD = 0.5;

    public static void main(String[] args) {
        System.out.println("Hello world!");

        String queryVideoPath = args[0];

        /**
         * Database Pre-Processing
         * */
        String databaseDirectory = "/Users/ptbhum/Desktop/csci576:568/MultiMediaProjectColorHistogram/DatabaseVideos";
        List<VideoMetadata> database = preprocessDatabase(databaseDirectory);
        System.out.println("Database processing done");

        /**
         * Query Video Processing
         * */
        List<BufferedImage> queryFrames = extractFrames(queryVideoPath);
        List<double[]> queryHistograms = calculateColorHistograms(queryFrames);

        List<MatchedVideo> matchedVideos = testMatchVideos(database, queryHistograms);
        for(MatchedVideo matchedVideo: matchedVideos) {
            System.out.println("The matchedVideos size is " + matchedVideo.getVideoName());
        }

        System.out.println("Done matching the database and query videos");

        System.out.print("Done processing the video files ");
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
                    List<Integer> shotBoundaries = detectShotBoundaries(frames, histograms);

                    // Create a VideoMetadata instance and add it to the list
                    VideoMetadata videoMetadata = new VideoMetadata(filePath, duration, frames, histograms, shotBoundaries);
                    videoMetadata.setShotBoundaries(shotBoundaries);
                    videoMetadataList.add(videoMetadata);
                }
            }
        }

        System.out.println("Length of video metadata " + videoMetadataList.size());

        return videoMetadataList;
    }

    private static List<Integer> detectShotBoundaries(List<BufferedImage> frames, List<double[]> histograms) {
        List<Integer> shotBoundaries = new ArrayList<>();

        // Compare consecutive frames' histograms
        for (int i = 1; i < histograms.size(); i++) {
            double[] prevHistogram = histograms.get(i - 1);
            double[] currHistogram = histograms.get(i);

            // Calculate histogram difference (e.g., Euclidean distance)
            double difference = calculateHistogramDifference(prevHistogram, currHistogram);

            // If the difference exceeds a threshold, consider it a shot boundary
            if (difference > SHOT_BOUNDARY_THRESHOLD) {
                shotBoundaries.add(i); // Store the index of the frame
            }
        }

        return shotBoundaries;
    }

    private static double calculateHistogramDifference(double[] histogram1, double[] histogram2) {
        // Calculate Euclidean distance between histograms
        double distance = 0.0;
        for (int j = 0; j < histogram1.length; j++) {
            distance += Math.pow(histogram1[j] - histogram2[j], 2);
        }
        return Math.sqrt(distance);
    }

    private static List<double[]> calculateColorHistograms(List<BufferedImage> frames) {
        List<double[]> histograms = new ArrayList<>();
        for (BufferedImage frame : frames) {
            double[] histogram = calculateHistogram(frame);
            histograms.add(histogram);
        }
        System.out.println("The length of the histograms for frames " + histograms.size());
        return histograms;
    }

    private static double[] calculateHistogram(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int numBins = 256; // Number of bins for each color channel
        double[] histogram = new double[numBins * 3]; // Histogram for RGB channels concatenated

        // Iterate over each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = frame.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Increment the corresponding bin in the histogram for each color channel
                histogram[red]++;
                histogram[green + numBins]++;
                histogram[blue + 2 * numBins]++;
            }
        }

        // Normalize the histogram (optional but recommended)
        int totalPixels = width * height;
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

    private static double getVideoDuration(String filePath) {
        try {
            // Build the ffmpeg command to get the duration
            String[] command = {"/Users/ptbhum/Desktop/csci576:568/MultiMediaProjectColorHistogram/ffmpeg", "-i", filePath};

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

            // Close the reader and wait for the process to complete
            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Return a default duration if unable to retrieve from ffmpeg
        return 0.0;
    }

    private static List<BufferedImage> extractFrames(String videoFilePath) {
        System.out.println("Extracting database/query video frames " + videoFilePath);
        List<BufferedImage> frames = new ArrayList<>();

        try {
            // Execute FFmpeg command to extract frames
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "/Users/ptbhum/Desktop/csci576:568/MultiMediaProjectColorHistogram/ffmpeg", "-i", videoFilePath, "-vf", "fps=1", "-f", "image2pipe", "-pix_fmt", "rgb24", "-")
                    .redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read FFmpeg output to get frames
            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Read frame data into a byte array
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Create a BufferedImage from the byte array
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                BufferedImage frame = ImageIO.read(byteArrayInputStream);
                if (frame != null && !isBMP(frame)) {
                    frames.add(frame);
                }
            }

            // Wait for the process to complete
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

        // Choose the smaller sequence length
        int minLength = Math.min(querySize, databaseSize);

        double totalSimilarity = 0.0;

        // Adjust the sliding window size according to the sequence length
        int windowSize = Math.min(10, minLength); // Choose an appropriate window size

        // Slide the window over the sequences
        for (int i = 0; i <= minLength - windowSize; i++) {
            List<double[]> queryWindow = queryHistograms.subList(i, i + windowSize);
            List<double[]> databaseWindow = databaseHistograms.subList(i, i + windowSize);

            // Calculate color histogram similarity for the current window
            double windowSimilarity = calculateColorHistogramSimilarity(queryWindow, databaseWindow);
            totalSimilarity += windowSimilarity;
        }

        // Average the similarities
        return totalSimilarity / (minLength - windowSize + 1); // Adjusted for the number of windows
    }

    private static List<MatchedVideo> testMatchVideos(List<VideoMetadata> database, List<double[]> queryHistograms) {
        List<MatchedVideo> matchedVideos = new ArrayList<>();

        // Iterate through each database video
        for (VideoMetadata videoMetadata : database) {

            String videoName = new File(videoMetadata.getFilePath()).getName(); // Extract video name from file path
            List<double[]> databaseHistograms = videoMetadata.getHistograms();

            // Calculate color histogram similarity
            double colorHistogramSimilarity = calculateColorHistogramSimilarity(queryHistograms, databaseHistograms);

            double sequenceSimilarity = calculateTemporalHistogramSimilarity(queryHistograms, databaseHistograms);

            // Check if sequence similarity exceeds the threshold
            if (sequenceSimilarity >= HISTOGRAM_THRESHOLD) {
                matchedVideos.add(new MatchedVideo(videoName, 0.0)); // Start time is not relevant in this case
            }
        }


            // Check if similarity exceeds the threshold
           /* if (colorHistogramSimilarity >= HISTOGRAM_THRESHOLD) {
                matchedVideos.add(new MatchedVideo(videoName, 0.0)); // Start time is not relevant in this case
            }*/

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
        // Calculate Euclidean distance between the histograms
        double distance = 0.0;
        for (int j = 0; j < histogram1.length; j++) {
            distance += Math.pow(histogram1[j] - histogram2[j], 2);
        }
        distance = Math.sqrt(distance);

        // Normalize distance to get similarity
        return 1.0 / (1.0 + distance);
    }
}

class MatchedVideo {
    private String videoName;
    private double startTime;

    public MatchedVideo(String videoName, double startTime) {
        this.videoName = videoName;
        this.startTime = startTime;
    }

    public String getVideoName() {
        return videoName;
    }

    public double getStartTime() {
        return startTime;
    }
}

class VideoMetadata {
    private String filePath;
    private double duration;
    private List<BufferedImage> frames;
    private List<double[]> histograms;

    private List<Integer> shotBoundaries;

    public VideoMetadata(String filePath, double duration, List<BufferedImage> frames, List<double[]> histograms, List<Integer> shotBoundaries) {
        this.filePath = filePath;
        this.duration = duration;
        this.frames = frames;
        this.histograms = histograms;
        this.shotBoundaries = shotBoundaries;
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

    public List<Integer> getShotBoundaries() {
        return shotBoundaries;
    }

    public List<double[]> getHistograms() {
        return histograms;
    }

    public void setShotBoundaries(List<Integer> shotBoundaries) {
        this.shotBoundaries = shotBoundaries;
    }
}
