<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Secret Santa Generator</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <style>
        .container { max-width: 600px; margin-top: 50px; }
    </style>
</head>
<body>
    <div class="container">
        <h2 class="mb-4">🎄 Secret Santa Participant Entry</h2>
        <p class="text-danger">A minimum of 3 participants is required for a successful draw!</p>
        
        <form action="generate" method="post">
            <div id="participants-list">
                <div class="form-group">
                    <label for="name1">Participant 1 (Required)</label>
                    <input type="text" class="form-control" name="participantName" required>
                </div>
                <div class="form-group">
                    <label for="name2">Participant 2 (Required)</label>
                    <input type="text" class="form-control" name="participantName" required>
                </div>
                <div class="form-group">
                    <label for="name3">Participant 3 (Required)</label>
                    <input type="text" class="form-control" name="participantName" required>
                </div>
            </div>

            <button type="button" class="btn btn-secondary mb-3" onclick="addParticipant()">+ Add Participant</button>
            <button type="submit" class="btn btn-primary btn-block">Generate Matches</button>
        </form>

        <%-- Display Error Messages passed from the Controller --%>
        <div class="mt-4">
            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <strong>Generation Failed:</strong> <%= request.getAttribute("error") %>
                </div>
            <% } %>
        </div>

    </div>

    <script>
        let participantCount = 3;
        function addParticipant() {
            participantCount++;
            const list = document.getElementById('participants-list');
            const newDiv = document.createElement('div');
            newDiv.className = 'form-group';
            newDiv.innerHTML = `
                <label for="name${participantCount}">Participant ${participantCount}</label>
                <input type="text" class="form-control" name="participantName" required>
            `;
            list.appendChild(newDiv);
        }
    </script>
</body>
</html>