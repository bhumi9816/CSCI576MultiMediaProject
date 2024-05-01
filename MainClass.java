import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.List;

public class MainClass {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java MainClass <sampleVideoPath>");
            System.exit(1);
        }

        String sampleVideoPath = args[0];
        String videoFolderPath = "Videos";
        String databaseFilePath = "motiondb.txt"; // Motion db file path
        String queryFeaturesFilePath = "queryFeatures.txt";

        MotionDetection motionDetection = new MotionDetection(videoFolderPath, sampleVideoPath, databaseFilePath, queryFeaturesFilePath);

        try {
            List<String[]> results = motionDetection.runMotionDetection(); // Change to String[][]
            printResults(results);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void printResults(List<String[]> results) { // Change method signature
        System.out.println("Motion detection top matches:");
        for (int i = 0; i < results.size(); i++) {
            String[] match = results.get(i); // Get the match at index i
            String videoName = match[0];
            String startFrame = match[1];
            System.out.println("Match " + (i + 1) + ": Video: " + videoName + ", Start Frame: " + startFrame);
        }
    }
}