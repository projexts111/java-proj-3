package com.secretsanta.dao;

import com.secretsanta.model.MatchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppDAO {

    private static Connection getConnection() throws URISyntaxException, SQLException {
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
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
        
        // --- CRITICAL FIX 2: Conditionally apply SSL ---
        // If the host is short (Internal Hostname), skip SSL enforcement.
        // The specific host 'dpg-d447o914d56c7385ktbg-a' has 28 characters.
        boolean isInternalHost = host.length() < 30; 
        
        if (!isInternalHost) {
            // Only apply SSL fix if connecting externally
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
    
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException, URISyntaxException { 
        String sql = "INSERT INTO matches (group_id, gifter_name, recipient_name) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); 

            for (MatchResult match : matches) {
                pstmt.setInt(1, groupId);
                pstmt.setString(2, match.getGifter());
                pstmt.setString(3, match.getRecipient());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit(); 
        }
    }
}
