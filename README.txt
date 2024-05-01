// Descriptor: Color Histogram, using threshold mechanism

Note: Slightly less precise and is bound to give false positive.

But given the challenges with time and for me setting opencv/javacv native library setup
and testing indicating Shot Boundary detection not outputting accurate results.
This is the descriptor I have decided to move forward with.


======
Setup
======
1. Need ffmpeg [source file, and executable]
Link to source file - git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg
Executable - https://evermeet.cx/ffmpeg/ffmpeg-114988-g8c62d77139.zip [you can add executable to your java project path]

2.
$ javac Main.java
$ java Main.java <query_video>

3. MatchedVideos contains the subsequent database video, matching with the query video. One of the them is the right video.
Given audio parser and motion detection, we can try to identify the exact match.




===========
Code Clean-up
===========

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

// Check if sequence similarity exceeds the threshold
            /*if (sequenceSimilarity >= HISTOGRAM_THRESHOLD) {
                matchedVideos.add(new MatchedVideo(videoName, 0.0)); // Start time is not relevant in this case
            }*/

 // Check if similarity exceeds the threshold
           /* if (colorHistogramSimilarity >= HISTOGRAM_THRESHOLD) {
                matchedVideos.add(new MatchedVideo(videoName, 0.0)); // Start time is not relevant in this case
            }*/


  private static double calculateShotBoundarySimilarity(List<Integer> queryShotBoundaries, List<Integer> databaseShotBoundaries) {
          // Calculate the number of common shot boundaries between the query and database videos
          long commonShotBoundaries = queryShotBoundaries.stream()
                  .filter(databaseShotBoundaries::contains)
                  .count();

          // Normalize similarity score by dividing by the total number of shot boundaries in the query video
          double similarity = (double) commonShotBoundaries / queryShotBoundaries.size();

          return similarity;
      }