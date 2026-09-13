package sg.edu.ntu.taskflowapi.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A task in TaskFlow. Plain POJO: no Spring annotations, Lombok generates the
 * getters, setters and the no-args constructor.
 *
 * status and priority carry the exact strings the task-manager frontend uses
 * ("todo" | "in-progress" | "done" and "low" | "medium" | "high"). completed is
 * the flag the assignment asks for and always mirrors status == "done"; the
 * service keeps the two in sync.
 *
 * Deliberately no all-args constructor: with Jackson 3 (Spring Boot 4) a
 * multi-argument constructor is picked up as the JSON creator, and a request
 * body that leaves out "completed" then fails with "Cannot map null into
 * type boolean". With only the no-args constructor Jackson uses the setters
 * and missing fields simply keep their defaults.
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({ "id", "title", "description", "status", "priority", "completed" })
public class Task {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String priority;
  private boolean completed;
}
