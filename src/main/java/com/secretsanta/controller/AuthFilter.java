package com.secretsanta.controller;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

// NOTE: This project doesn't require a strict login, but we include the filter
// structure to maintain the best practice from the previous project.
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        // Currently, we allow all requests to pass through since the app is simple/public.
        // In a secured version (like the previous project), session checks would go here:
        /* HttpServletRequest req = (HttpServletRequest) request;
        if (req.getSession().getAttribute("userId") == null) {
            // Redirect to login page
            ((HttpServletResponse) response).sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }
        */
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}