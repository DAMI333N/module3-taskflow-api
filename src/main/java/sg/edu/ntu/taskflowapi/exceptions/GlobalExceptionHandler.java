package sg.edu.ntu.taskflowapi.exceptions;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Central place for the API's error responses, so the controller stays free of try/catch
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(TaskNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
    logger.warn(ex.getMessage());
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(InvalidTaskException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTask(InvalidTaskException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // e.g. GET /api/tasks/abc, where "abc" cannot be converted to a Long id
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return build(HttpStatus.BAD_REQUEST, "Task id must be a number, got: " + ex.getValue());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
  }

  @ExceptionHandler(SummaryUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleSummaryUnavailable(SummaryUnavailableException ex) {
    logger.error(ex.getMessage(), ex.getCause());
    return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
    ErrorResponse body = new ErrorResponse(status.value(), message, LocalDateTime.now());
    return new ResponseEntity<>(body, status);
  }
}
