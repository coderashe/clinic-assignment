package com.clinic.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Turns business failures into structured "[CODE] message" banners on the page the user came from. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClinicException.class)
    public String business(ClinicException ex, HttpServletRequest req, RedirectAttributes ra) {
        return fail("[" + ex.getCode() + "] " + ex.getMessage(), req, ra);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public String concurrency(HttpServletRequest req, RedirectAttributes ra) {
        return fail("[CONCURRENT_UPDATE] This record was changed by someone else. Please review and retry.", req, ra);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integrity(HttpServletRequest req, RedirectAttributes ra) {
        return fail("[DATA_CONFLICT] The data conflicts with an existing record.", req, ra);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String badInput(HttpServletRequest req, RedirectAttributes ra) {
        return fail("[INVALID_INPUT] One or more fields are missing or malformed.", req, ra);
    }

    private String fail(String message, HttpServletRequest req, RedirectAttributes ra) {
        ra.addFlashAttribute("error", message);
        return "redirect:" + backPath(req);
    }

    /** Same-origin path of the Referer for form posts; "/" for GETs (avoids redirect loops). */
    private String backPath(HttpServletRequest req) {
        if ("GET".equalsIgnoreCase(req.getMethod())) return "/";
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                URI u = URI.create(ref);
                String p = u.getRawPath();
                if (p != null && p.startsWith("/")) {
                    return p + (u.getRawQuery() == null ? "" : "?" + u.getRawQuery());
                }
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return "/";
    }
}
