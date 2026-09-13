package sg.edu.ntu.taskflowapi.service;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import sg.edu.ntu.taskflowapi.exceptions.InvalidTaskException;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;
import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;

@Service
public class TaskService {

  private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

  public static final String STATUS_TODO = "todo";
  public static final String STATUS_IN_PROGRESS = "in-progress";
  public static final String STATUS_DONE = "done";
  public static final Set<String> VALID_STATUSES = Set.of(STATUS_TODO, STATUS_IN_PROGRESS, STATUS_DONE);

  public static final String DEFAULT_PRIORITY = "medium";
  public static final Set<String> VALID_PRIORITIES = Set.of("low", DEFAULT_PRIORITY, "high");

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public List<Task> findAllTasks() {
    return taskRepository.findAll();
  }

  public Task findTaskById(Long id) {
    return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }

  public Task createTask(Task task) {
    // ids come from the repository, never from the client
    task.setId(null);
    normalise(task);
    Task createdTask = taskRepository.save(task);
    logger.info("Created task {} \"{}\"", createdTask.getId(), createdTask.getTitle());
    return createdTask;
  }

  // Full replacement: every field is taken from the request body (fields left
  // out fall back to the defaults applied in normalise)
  public Task updateTask(Long id, Task task) {
    Task taskToUpdate = findTaskById(id);
    normalise(task);
    taskToUpdate.setTitle(task.getTitle());
    taskToUpdate.setDescription(task.getDescription());
    taskToUpdate.setStatus(task.getStatus());
    taskToUpdate.setPriority(task.getPriority());
    taskToUpdate.setCompleted(task.isCompleted());
    Task updatedTask = taskRepository.save(taskToUpdate);
    logger.info("Updated task {} (status: {})", id, updatedTask.getStatus());
    return updatedTask;
  }

  public void deleteTask(Long id) {
    findTaskById(id);
    taskRepository.deleteById(id);
    logger.info("Deleted task {}", id);
  }

  public Task markTaskAsComplete(Long id) {
    Task task = findTaskById(id);
    task.setStatus(STATUS_DONE);
    task.setCompleted(true);
    Task completedTask = taskRepository.save(task);
    logger.info("Marked task {} as complete", id);
    return completedTask;
  }

  // Validates the incoming fields and keeps status and completed consistent.
  // status is the source of truth; completed only matters when status is absent,
  // which is how a client that knows only the completed flag can still set "done".
  private void normalise(Task task) {
    if (task.getTitle() == null || task.getTitle().isBlank()) {
      throw new InvalidTaskException("title is required");
    }
    task.setTitle(task.getTitle().trim());

    if (task.getStatus() == null) {
      task.setStatus(task.isCompleted() ? STATUS_DONE : STATUS_TODO);
    } else if (!VALID_STATUSES.contains(task.getStatus())) {
      throw new InvalidTaskException("status must be one of: todo, in-progress, done");
    }

    if (task.getPriority() == null) {
      task.setPriority(DEFAULT_PRIORITY);
    } else if (!VALID_PRIORITIES.contains(task.getPriority())) {
      throw new InvalidTaskException("priority must be one of: low, medium, high");
    }

    task.setCompleted(STATUS_DONE.equals(task.getStatus()));
  }
}
