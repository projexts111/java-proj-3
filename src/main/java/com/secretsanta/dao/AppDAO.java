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
     * Prioritizes robust, separated environment variables (DB_*) over the complex DATABASE_URL.
     */
    private static Connection getConnection() throws SQLException {
        // --- PRIMARY LOGIC: Use separated, clean environment variables ---
        String host = System.getenv("DB_HOST");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        String dbName = System.getenv("DB_NAME"); 
        
        // Check if the robust variables are available
        if (host != null && username != null && password != null && dbName != null) {
            
            // Build the JDBC URL from the separated components
            // Note: Cloud DBs universally require SSL, so we hard-code the parameters.
            String jdbcUrl = "jdbc:postgresql://" + host + "/" + dbName + "?ssl=true&sslmode=require";
            
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                // This exception should be caught by the Dockerfile fix, but kept as a safeguard.
                throw new SQLException("PostgreSQL driver not available. (Check Dockerfile COPY).", e);
            }

            // Return connection using the simple, direct variables
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        // --- FALLBACK LOGIC: If robust variables are missing, fall back to parsing DATABASE_URL ---
        
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            // Final fallback for local testing (WILL FAIL ON CLOUD)
            System.err.println("Database environment variables are missing. Using local fallback.");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/secretsanta", "user", "password");
        }

        try {
            // Normalize the URL scheme from "postgresql" to "postgres"
            URI dbUri = new URI(dbUrl.replaceFirst("^postgresql://", "postgres://"));
            
            String userInfo = dbUri.getUserInfo();
            if (userInfo == null) {
                // URISyntaxException is now caught and wrapped internally
                throw new URISyntaxException(dbUrl, "User info (username:password) is missing from the database URL.");
            }
            
            String fallbackUsername = userInfo.split(":")[0];
            String fallbackPassword = userInfo.split(":")[1];
            String fallbackHost = dbUri.getHost();
            int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
            String fallbackDbName = dbUri.getPath().replaceFirst("/", "");

            String jdbcUrl = "jdbc:postgresql://" + fallbackHost + ":" + port + "/" + fallbackDbName;
            
            // Hardcode SSL requirement, removing brittle host-length check
            jdbcUrl += "?ssl=true&sslmode=require";
            
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(jdbcUrl, fallbackUsername, fallbackPassword);
            
        } catch (URISyntaxException e) {
            // CATCH: URISyntaxException is caught here and wrapped into a SQLException.
            // This is why the Controller no longer needs to declare it.
            throw new SQLException("Database connection failed due to invalid URL format.", e);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not available. (Check Dockerfile COPY).", e);
        }
    }
    
    // ---------------------------------------------------------------------------------

    /**
     * Saves the list of generated matches to the database in a single transaction.
     * The method only throws SQLException, as URISyntaxException is handled internally in getConnection().
     */
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException { 
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
