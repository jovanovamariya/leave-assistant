package dev.leaveassistant.leave;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem responses. The base class already maps bad JSON and failed
 * validation to 400; the handlers below add our own errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** A business rule was broken: the request was understood but conflicts with the rules. */
    @ExceptionHandler(RuleViolationException.class)
    public ProblemDetail ruleViolation(RuleViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Rule violation", ex.getMessage());
    }

    @ExceptionHandler(LeaveRequestNotFoundException.class)
    public ProblemDetail notFound(LeaveRequestNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    /** For example a start date after the end date. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidInput(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
