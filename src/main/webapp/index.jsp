<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // FINAL FIX: Use a reliable HTTP 302 redirect via response.sendRedirect()
    // This solves the issue where PaaS environments struggle with jsp:forward from the root index.
    response.sendRedirect(request.getContextPath() + "/form");
%>
