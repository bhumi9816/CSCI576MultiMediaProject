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

