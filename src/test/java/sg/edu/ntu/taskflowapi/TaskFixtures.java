package sg.edu.ntu.taskflowapi;

import sg.edu.ntu.taskflowapi.model.Task;

// Builds Task objects for tests; the model deliberately has no all-args constructor
public final class TaskFixtures {

  private TaskFixtures() {
  }

  public static Task task(Long id, String title, String description, String status, String priority) {
    Task task = new Task();
    task.setId(id);
    task.setTitle(title);
    task.setDescription(description);
    task.setStatus(status);
    task.setPriority(priority);
    task.setCompleted("done".equals(status));
    return task;
  }
}
