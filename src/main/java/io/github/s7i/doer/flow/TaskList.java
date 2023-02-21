package io.github.s7i.doer.flow;

import static java.util.Objects.requireNonNull;

import java.util.Collection;

public class TaskList implements Job {

    protected final Collection<Task> tasks;

    public TaskList(Collection<Task> tasks) {
        requireNonNull(tasks, "task list");
        this.tasks = tasks;
    }

    @Override
    public int getTaskCount() {
        return tasks.size();
    }

    @Override
    public Iterable<Task> getTasks() {
        return tasks;
    }
}
