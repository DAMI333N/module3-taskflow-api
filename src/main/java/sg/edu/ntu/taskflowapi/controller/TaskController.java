package sg.edu.ntu.taskflowapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.service.TaskService;
import sg.edu.ntu.taskflowapi.service.TaskSummaryService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;
  private final TaskSummaryService taskSummaryService;

  public TaskController(TaskService taskService, TaskSummaryService taskSummaryService) {
    this.taskService = taskService;
    this.taskSummaryService = taskSummaryService;
  }

  // CREATE
  @PostMapping("")
  public ResponseEntity<Task> createTask(@RequestBody Task task) {
    Task newTask = taskService.createTask(task);
    return new ResponseEntity<>(newTask, HttpStatus.CREATED);
  }

  // READ (GET ALL)
  @GetMapping("")
  public ResponseEntity<List<Task>> getAllTasks() {
    List<Task> allTasks = taskService.findAllTasks();
    return new ResponseEntity<>(allTasks, HttpStatus.OK);
  }

  // READ (AI SUMMARY) - a literal path, so it never collides with /{id}
  @GetMapping(value = "/summary", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getTaskSummary() {
    String summary = taskSummaryService.summariseTasks();
    return new ResponseEntity<>(summary, HttpStatus.OK);
  }

  // READ (GET ONE)
  @GetMapping("/{id}")
  public ResponseEntity<Task> getTask(@PathVariable Long id) {
    Task foundTask = taskService.findTaskById(id);
    return new ResponseEntity<>(foundTask, HttpStatus.OK);
  }

  // UPDATE
  @PutMapping("/{id}")
  public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
    Task updatedTask = taskService.updateTask(id, task);
    return new ResponseEntity<>(updatedTask, HttpStatus.OK);
  }

  // DELETE (200 as the brief asks, rather than 204)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  // UPDATE (MARK AS COMPLETE)
  @PutMapping("/{id}/complete")
  public ResponseEntity<Task> markTaskAsComplete(@PathVariable Long id) {
    Task completedTask = taskService.markTaskAsComplete(id);
    return new ResponseEntity<>(completedTask, HttpStatus.OK);
  }
}
