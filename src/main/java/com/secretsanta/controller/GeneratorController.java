package com.secretsanta.controller;

import com.secretsanta.dao.AppDAO;
import com.secretsanta.model.MatchResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
// REMOVED: import java.net.URISyntaxException; // This is no longer necessary
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@WebServlet(name = "GeneratorController", urlPatterns = {"/generate", "/form"}) // <-- UPDATED: Handles /form path
public class GeneratorController extends HttpServlet {
    private final AppDAO appDAO = new AppDAO();

    // --- FIX: Add doGet to handle the /form endpoint redirect from index.jsp ---
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        
        // Dispatch securely to the JSP form view
        request.getRequestDispatcher("/WEB-INF/views/generator-form.jsp").forward(request, response);
    }
    // --------------------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException { 
        
        String[] namesArray = request.getParameterValues("participantName");
        
        // 1. Input Validation: Minimum participants check
        if (namesArray == null || namesArray.length < 3) {
            request.setAttribute("error", "Please enter at least three unique participants to run the Secret Santa draw.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            return;
        }

        // Convert array to modifiable list
        List<String> participants = List.of(namesArray);
        List<MatchResult> matches;
        
        try {
            matches = generateMatches(participants);
            
            // This call now only throws SQLException (URISyntaxException is handled in AppDAO)
            appDAO.saveMatches(1, matches); 

            // SUCCESS: Inform the user matches are stored, awaiting secure reveal link generation/distribution
            request.setAttribute("message", "Matches successfully generated and stored in the database! (No self-matches detected).");
            request.getRequestDispatcher("/WEB-INF/views/result-page.jsp").forward(request, response);

        } catch (IllegalArgumentException e) {
            // Catches the error thrown by the algorithm if it fails (unlikely, but guarded)
            request.setAttribute("error", "Matching failed: " + e.getMessage() + " Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (SQLException e) {
            // Catches DB connection/transaction errors and the wrapped URISyntaxException
            request.setAttribute("error", "A database error occurred during saving: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } 
        // ❌ REMOVED THE FOLLOWING BLOCK TO FIX COMPILATION ERROR:
        /* catch (URISyntaxException e) {
            request.setAttribute("error", "Configuration Error: The DATABASE_URL format is invalid. Check the environment variable format.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } */
    }

    /**
     * Implements the Derangement (No Self-Match) Algorithm.
     * @param participants The list of participant names.
     * @return A list of MatchResult objects where no Gifter is assigned themselves.
     */
    private List<MatchResult> generateMatches(List<String> participants) throws IllegalArgumentException {
        // Create two copies: one for gifters (fixed order) and one for recipients (to be shuffled)
        List<String> gifters = new ArrayList<>(participants);
        List<String> recipients = new ArrayList<>(participants);
        List<MatchResult> matches = new ArrayList<>();
        Random random = new Random();

        // 1. Shuffle recipients randomly
        Collections.shuffle(recipients, random); 

        for (int i = 0; i < gifters.size(); i++) {
            String gifter = gifters.get(i);
            String recipient = recipients.get(i); 

            // Constraint Check: No Self-Match
            if (gifter.equals(recipient)) {
                
                if (i == gifters.size() - 1) {
                    // CRITICAL FIX: Self-match on the LAST person
                    
                    if (i == 0) {
                        throw new IllegalArgumentException("List size is 1 or less.");
                    }
                    
                    // Swap the last recipient (i) with the previous one (i-1)
                    Collections.swap(recipients, i, i - 1);
                    
                    recipient = recipients.get(i);
                    
                    if (gifter.equals(recipient)) {
                        throw new IllegalArgumentException("Self-match persists after final swap attempt.");
                    }
                } else {
                    // Simple Fix: Self-match, but NOT the last person.
                    
                    int swapIndex = i + 1 + random.nextInt(recipients.size() - i - 1);
                    
                    Collections.swap(recipients, i, swapIndex);
                    
                    recipient = recipients.get(i);
                    
                    if (gifter.equals(recipient)) {
                        throw new IllegalArgumentException("Self-match persists after simple swap attempt.");
                    }
                }
            }
            
            // Record the final, valid match
            matches.add(new MatchResult(gifter, recipient));
        }

        return matches;
    }
}
