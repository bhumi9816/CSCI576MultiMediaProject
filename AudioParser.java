import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import static java.lang.Double.MAX_VALUE;

public class AudioParser {

    // Data values for the query and database videos are stored here when finding best match
    ArrayList<Double> queryZCVals = new ArrayList<>();
    ArrayList<Double> databaseZCVals = new ArrayList<>();
    ArrayList<Double> queryAAVals = new ArrayList<>();
    ArrayList<Double> databaseAAVals = new ArrayList<>();

    // Public variables for passing the best match video and start time back to main class
    public String currentMinVidZC;
    public int currentMinFrameZC;

    public String currentMinVidAA;
    public String secondMinVidAA;
    public String thirdMinVidAA;

    public int currentMinFrameAA;
    public int secondMinFrameAA;
    public int thirdMinFrameAA;

    // Empty constructor for main class to instantiate an AudioParser
    public AudioParser() {
    }

    // Analyzes the database videos and stores the results in .txt files
    public void analyzeAudio(String inputFile, String outputFile, String outputFile2) {

        try {

            // Set up the FileWriter for analysis output
            FileWriter writer = new FileWriter(outputFile);
            FileWriter writer2 = new FileWriter(outputFile2);

            // Set up the audio stream and obtain some relevant information from it for later use
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(inputFile));
            AudioFormat format = audioInputStream.getFormat();
            int frameLength = (int) audioInputStream.getFrameLength();
            int frameRate = (int) format.getFrameRate();
            int frameSize = format.getFrameSize();

            // Calculate how many audio frames exist in each video frame
            int videoFPS = 30;
            int audioFramesPerVideoFrame = frameRate/videoFPS;

            // Set up an array of bytes to store a video frame's worth of samples
            int sampleBytesPerVideoFrame = audioFramesPerVideoFrame * frameSize;
            byte[] audioData = new byte[sampleBytesPerVideoFrame];
            int bytesRead;

            // Calculate zero crossing rate and average amplitude for each second
            for (int i = 0; i < frameLength; i += audioFramesPerVideoFrame) {

                bytesRead = audioInputStream.read(audioData);

                if (bytesRead > 0) {

                    double ZCRate = calcZCRFixed(audioData, frameSize);
                    writer.write(ZCRate + " ");

                    double avgAmplitude = calculateAvgAmplitude(audioData);
                    writer2.write(avgAmplitude + " ");

                }

            }

            // Close the audio stream for inputFile and the file writer for outputFile
            audioInputStream.close();
            writer.close();

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }

    }

    // This method calculates the zero-crossing rate for each second of the given WAV file
    private static double calcZCRFixed(byte[] audioData, int frameSize) {

        //Currently only using the left channel to calculate Z-C Rate
        int numZeroCrossingsL = 0;

        for (int i = 0; i < audioData.length/frameSize; i += 4) {

            // The indexes correspond to interleaved stereo sample data
            int sample1L = ((audioData[i]) << 8) | (audioData[i+1]);
            int sample2L = ((audioData[i+4]) << 8) | (audioData[i+5]);

            if ((sample1L >= 0 && sample2L < 0) || (sample1L < 0 && sample2L >= 0)) {
                numZeroCrossingsL++;
            }

        }

        // Divide by two since there are 2 bytes per sample.
        return (double) numZeroCrossingsL / (audioData.length / 4.0);

    }

    // This method calculates the root-mean-squared amplitude for each second of the given WAV file.
    private static double calculateAvgAmplitude(byte[] audioData) {

        double amplitudeSumSquares = 0.0;

        for (byte audioDatum : audioData) {
            amplitudeSumSquares += (audioDatum * audioDatum);
        }

        return Math.sqrt( amplitudeSumSquares / (audioData.length));

    }

    // Similar to analyzeDatabase, but stores the results in an ArrayList for more efficient use when finding matches.
    public void analyzeQuery(String queryPath) {

        try {

            // Set up the audio stream and obtain some relevant information from it for later use
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(queryPath));
            AudioFormat format = audioInputStream.getFormat();
            int frameLength = (int) audioInputStream.getFrameLength();
            int frameRate = (int) format.getFrameRate();
            int frameSize = format.getFrameSize();

            // Calculate how many audio frames exist in each video frame
            int videoFPS = 30;
            int audioFramesPerVideoFrame = frameRate/videoFPS;

            // Set up an array of bytes to store a video frame's worth of samples
            int sampleBytesPerVideoFrame = audioFramesPerVideoFrame * frameSize;
            byte[] audioData = new byte[sampleBytesPerVideoFrame];
            int bytesRead;

            // Calculate zero crossing rate for each second
            for (int i = 0; i < frameLength; i += audioFramesPerVideoFrame) {

                bytesRead = audioInputStream.read(audioData);

                if (bytesRead > 0) {

                    double zeroCRL = calcZCRFixed(audioData, frameSize);
                    queryZCVals.add(zeroCRL);

                    double avgAmplitude = calculateAvgAmplitude(audioData);
                    queryAAVals.add(avgAmplitude);

                }

            }

            // Close the audio stream for inputFile and the file writer for outputFile
            audioInputStream.close();

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }

    }

    // Finds the best matching video and start time for the query video. Results are stored in the class' instance vars.
    public void findBestMatch() {

        int databaseSize = 20;
        double currentMinDifferenceZC = MAX_VALUE;
        double currentMinDifferenceAA = MAX_VALUE;
        double currentSecondBestDiffAA = MAX_VALUE;
        double currentThirdBestDiffAA = MAX_VALUE;

        // This process repeats as many times as there are videos in the database
        for (int videoIdx = 1; videoIdx <= databaseSize; videoIdx++) {

            // Clear the ArrayList vals from the previous iteration
            databaseZCVals.clear();
            databaseAAVals.clear();

            // Load all the zero-crossing values from this database video to the ArrayLists
            String currentVidZC = "Database/Output/ZC_output" + videoIdx + ".txt";
            String currentMP4 = "video" + videoIdx + ".mp4";
            String currentVidAA = "Database/Output/AA_output" + videoIdx + ".txt";
            Scanner scanner;
            Scanner scanner2;

            try {
                scanner = new Scanner(new File(currentVidZC));
                scanner2 = new Scanner(new File(currentVidAA));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            while (scanner.hasNextDouble()) {

                databaseZCVals.add(scanner.nextDouble());

            }

            while (scanner2.hasNextDouble()) {

                databaseAAVals.add(scanner2.nextDouble());

            }

            // Close the scanners now that the database values have been obtained
            scanner.close();
            scanner2.close();

            /*
                This process repeats as many times as there are seconds in the video being inspected minus the number
                of seconds in the query video. This process is finding the best match based on zero-crossing rate.
            */
            for (int databaseSeconds = 0; databaseSeconds <= databaseZCVals.size() - queryZCVals.size(); databaseSeconds++) {

                double currentDiffZC = 0;

                for (int querySeconds = 0; querySeconds < queryZCVals.size(); querySeconds++) {

                    double queryZCR = queryZCVals.get(querySeconds);
                    double databaseZCR = databaseZCVals.get(databaseSeconds + querySeconds);

                    currentDiffZC += Math.abs(queryZCR - databaseZCR);

                }

                if (currentDiffZC < currentMinDifferenceZC) {

                    currentMinDifferenceZC = currentDiffZC;
                    currentMinVidZC = currentMP4;
                    currentMinFrameZC = databaseSeconds;

                }

            }

            // Repeating a similar process as above, but using root-mean-square values as reference
            for (int databaseSeconds = 0; databaseSeconds <= databaseAAVals.size() - queryAAVals.size(); databaseSeconds++) {

                double currentDiffAA = 0;

                for (int querySeconds = 0; querySeconds < queryAAVals.size(); querySeconds++) {

                    double queryAA = queryAAVals.get(querySeconds);
                    double databaseAA = databaseAAVals.get(databaseSeconds + querySeconds);

                    currentDiffAA += Math.abs(queryAA - databaseAA);

                }

                if (currentDiffAA < currentMinDifferenceAA) {

                    currentMinDifferenceAA = currentDiffAA;

                    thirdMinVidAA = secondMinVidAA;
                    secondMinVidAA = currentMinVidAA;
                    currentMinVidAA = currentMP4;

                    thirdMinFrameAA = secondMinFrameAA;
                    secondMinFrameAA = currentMinFrameAA;
                    currentMinFrameAA = databaseSeconds;

                }
                else if (currentDiffAA < currentSecondBestDiffAA) {

                    currentSecondBestDiffAA = currentDiffAA;

                    thirdMinVidAA = secondMinVidAA;
                    secondMinVidAA = currentMP4;

                    thirdMinFrameAA = secondMinFrameAA;
                    secondMinFrameAA = databaseSeconds;

                }
                else  if (currentDiffAA < currentThirdBestDiffAA) {

                    currentThirdBestDiffAA = currentDiffAA;
                    thirdMinVidAA = currentMP4;
                    thirdMinFrameAA = databaseSeconds;

                }

            }

        }

    }

    public List<String[]> retrieveResults() {

        String[] bestMatch = {currentMinVidAA, String.valueOf(currentMinFrameAA), ""};
        String[] secondMatch = {secondMinVidAA, String.valueOf(secondMinFrameAA), ""};
        String[] thirdMatch = {thirdMinVidAA, String.valueOf(thirdMinFrameAA), ""};

        List<String[]> results = new ArrayList<>();
        results.add(bestMatch);
        results.add(secondMatch);
        results.add(thirdMatch);

        return results;

    }

}