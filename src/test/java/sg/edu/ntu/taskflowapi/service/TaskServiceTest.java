package sg.edu.ntu.taskflowapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sg.edu.ntu.taskflowapi.TaskFixtures.task;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.ntu.taskflowapi.exceptions.InvalidTaskException;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;
import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private TaskService taskService;

  private Task existingTask;

  @BeforeEach
  void setUp() {
    existingTask = task(3L, "Build login page", "Create a login form.", "in-progress", "high");
  }

  // The mocked repository hands back whatever it was asked to save
  private void repositorySavesWhatItGets() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void findAllTasksReturnsRepositoryList() {
    when(taskRepository.findAll()).thenReturn(List.of(existingTask));

    List<Task> tasks = taskService.findAllTasks();

    assertEquals(1, tasks.size());
    assertEquals("Build login page", tasks.get(0).getTitle());
  }

  @Test
  void findTaskByIdReturnsTask() {
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));

    assertEquals(existingTask, taskService.findTaskById(3L));
  }

  @Test
  void findTaskByIdThrowsWhenMissing() {
    when(taskRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.findTaskById(99L));
  }

  @Test
  void createTaskAppliesDefaultsAndIgnoresClientId() {
    repositorySavesWhatItGets();
    Task request = new Task();
    request.setId(999L);
    request.setTitle("  Bare minimum  ");

    Task created = taskService.createTask(request);

    ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
    verify(taskRepository, times(1)).save(saved.capture());
    assertNull(saved.getValue().getId(), "the repository generates ids, not the client");
    assertEquals("Bare minimum", created.getTitle());
    assertEquals("todo", created.getStatus());
    assertEquals("medium", created.getPriority());
    assertFalse(created.isCompleted());
  }

  @Test
  void createTaskDerivesDoneFromCompletedWhenStatusIsMissing() {
    repositorySavesWhatItGets();
    Task request = new Task();
    request.setTitle("Grader task");
    request.setCompleted(true);

    Task created = taskService.createTask(request);

    assertEquals("done", created.getStatus());
    assertTrue(created.isCompleted());
  }

  @Test
  void createTaskKeepsFrontendFieldsAndSyncsCompleted() {
    repositorySavesWhatItGets();
    Task request = task(null, "Deploy", "Ship it.", "done", "low");
    request.setCompleted(false); // stale flag from a client that only knows status

    Task created = taskService.createTask(request);

    assertEquals("done", created.getStatus());
    assertEquals("low", created.getPriority());
    assertTrue(created.isCompleted(), "status is the source of truth");
  }

  @Test
  void createTaskRejectsBlankTitle() {
    Task request = new Task();
    request.setTitle("   ");

    assertThrows(InvalidTaskException.class, () -> taskService.createTask(request));
    verify(taskRepository, never()).save(any(Task.class));
  }

  @Test
  void createTaskRejectsUnknownStatus() {
    Task request = task(null, "Task", null, "finished", "low");

    assertThrows(InvalidTaskException.class, () -> taskService.createTask(request));
  }

  @Test
  void createTaskRejectsUnknownPriority() {
    Task request = task(null, "Task", null, "todo", "urgent");

    assertThrows(InvalidTaskException.class, () -> taskService.createTask(request));
  }

  @Test
  void updateTaskReplacesFieldsAndFollowsStatus() {
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));
    repositorySavesWhatItGets();
    Task request = task(3L, "Build login page", "Finished the form.", "done", "high");
    request.setCompleted(false); // stale flag sent back by the frontend

    Task updated = taskService.updateTask(3L, request);

    assertEquals(3L, updated.getId());
    assertEquals("Finished the form.", updated.getDescription());
    assertEquals("done", updated.getStatus());
    assertTrue(updated.isCompleted());
  }

  @Test
  void updateTaskClearsCompletedWhenStatusMovesBack() {
    existingTask.setStatus("done");
    existingTask.setCompleted(true);
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));
    repositorySavesWhatItGets();
    Task request = task(3L, "Build login page", "Reopened.", "in-progress", "high");
    request.setCompleted(true);

    Task updated = taskService.updateTask(3L, request);

    assertEquals("in-progress", updated.getStatus());
    assertFalse(updated.isCompleted());
  }

  @Test
  void updateTaskRejectsInvalidBodyWithoutTouchingStoredTask() {
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));
    Task request = task(3L, "Changed title", null, "finished", "high");

    assertThrows(InvalidTaskException.class, () -> taskService.updateTask(3L, request));

    assertEquals("Build login page", existingTask.getTitle());
    assertEquals("in-progress", existingTask.getStatus());
    verify(taskRepository, never()).save(any(Task.class));
  }

  @Test
  void updateTaskThrowsWhenMissing() {
    when(taskRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class,
        () -> taskService.updateTask(99L, task(99L, "Ghost", null, "todo", "low")));
  }

  @Test
  void deleteTaskRemovesExistingTask() {
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));

    taskService.deleteTask(3L);

    verify(taskRepository, times(1)).deleteById(3L);
  }

  @Test
  void deleteTaskThrowsWhenMissing() {
    when(taskRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(99L));
    verify(taskRepository, never()).deleteById(any());
  }

  @Test
  void markTaskAsCompleteSetsStatusAndFlagThenSaves() {
    when(taskRepository.findById(3L)).thenReturn(Optional.of(existingTask));
    repositorySavesWhatItGets();

    Task completed = taskService.markTaskAsComplete(3L);

    assertEquals("done", completed.getStatus());
    assertTrue(completed.isCompleted());
    verify(taskRepository, times(1)).save(existingTask);
  }

  @Test
  void markTaskAsCompleteThrowsWhenMissing() {
    when(taskRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.markTaskAsComplete(99L));
  }
}
