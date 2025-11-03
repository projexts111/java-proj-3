<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Generation Success</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <style>
        .container { max-width: 600px; margin-top: 50px; }
    </style>
</head>
<body>
    <div class="container text-center">
        <h2 class="mb-4 text-success">🎉 Matches Generated Successfully!</h2>
        <p class="lead"><%= request.getAttribute("message") %></p>
        
        <blockquote class="blockquote">
            <p class="mb-0">The matches have been securely stored in the database.</p>
            <footer class="blockquote-footer">Next step: Implement and distribute the unique, secret reveal links/tokens to each participant.</footer>
        </blockquote>
        
        <a href="<%= request.getContextPath() %>/index.jsp" class="btn btn-info mt-4">Start New Draw</a>
    </div>
</body>
</html>