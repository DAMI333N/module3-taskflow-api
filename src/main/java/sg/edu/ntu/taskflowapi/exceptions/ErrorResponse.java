package sg.edu.ntu.taskflowapi.exceptions;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// Body returned for every error the API reports itself
@JsonPropertyOrder({ "status", "message", "timestamp" })
public record ErrorResponse(int status, String message, LocalDateTime timestamp) {
}
