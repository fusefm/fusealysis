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

        // Connect to the MySQL server
        doConnect();
        if (mysqlConn == null) {
            System.out.println("MySQL connection failed. Exiting");
            System.exit(0);
        }

        // Get in and out volumes from the server
        int inVol = 0, outVol = 0;
        try {
            PreparedStatement inOutLevels = mysqlConn.prepareStatement("");
            ResultSet executeQuery = inOutLevels.executeQuery();
        } catch (SQLException ex) {
            System.out.println("Could not get in and out volume settings from MySQL server. Exiting");
            System.out.println(ex.toString());
            System.exit(0);
        }
        if (inVol == 0 && outVol == 0) {
            System.out.println("Could not get in and out volume settings from MySQL server. Exiting");
            System.exit(0);
        }

        // Initialise the analyser and queue
        FileAnalyser fa = new FileAnalyser(directoryBase, inVol, outVol);
        FileQueue fq = new FileQueue(mysqlConn);

        while (true) {
            // Populate the queue
            if (!fq.populateAnalysisList()) {
                doConnect();
            }

            // While more tracks are available, analyse them
            while (fq.hasNext()) {
                String currentTrack = fq.next();
                fq.setInPoint(fa.getInTime(currentTrack));
                fq.setOutPoint(fa.getOutTime(currentTrack));
                fq.saveTrack();
            }
            
            // Sleep for a minute before checking for more tracks
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                System.out.println("Sleep failed!");
            }
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
}
