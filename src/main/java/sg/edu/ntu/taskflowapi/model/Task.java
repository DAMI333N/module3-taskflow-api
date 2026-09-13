package sg.edu.ntu.taskflowapi.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A task in TaskFlow. Plain POJO: no Spring annotations, Lombok generates the
 * getters, setters and constructors.
 *
 * status and priority carry the exact strings the task-manager frontend uses
 * ("todo" | "in-progress" | "done" and "low" | "medium" | "high"). completed is
 * the flag the assignment asks for and always mirrors status == "done"; the
 * service keeps the two in sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "id", "title", "description", "status", "priority", "completed" })
public class Task {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String priority;
  private boolean completed;
}
