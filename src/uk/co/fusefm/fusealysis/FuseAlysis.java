package uk.co.fusefm.fusealysis;

import java.sql.*;

/**
 * Analysis daemon accompanying Fusic. Monitors uploaded files and analyses
 * their tops and tails, before recording this data in MySQL
 *
 * @author Andrew Bonney
 */
public class FuseAlysis {

    private static String mysqlServer = null, mysqlPort = "3306",
            mysqlDatabase = "playoutsystem", mysqlUsername = "playoutsystem",
            mysqlPassword = "", directoryBase = null;
    private static Connection mysqlConn;
    private static int inVol = 0, outVol = 0, checkFrequency = 1;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // Analyse command line args
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                    mysqlServer = args[i + 1];
                    i++;
                    break;
                case "-p":
                    mysqlPort = args[i + 1];
                    i++;
                    break;
                case "-t":
                    mysqlDatabase = args[i + 1];
                    i++;
                    break;
                case "-u":
                    mysqlUsername = args[i + 1];
                    i++;
                    break;
                case "-k":
                    mysqlPassword = args[i + 1];
                    i++;
                    break;
                case "-d":
                    directoryBase = args[i + 1];
                    i++;
                    break;
                case "-f":
                    checkFrequency = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
            }
        }

        // Check all args exist
        if (mysqlServer == null) {
            System.out.println("No MySQL server specified. Exiting");
            System.exit(0);
        } else if (directoryBase == null) {
            System.out.println("No base directory specified. Exiting");
            System.exit(0);
        }

        directoryBase = directoryBase.replace("\\", "/");
        if (!directoryBase.endsWith("/")) {
            directoryBase += "/";
        }

        // Connect to the MySQL server
        doConnect();
        if (mysqlConn == null) {
            System.out.println("MySQL connection failed. Exiting");
            System.exit(0);
        }

        // Get in and out volumes, and check frequency from the server
        refreshSettings();
        if (inVol == 0 && outVol == 0) {
            System.out.println("Could not get in and out volume settings from MySQL server. Exiting");
            try {
                mysqlConn.close();
            } catch (SQLException ex) {
                // Do nothing
            }
            System.exit(0);
        }

        // Initialise the analyser and queue
        FileAnalyser fa = new FileAnalyser(directoryBase, inVol, outVol);
        if (!fa.getInitStatus()) {
            try {
                mysqlConn.close();
            } catch (SQLException ex) {
                // Do nothing
            }
            System.exit(0);
        }
        FileQueue fq = new FileQueue(mysqlConn);

        System.out.println("FuseAlysis successfully initialised...");

        while (true) {
            // Populate the queue
            System.out.println("Updating directory listing...");
            while (!fq.populateAnalysisList()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    System.out.println("Sleep failed!");
                }
                doConnect();
            }

            // While more tracks are available, analyse them
            while (fq.hasNext()) {
                String currentTrack = fq.next();
                System.out.println("Currently analysing " + directoryBase + currentTrack);
                fq.setInPoint(fa.getInTime(currentTrack));
                fq.setOutPoint(fa.getOutTime(currentTrack));
                fq.saveTrack();
                System.out.println("Saving settings for " + directoryBase + currentTrack);
            }

            // Sleep for a minute (default) before checking for more tracks
            System.out.println("All done! Waiting for " + checkFrequency + " minute(s)...");
            try {
                Thread.sleep(checkFrequency * 60000);
            } catch (InterruptedException ex) {
                System.out.println("Sleep failed!");
            }

            refreshSettings();
            if (inVol == 0 && outVol == 0) {
                System.out.println("Could not get in and out volume settings from MySQL server. Exiting");
                try {
                    mysqlConn.close();
                } catch (SQLException ex) {
                    // Do nothing
                }
                System.exit(0);
            }
            fa.updateSettings(inVol, outVol);
        }
    }

    private static void doConnect() {
        mysqlConn = null;
        try {
            mysqlConn = DriverManager.getConnection("jdbc:mysql://" + mysqlServer + ":" + mysqlPort + "/" + mysqlDatabase + "?"
                    + "user=" + mysqlUsername + "&password=" + mysqlPassword);
        } catch (SQLException ex) {
            System.out.println("MySQL connection failed. Exiting");
            System.out.println(ex.toString());
            System.exit(0);
        }
    }

    private static void refreshSettings() {
        try {
            PreparedStatement inOutLevels = mysqlConn.prepareStatement("SELECT "
                    + "Setting_Name,Setting_Value FROM tbl_settings WHERE "
                    + "Setting_Name = 'analysis_daemon_frequency_minutes' "
                    + "OR Setting_Name = 'fade_in_detection_level' OR "
                    + "Setting_Name = 'fade_out_detection_level'");
            ResultSet inOutResults = inOutLevels.executeQuery();
            while (inOutResults.next()) {
                switch (inOutResults.getString("Setting_Name")) {
                    case "analysis_daemon_frequency_minutes":
                        checkFrequency = inOutResults.getInt("Setting_Value");
                        break;
                    case "fade_in_detection_level":
                        inVol = inOutResults.getInt("Setting_Value");
                        break;
                    case "fade_out_detection_level":
                        outVol = inOutResults.getInt("Setting_Value");
                        break;
                }
            }
            inOutResults.close();
            inOutLevels.close();
        } catch (SQLException ex) {
            System.out.println("Could not get in and out volume settings from MySQL server. Exiting");
            System.out.println(ex.toString());
            System.exit(0);
        }
    }
}
