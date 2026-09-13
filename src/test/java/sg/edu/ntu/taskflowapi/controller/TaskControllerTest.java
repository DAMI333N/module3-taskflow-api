package sg.edu.ntu.taskflowapi.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static sg.edu.ntu.taskflowapi.TaskFixtures.task;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sg.edu.ntu.taskflowapi.exceptions.InvalidTaskException;
import sg.edu.ntu.taskflowapi.exceptions.SummaryUnavailableException;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;
import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.service.TaskService;
import sg.edu.ntu.taskflowapi.service.TaskSummaryService;

// Web layer only: the two services are mocked, so these tests pin down the
// routes, status codes, JSON shape and error bodies of the controller.
@WebMvcTest(TaskController.class)
public class TaskControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskService taskService;

  @MockitoBean
  private TaskSummaryService taskSummaryService;

  private final Task loginTask = task(3L, "Build login page", "Create a login form.", "in-progress", "high");
  private final Task readmeTask = task(5L, "Update README", "Add setup instructions.", "todo", "low");

  @Test
  void getAllTasksReturnsBareJsonArray() throws Exception {
    when(taskService.findAllTasks()).thenReturn(List.of(loginTask, readmeTask));

    mockMvc.perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(3))
        .andExpect(jsonPath("$[0].status").value("in-progress"))
        .andExpect(jsonPath("$[0].priority").value("high"))
        .andExpect(jsonPath("$[0].completed").value(false))
        .andExpect(jsonPath("$[1].title").value("Update README"));
  }

  @Test
  void getTaskReturnsTask() throws Exception {
    when(taskService.findTaskById(3L)).thenReturn(loginTask);

    mockMvc.perform(get("/api/tasks/3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.title").value("Build login page"))
        .andExpect(jsonPath("$.description").value("Create a login form."));
  }

  @Test
  void getUnknownTaskReturns404WithErrorBody() throws Exception {
    when(taskService.findTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

    mockMvc.perform(get("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Could not find task with id: 99"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void getTaskWithNonNumericIdReturns400() throws Exception {
    mockMvc.perform(get("/api/tasks/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("must be a number")));
  }

  @Test
  void createTaskReturns201WithCreatedTask() throws Exception {
    Task created = task(7L, "Wire frontend to API", "Replace localStorage.", "in-progress", "high");
    when(taskService.createTask(any(Task.class))).thenReturn(created);

    String body = """
        {
          "title": "Wire frontend to API",
          "description": "Replace localStorage.",
          "status": "in-progress",
          "priority": "high"
        }
        """;

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.status").value("in-progress"))
        .andExpect(jsonPath("$.completed").value(false));
    verify(taskService, times(1)).createTask(any(Task.class));
  }

  @Test
  void createTaskWithBlankTitleReturns400() throws Exception {
    when(taskService.createTask(any(Task.class))).thenThrow(new InvalidTaskException("title is required"));

    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("title is required"));
  }

  @Test
  void createTaskWithMalformedJsonReturns400() throws Exception {
    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
  }

  @Test
  void updateTaskReturns200WithUpdatedTask() throws Exception {
    Task updated = task(3L, "Build login page", "Finished.", "done", "high");
    when(taskService.updateTask(eq(3L), any(Task.class))).thenReturn(updated);

    String body = """
        {
          "id": 3,
          "title": "Build login page",
          "description": "Finished.",
          "status": "done",
          "priority": "high",
          "completed": false
        }
        """;

    mockMvc.perform(put("/api/tasks/3").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("done"))
        .andExpect(jsonPath("$.completed").value(true));
  }

  @Test
  void deleteTaskReturns200WithEmptyBody() throws Exception {
    mockMvc.perform(delete("/api/tasks/3"))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
    verify(taskService, times(1)).deleteTask(3L);
  }

  @Test
  void deleteUnknownTaskReturns404() throws Exception {
    doThrow(new TaskNotFoundException(99L)).when(taskService).deleteTask(99L);

    mockMvc.perform(delete("/api/tasks/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  void markTaskAsCompleteReturns200WithDoneTask() throws Exception {
    Task completed = task(3L, "Build login page", "Create a login form.", "done", "high");
    when(taskService.markTaskAsComplete(3L)).thenReturn(completed);

    mockMvc.perform(put("/api/tasks/3/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("done"))
        .andExpect(jsonPath("$.completed").value(true));
  }

  @Test
  void summaryReturnsPlainText() throws Exception {
    when(taskSummaryService.summariseTasks()).thenReturn("Two tasks are done and four are pending.");

    mockMvc.perform(get("/api/tasks/summary"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Two tasks are done and four are pending."));
  }

  @Test
  void summaryReturns502WhenTheModelIsUnavailable() throws Exception {
    when(taskSummaryService.summariseTasks())
        .thenThrow(new SummaryUnavailableException("Could not generate a summary: 401 Unauthorized"));

    mockMvc.perform(get("/api/tasks/summary"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.message").value(containsString("Could not generate a summary")));
  }
}
