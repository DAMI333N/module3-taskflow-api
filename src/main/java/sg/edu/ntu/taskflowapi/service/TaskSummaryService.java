package sg.edu.ntu.taskflowapi.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import sg.edu.ntu.taskflowapi.exceptions.SummaryUnavailableException;
import sg.edu.ntu.taskflowapi.model.Task;

// Task 6: sends the task list to the model and returns a short plain-English summary.
// Kept separate from TaskService so the CRUD layer has no dependency on Spring AI.
@Service
public class TaskSummaryService {

  private static final Logger logger = LoggerFactory.getLogger(TaskSummaryService.class);

  public static final String NO_TASKS_SUMMARY =
      "There are no tasks yet, so nothing is pending and nothing has been completed.";

  private static final String SYSTEM_PROMPT = """
      You are an assistant for TaskFlow, a task management app.
      Summarise the task list you are given in plain English, in one short paragraph
      of at most four sentences: first what is done, then what is still pending.
      Mention pending high-priority tasks by name. Do not use bullet points, headings,
      or a preamble, and do not invent tasks that are not in the list.
      """;

  private static final String USER_TEMPLATE = """
      Here is the current task list, one task per line:

      {tasks}

      Summarise what is done and what is pending.
      """;

  private final TaskService taskService;
  private final ChatClient chatClient;

  public TaskSummaryService(TaskService taskService, ChatClient.Builder chatClientBuilder) {
    this.taskService = taskService;
    this.chatClient = chatClientBuilder.build();
  }

  public String summariseTasks() {
    List<Task> tasks = taskService.findAllTasks();
    if (tasks.isEmpty()) {
      return NO_TASKS_SUMMARY;
    }

    String taskList = tasks.stream()
        .map(this::describe)
        .collect(Collectors.joining("\n"));
    logger.info("Requesting an AI summary of {} tasks", tasks.size());

    String summary;
    try {
      // .param() keeps the task text out of the template syntax, per lesson 3.15
      summary = chatClient.prompt()
          .system(SYSTEM_PROMPT)
          .user(u -> u.text(USER_TEMPLATE).param("tasks", taskList))
          .call()
          .content();
    } catch (RuntimeException e) {
      throw new SummaryUnavailableException("Could not generate a summary: " + e.getMessage(), e);
    }

    if (summary == null || summary.isBlank()) {
      throw new SummaryUnavailableException("The model returned an empty summary");
    }
    return summary.trim();
  }

  private String describe(Task task) {
    String line = String.format("- [%s] %s (priority: %s, status: %s)",
        task.isCompleted() ? "done" : "pending", task.getTitle(), task.getPriority(), task.getStatus());
    if (task.getDescription() != null && !task.getDescription().isBlank()) {
      line += ": " + task.getDescription();
    }
    return line;
  }
}
