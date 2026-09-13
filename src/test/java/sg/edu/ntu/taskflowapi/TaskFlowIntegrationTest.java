package sg.edu.ntu.taskflowapi;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import sg.edu.ntu.taskflowapi.service.TaskSummaryService;

// Full stack through the real controller, service and in-memory repository.
// Only the AI summary service is mocked so no network call is made; the dummy
// api-key property keeps the OpenAI auto-configuration happy without a real key.
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureMockMvc
public class TaskFlowIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskSummaryService taskSummaryService;

  @Test
  void seedTasksArePreloadedInFrontendOrder() throws Exception {
    mockMvc.perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(6)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].title").value("Set up project repository"))
        .andExpect(jsonPath("$[0].status").value("done"))
        .andExpect(jsonPath("$[0].completed").value(true))
        .andExpect(jsonPath("$[2].status").value("in-progress"))
        .andExpect(jsonPath("$[2].completed").value(false))
        .andExpect(jsonPath("$[5].title").value("Deploy to Vercel"));
  }

  @Test
  void fullTaskLifecycle() throws Exception {
    // CREATE with the exact body the task-manager frontend would send
    String createBody = """
        {
          "title": "Wire frontend to API",
          "description": "Replace localStorage with fetch calls.",
          "status": "in-progress",
          "priority": "high"
        }
        """;
    String created = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(createBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("in-progress"))
        .andExpect(jsonPath("$.priority").value("high"))
        .andExpect(jsonPath("$.completed").value(false))
        .andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(created, "$.id");

    // READ
    mockMvc.perform(get("/api/tasks/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Wire frontend to API"));

    // UPDATE: the frontend spreads the whole task back, including a stale completed flag
    String doneBody = """
        {
          "id": %d,
          "title": "Wire frontend to API",
          "description": "Done via fetch.",
          "status": "done",
          "priority": "high",
          "completed": false
        }
        """.formatted(id);
    mockMvc.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON).content(doneBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Done via fetch."))
        .andExpect(jsonPath("$.status").value("done"))
        .andExpect(jsonPath("$.completed").value(true));

    // UPDATE back to in-progress with a stale completed:true
    String reopenBody = """
        {
          "id": %d,
          "title": "Wire frontend to API",
          "description": "Reopened.",
          "status": "in-progress",
          "priority": "medium",
          "completed": true
        }
        """.formatted(id);
    mockMvc.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON).content(reopenBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("in-progress"))
        .andExpect(jsonPath("$.priority").value("medium"))
        .andExpect(jsonPath("$.completed").value(false));

    // COMPLETE
    mockMvc.perform(put("/api/tasks/" + id + "/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("done"))
        .andExpect(jsonPath("$.completed").value(true));

    // DELETE returns 200 with no body, then the task is gone
    mockMvc.perform(delete("/api/tasks/" + id))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
    mockMvc.perform(get("/api/tasks/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Could not find task with id: " + id));
  }

  @Test
  void createWithOnlyATitleAppliesDefaults() throws Exception {
    // Regression: a body without "completed" must not be rejected by the JSON binding
    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Bare minimum\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("todo"))
        .andExpect(jsonPath("$.priority").value("medium"))
        .andExpect(jsonPath("$.completed").value(false))
        .andExpect(jsonPath("$.description").value(nullValue()));
  }

  @Test
  void createWithOnlyTheCompletedFlagBecomesDone() throws Exception {
    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\": \"Grader task\", \"completed\": true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("done"))
        .andExpect(jsonPath("$.completed").value(true));
  }

  @Test
  void createWithUnknownStatusIsRejected() throws Exception {
    mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\": \"Task\", \"status\": \"finished\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("status must be one of: todo, in-progress, done"));
  }

  @Test
  void corsPreflightAllowsTheViteDevOrigin() throws Exception {
    mockMvc.perform(options("/api/tasks")
        .header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void corsPreflightRejectsUnknownOrigins() throws Exception {
    mockMvc.perform(options("/api/tasks")
        .header("Origin", "http://evil.example")
        .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void summaryEndpointReturnsWhatTheSummaryServiceProduces() throws Exception {
    when(taskSummaryService.summariseTasks()).thenReturn("Two tasks are done and four are pending.");

    mockMvc.perform(get("/api/tasks/summary"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Two tasks are done and four are pending."));
  }
}
