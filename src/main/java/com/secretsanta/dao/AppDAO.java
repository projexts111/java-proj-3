package com.secretsanta.dao;

import com.secretsanta.model.MatchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppDAO {

    /**
     * Establishes a connection to the PostgreSQL database.
     * Includes fixes for PaaS environment variables (DATABASE_URL) and cloud SSL.
     */
    private static Connection getConnection() throws URISyntaxException, SQLException {
        // Look for the connection string in the environment variables
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            // Fallback for local testing (WILL FAIL ON CLOUD)
            System.err.println("DATABASE_URL environment variable is missing. Using local fallback.");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/secretsanta", "user", "password");
        }

        // FIX 1: Normalize the URL scheme from "postgresql" to "postgres"
        String correctedUrl = dbUrl.replaceFirst("^postgresql://", "postgres://");
        
        URI dbUri = new URI(correctedUrl);

        String userInfo = dbUri.getUserInfo();
        
        if (userInfo == null) {
            throw new URISyntaxException(dbUrl, "User info (username:password) is missing from the database URL.");
        }
        
        String username = userInfo.split(":")[0];
        String password = userInfo.split(":")[1];
        String host = dbUri.getHost();
        int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
        String dbName = dbUri.getPath().replaceFirst("/", "");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        
        // --- CRITICAL FIX 2: Conditionally apply SSL based on host type ---
        // Internal hosts typically fail if forced to use SSL. External hosts require it.
        // Render's internal hostnames are typically much shorter than external FQDNs.
        boolean isInternalHost = host.length() < 30; 
        
        if (!isInternalHost) {
            // Apply SSL fix only if connecting externally (long hostname)
            if (!jdbcUrl.contains("?")) {
                jdbcUrl += "?ssl=true&sslmode=require";
            } else {
                jdbcUrl += "&ssl=true&sslmode=require";
            }
        } 
        // ---------------------------------------------------------------------------------

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found.");
            throw new SQLException("PostgreSQL driver not available.", e);
        }

        return DriverManager.getConnection(jdbcUrl, username, password);
    }
    
    /**
     * Saves the list of generated matches to the database in a single transaction.
     * Method declares the checked exceptions thrown by getConnection().
     */
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException, URISyntaxException { 
        String sql = "INSERT INTO matches (group_id, gifter_name, recipient_name) VALUES (?, ?, ?)";
        
        // getConnection() call is placed inside the try-with-resources block.
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Begin Transaction - CRITICAL for data integrity
            conn.setAutoCommit(false); 

            for (MatchResult match : matches) {
                pstmt.setInt(1, groupId);
                pstmt.setString(2, match.getGifter());
                pstmt.setString(3, match.getRecipient());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit(); // Commit Transaction
        }
    }
}
