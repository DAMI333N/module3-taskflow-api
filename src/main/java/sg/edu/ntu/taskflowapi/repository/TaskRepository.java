package sg.edu.ntu.taskflowapi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import sg.edu.ntu.taskflowapi.model.Task;

@Repository
public class TaskRepository {

  // Initialised inline so the collection is never null
  private final List<Task> tasks = new ArrayList<>();

  // A simple counter for ids. AtomicLong because this bean is a shared singleton.
  private final AtomicLong nextId = new AtomicLong(1);

  // Preload the same six tasks the task-manager frontend seeds itself with,
  // so the two look identical once they are wired together (ids 1 to 6).
  public TaskRepository() {
    save(seed("Set up project repository",
        "Initialise a Git repo, add a .gitignore, and push the first commit.", "done", "high"));
    save(seed("Design database schema",
        "Draft the ERD for the contacts and deals tables.", "done", "high"));
    save(seed("Build login page",
        "Create a login form with email and password fields and basic validation.", "in-progress", "high"));
    save(seed("Write unit tests for reducer",
        "Cover ADD_TASK, DELETE_TASK, and SET_FILTER with at least two cases each.", "todo", "medium"));
    save(seed("Update README",
        "Add setup instructions, a screenshot, and a description of the tech stack.", "todo", "low"));
    save(seed("Deploy to Vercel",
        "Connect the GitHub repo to Vercel and configure environment variables.", "todo", "medium"));
  }

  private static Task seed(String title, String description, String status, String priority) {
    Task task = new Task();
    task.setTitle(title);
    task.setDescription(description);
    task.setStatus(status);
    task.setPriority(priority);
    task.setCompleted("done".equals(status));
    return task;
  }

  // Get All (a copy, so callers cannot change the stored list by accident)
  public List<Task> findAll() {
    return new ArrayList<>(tasks);
  }

  // Get One: loop through the list to find a task by its id
  public Optional<Task> findById(Long id) {
    for (Task task : tasks) {
      if (task.getId().equals(id)) {
        return Optional.of(task);
      }
    }
    return Optional.empty();
  }

  // Create (no id yet) or save back an existing task (id already assigned)
  public Task save(Task task) {
    if (task.getId() == null) {
      task.setId(nextId.getAndIncrement());
      tasks.add(task);
      return task;
    }
    int index = indexOf(task.getId());
    if (index == -1) {
      tasks.add(task);
    } else {
      tasks.set(index, task);
    }
    return task;
  }

  // Delete
  public void deleteById(Long id) {
    tasks.removeIf(task -> task.getId().equals(id));
  }

  private int indexOf(Long id) {
    for (int i = 0; i < tasks.size(); i++) {
      if (tasks.get(i).getId().equals(id)) {
        return i;
      }
    }
    return -1;
  }
}
