package sg.edu.ntu.taskflowapi.exceptions;

public class SummaryUnavailableException extends RuntimeException {
  public SummaryUnavailableException(String message) {
    super(message);
  }

  public SummaryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
