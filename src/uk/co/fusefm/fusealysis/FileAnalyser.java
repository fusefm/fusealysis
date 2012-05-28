package uk.co.fusefm.fusealysis;

import java.util.logging.Level;
import java.util.logging.Logger;
import jouvieje.bass.Bass;
import jouvieje.bass.BassInit;
import jouvieje.bass.structures.HSTREAM;
import jouvieje.bass.defines.BASS_ACTIVE;
import jouvieje.bass.defines.BASS_DEVICE;
import jouvieje.bass.defines.BASS_POS;
import jouvieje.bass.defines.BASS_STREAM;
import jouvieje.bass.exceptions.BassException;

/**
 * Analyses tracks' tops and tails using the BASS library
 *
 * @author Andrew Bonney
 */
public class FileAnalyser {

    private final String baseDirectory;
    private int trackFrontVolume, trackBackVolume;
    private boolean loadSuccess = false;

    /**
     * @param baseDir Base directory for tracks to analyse
     * @param frontVol Volume level at which to set in points
     * @param backVol Volume level at which to set out points
     */
    public FileAnalyser(String baseDir, int frontVol, int backVol) {
        baseDirectory = baseDir;
        trackFrontVolume = frontVol;
        trackBackVolume = backVol;
        try {
            BassInit.loadLibraries();
        } catch (BassException e) {
            System.out.println("NativeBass error! " + e.getMessage());
            return;
        }

        if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
            System.out.println("Error!  NativeBass library version "
                    + "(" + BassInit.NATIVEBASS_LIBRARY_VERSION() + ") is "
                    + "different to jar version ("
                    + BassInit.NATIVEBASS_JAR_VERSION() + ")\n");
            return;
        }
        if (!Bass.BASS_Init(-1, 44100, BASS_DEVICE.BASS_DEVICE_MONO, null, null)) {
            System.out.println("Could not initialise BASS");
            return;
        }
        loadSuccess = true;
    }

    public boolean getInitStatus() {
        return loadSuccess;
    }

    public void updateSettings(int frontVol, int backVol) {
        trackFrontVolume = frontVol;
        trackBackVolume = backVol;
    }

    /**
     * Analyse the specified track and return its in time
     *
     * @param relativePath
     * @return
     */
    public double getInTime(String relativePath) {
        return getTime(relativePath,false);
    }

    /**
     * Analyse the specified track and return its out time
     *
     * @param relativePath
     * @return
     */
    public double getOutTime(String relativePath) {
        return getTime(relativePath,true);
    }
    
    private double getTime(String relativePath, boolean reverse) {
        if (reverse) {
            //TODO: Not yet implemented
            return 0;
        }
        String extension = relativePath.substring(relativePath.lastIndexOf(".")+1,relativePath.length());
        if (extension.equalsIgnoreCase("flac") || extension.equalsIgnoreCase("m4a")) {
            System.out.println("FLAC and M4A currently unsupported.");
            return 0;
        }
        String fileLoc = baseDirectory + relativePath;
        double trackPos = 0;
        HSTREAM stream = Bass.BASS_StreamCreateFile(false, fileLoc, BASS_STREAM.BASS_STREAM_AUTOFREE, 0, 0);
        int errorCode = Bass.BASS_ErrorGetCode();
        if (errorCode != 0) {
            System.out.println("Error opening file " + fileLoc + " code " + errorCode);
            System.exit(0);
        }
        int streamID = stream.asInt();
        Bass.BASS_ChannelPlay(streamID, true);
        while (Bass.BASS_ChannelIsActive(streamID) == BASS_ACTIVE.BASS_ACTIVE_PLAYING) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                // Do nothing
            }
            int bassLevel = Bass.BASS_ChannelGetLevel(streamID);
            if (bassLevel <= 0) {
                continue;
            }
            String levelString = Integer.toBinaryString(bassLevel);
            int channelLevel = Integer.parseInt(new StringBuffer(levelString.substring(0, 16)).reverse().toString(), 2);
            long bytePosition = Bass.BASS_ChannelGetPosition(streamID, BASS_POS.BASS_POS_BYTE);
            double currentPos = Bass.BASS_ChannelBytes2Seconds(streamID, bytePosition);
            if (currentPos > 10) {
                System.out.println("Tried first 10 seconds, no audio found. Skipping...");
                break;
            }
            if (channelLevel >= trackFrontVolume) {
                trackPos = currentPos;
                break;
            }
        }
        Bass.BASS_ChannelStop(streamID);
        Bass.BASS_StreamFree(stream);
        return trackPos;
    }
}
