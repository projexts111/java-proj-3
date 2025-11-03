package com.secretsanta.dao;

import com.secretsanta.model.MatchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppDAO {

    // NOTE: getConnection is now correctly declaring both checked exceptions
    private static Connection getConnection() throws URISyntaxException, SQLException {
        // Look for the connection string in the environment variables
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            System.err.println("DATABASE_URL environment variable is missing. Using local fallback.");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/secretsanta", "user", "password");
        }

        // URI constructor throws URISyntaxException - THIS IS THE SOURCE OF THE ERROR
        URI dbUri = new URI(dbUrl); 
        String userInfo = dbUri.getUserInfo();
        
        // Handle potential null userInfo (e.g., if URI is incomplete)
        if (userInfo == null) {
            throw new URISyntaxException(dbUrl, "User info (username:password) is missing from the database URL.");
        }
        
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
    
    // FIX APPLIED HERE: Added 'throws URISyntaxException' to the method signature
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException, URISyntaxException { 
        String sql = "INSERT INTO matches (group_id, gifter_name, recipient_name) VALUES (?, ?, ?)";
        // Since getConnection() is called here, this method must throw the same exceptions.
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
