<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <title>Application Error</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <style>
        .container { max-width: 600px; margin-top: 50px; }
    </style>
</head>
<body>
    <div class="container text-center">
        <h2 class="mb-4 text-danger">⚠️ An Error Occurred</h2>
        
        <% 
        String errorMessage = (String) request.getAttribute("error");
        if (errorMessage == null) {
            errorMessage = "An unexpected error has occurred. Please check server logs.";
        }
        %>

        <p class="lead"><%= errorMessage %></p>
        
        <a href="<%= request.getContextPath() %>/index.jsp" class="btn btn-warning mt-4">Go Back to Form</a>
    </div>
</body>
</html>