import javafx.application.Application;

public class Main {

    public static void main(String[] args) {

        // Create the audioParser instance
        AudioParser audioParser = new AudioParser();

        // Create outputs for the database WAV files - commented out once all DB WAV files analyzed
        //analyzeDatabase(audioParser);

        // Analyze the query video
        String queryPath = args[0];
        audioParser.analyzeQuery(queryPath);

        // Find the best matching video and print out results
        audioParser.findBestMatch();
        int bestMatchTimeZC = audioParser.currentMinTimeZC;
        String startTime = Integer.toString(bestMatchTimeZC);
        int bestMatchMinutesZC = bestMatchTimeZC/60;
        int bestMatchSecondsZC = bestMatchTimeZC%60;

        int bestMatchTimeAA = audioParser.currentMinTimeAA;
        int bestMatchMinutesAA = bestMatchTimeAA/60;
        int bestMatchSecondsAA = bestMatchTimeAA%60;

        System.out.println("The best match using zero-crossing rate is in " + audioParser.currentMinVidZC + " at time "
                + bestMatchMinutesZC + ":" + bestMatchSecondsZC);

        System.out.println("The best match using average amplitude is in " + audioParser.currentMinVidAA + " at time "
                + bestMatchMinutesAA + ":" + bestMatchSecondsAA);

        // Play the best match MP4 at the corresponding start time
        Application.launch(MP4Player.class, audioParser.currentMinVidZC, startTime);

    }

    public static void analyzeDatabase(AudioParser audioParser) {

        for (int fileIdx = 1; fileIdx < 21; fileIdx++) {

            String inputFile = "Database/WAV/video" + fileIdx + ".wav";
            String outputFile =  "Database/Output/ZC_output" + fileIdx + ".txt";
            String outputFile2 = "Database/Output/AA_output" + fileIdx + ".txt";
            audioParser.analyzeAudio(inputFile, outputFile,outputFile2);

        }

        System.out.println("Done analyzing the database file(s)!");

    }

}
