package io.github.s7i.doer.flow

import spock.lang.Specification

class JobFlowTest extends Specification {

    class JobImpl implements Job {

        def list = [new TaskImpl(), new TaskImpl()]
        @Override
        int getTaskCount() {
            list.size()
        }

        @Override
        Iterable<Task> getTasks() {
            list
        }
    }

    class TaskImpl implements Task {

        @Override
        void execute() {

        }
    }

    def "test"() {

    }

}
