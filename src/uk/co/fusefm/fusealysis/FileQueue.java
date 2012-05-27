package uk.co.fusefm.fusealysis;

import java.sql.Connection;

/**
 * Maintains a queue of tracks to be analysed
 * @author Andrew Bonney
 */
public class FileQueue {
    private Connection dbConn;
    private int currentTrackID, currentInTime, currentOutTime;
    
    /**
     * @param db Database connection instance
     */
    public FileQueue(Connection db) {
        dbConn = db;
    }
    
    /**
     * Generate a list of tracks to be analysed
     * @return False on MySQL connection failure, true otherwise
     */
    public boolean populateAnalysisList() {
        
    }
    
    /**
     * Check if any more non-analysed tracks exist in the current queue
     * @return 
     */
    public boolean hasNext() {
        
    }
    
    /**
     * Get the location of the next track to be analysed
     * @return 
     */
    public String next() {
        currentInTime = 0;
        currentOutTime = 0;
    }
    
    /**
     * Set the in time for the current track
     * @param inTime 
     */
    public void setInPoint(int inTime) {
        currentInTime = inTime;
    }
    
    /**
     * Set the out time for the current track
     * @param outTime 
     */
    public void setOutPoint(int outTime) {
        currentOutTime = outTime;
    }
    
    /**
     * Save the changes to the database
     */
    public void saveTrack() {
        
    }
}
