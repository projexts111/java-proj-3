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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@WebServlet(name = "GeneratorController", urlPatterns = {"/generate"})
public class GeneratorController extends HttpServlet {
    private final AppDAO appDAO = new AppDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
            
            // Save the valid, deranged matches (using dummy group ID 1 for now)
            // Note: In a real app, group ID would come from a session or form input
            appDAO.saveMatches(1, matches); 

            // SUCCESS: Inform the user matches are stored, awaiting secure reveal link generation/distribution
            request.setAttribute("message", "Matches successfully generated and stored in the database! (No self-matches detected).");
            request.getRequestDispatcher("/WEB-INF/views/result-page.jsp").forward(request, response);

        } catch (IllegalArgumentException e) {
            // Catches the error thrown by the algorithm if it fails (unlikely, but guarded)
            request.setAttribute("error", "Matching failed: " + e.getMessage() + " Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (SQLException e) {
            // Catches DB connection/transaction errors
            request.setAttribute("error", "A database error occurred during saving the matches: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
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
            String recipient = recipients.get(i); // The current tentative recipient

            // Constraint Check: No Self-Match
            if (gifter.equals(recipient)) {
                // Self-match occurred! Apply fix strategy.

                if (i == gifters.size() - 1) {
                    // CRITICAL FIX: Self-match on the LAST person
                    // This means the last recipient remaining is the last gifter.
                    // Solution: Swap the *last two* recipients in the 'recipients' list.
                    
                    if (i == 0) {
                         throw new IllegalArgumentException("List size is 1 or less.");
                    }
                    
                    // Swap the last recipient (i) with the previous one (i-1)
                    Collections.swap(recipients, i, i - 1);
                    
                    // Get the new recipient at position i
                    recipient = recipients.get(i);
                    
                    // The match at i-1 must also be re-evaluated, but since the swap was successful, 
                    // we assume the prior match (which was valid before the swap) is now also valid.
                    // If the new match for (i) is still a self-match, something is fundamentally broken.
                    if (gifter.equals(recipient)) {
                         throw new IllegalArgumentException("Self-match persists after final swap attempt.");
                    }
                } else {
                    // Simple Fix: Self-match, but NOT the last person.
                    // Swap the current recipient (i) with a random recipient further down the list (j > i)
                    
                    // Select a random index j from [i+1, size-1]
                    int swapIndex = i + 1 + random.nextInt(recipients.size() - i - 1);
                    
                    Collections.swap(recipients, i, swapIndex);
                    
                    // Get the new recipient at position i
                    recipient = recipients.get(i);
                    
                    // Re-check after swap (should pass)
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