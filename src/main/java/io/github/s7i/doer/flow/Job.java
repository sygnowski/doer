package io.github.s7i.doer.flow;

public interface Job {

    int getTaskCount();

    Iterable<Task> getTasks();

}
