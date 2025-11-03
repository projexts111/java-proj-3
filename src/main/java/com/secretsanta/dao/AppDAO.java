package com.secretsanta.dao;

import com.secretsanta.model.MatchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppDAO {

    // The CRITICAL FIX for PostgreSQL URLs in PaaS environments (like Render)
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
        String username = userInfo.split(":")[0];
        String password = userInfo.split(":")[1];
        String host = dbUri.getHost();
        int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
        String dbName = dbUri.getPath().replaceFirst("/", "");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found.");
            throw new SQLException("PostgreSQL driver not available.", e);
        }

        return DriverManager.getConnection(jdbcUrl, username, password);
    }
    
    // Minimal logic to save the results
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException {
        String sql = "INSERT INTO matches (group_id, gifter_name, recipient_name) VALUES (?, ?, ?)";
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