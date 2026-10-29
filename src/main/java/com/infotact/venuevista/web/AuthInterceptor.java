package com.infotact.venuevista.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Keeps the organizer console behind a session login. */
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_KEY = "vv.admin";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        var session = req.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY))) return true;
        if (req.getRequestURI().startsWith("/admin/api/")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            res.sendRedirect("/admin/login");
        }
        return false;
    }
}
