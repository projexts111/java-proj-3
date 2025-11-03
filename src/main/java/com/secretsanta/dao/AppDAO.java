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
     * Includes the CRITICAL FIX for parsing PaaS environment variables (DATABASE_URL)
     * and ensuring SSL is used for cloud connections.
     */
    private static Connection getConnection() throws URISyntaxException, SQLException {
        // Look for the connection string in the environment variables
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            // Placeholder for local testing (WILL FAIL ON RENDER/PaaS)
            System.err.println("DATABASE_URL environment variable is missing. Using local fallback.");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/secretsanta", "user", "password");
        }

        // Parse the complex URL format: postgres://user:password@host:port/dbname
        URI dbUri = new URI(dbUrl);
        String userInfo = dbUri.getUserInfo();
        
        // Handle potential null userInfo (e.g., if URI is incomplete)
        if (userInfo == null) {
            // Throw URISyntaxException to force failure early if config is bad
            throw new URISyntaxException(dbUrl, "User info (username:password) is missing from the database URL.");
        }
        
        String username = userInfo.split(":")[0];
        String password = userInfo.split(":")[1];
        String host = dbUri.getHost();
        int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
        String dbName = dbUri.getPath().replaceFirst("/", "");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        
        // --- CRITICAL SSL FIX: Add the required parameters for PaaS database connections ---
        // Without this, the application will crash during startup on platforms requiring SSL.
        if (!jdbcUrl.contains("?")) {
            jdbcUrl += "?ssl=true&sslmode=require";
        } else {
            jdbcUrl += "&ssl=true&sslmode=require";
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
     * The method declares the checked exceptions thrown by getConnection().
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
