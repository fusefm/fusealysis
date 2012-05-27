package uk.co.fusefm.fusealysis;

/**
 * Analyses tracks' tops and tails using the BASS library
 * @author Andrew Bonney
 */
public class FileAnalyser {
    private final String baseDirectory;
    private final int trackFrontVolume, trackBackVolume;
    
    /**
     * @param baseDir Base directory for tracks to analyse
     * @param frontVol Volume level at which to set in points
     * @param backVol Volume level at which to set out points
     */
    public FileAnalyser (String baseDir, int frontVol, int backVol) {
        baseDirectory = baseDir;
        trackFrontVolume = frontVol;
        trackBackVolume = backVol;
    }
    
    /**
     * Analyse the specified track and return its in time
     * @param relativePath
     * @return 
     */
    public int getInTime(String relativePath) {
        
    }
    
    /**
     * Analyse the specified track and return its out time
     * @param relativePath
     * @return 
     */
    public int getOutTime(String relativePath) {
        
    }
}
