package com.secretsanta.dao;

import com.secretsanta.model.MatchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppDAO {

    private static Connection getConnection() throws SQLException {
        // --- PRIMARY LOGIC: Use separated, clean environment variables ---
        String host = System.getenv("DB_HOST");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        String dbName = System.getenv("DB_NAME"); 
        
        if (host != null && username != null && password != null && dbName != null) {
            
            // Build the JDBC URL from the separated components
            String jdbcUrl = "jdbc:postgresql://" + host + "/" + dbName;
            
            // ⭐️ FINAL FIX: ADD sslfactory for cloud deployment stability
            jdbcUrl += "?ssl=true&sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
            
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL driver not available. (Check Dockerfile COPY).", e);
            }
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        // --- FALLBACK LOGIC: If robust variables are missing, fall back to parsing DATABASE_URL ---
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            System.err.println("Database environment variables are missing. Using local fallback.");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/secretsanta", "user", "password");
        }

        try {
            URI dbUri = new URI(dbUrl.replaceFirst("^postgresql://", "postgres://"));
            
            String userInfo = dbUri.getUserInfo();
            if (userInfo == null) {
                throw new URISyntaxException(dbUrl, "User info (username:password) is missing from the database URL.");
            }
            
            String fallbackUsername = userInfo.split(":")[0];
            String fallbackPassword = userInfo.split(":")[1];
            String fallbackHost = dbUri.getHost();
            int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
            String fallbackDbName = dbUri.getPath().replaceFirst("/", "");

            String jdbcUrl = "jdbc:postgresql://" + fallbackHost + ":" + port + "/" + fallbackDbName;
            
            // ⭐️ FINAL FIX: ADD sslfactory to the fallback logic too
            jdbcUrl += "?ssl=true&sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
            
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(jdbcUrl, fallbackUsername, fallbackPassword);
            
        } catch (URISyntaxException e) {
            throw new SQLException("Database connection failed due to invalid URL format.", e);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not available. (Check Dockerfile COPY).", e);
        }
    }
    
    // saveMatches method remains unchanged, as it only throws SQLException
    public void saveMatches(int groupId, List<MatchResult> matches) throws SQLException { 
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
