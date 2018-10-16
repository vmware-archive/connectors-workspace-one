package com.vmware.connectors.servicenow;

public enum TaskKey {
    change_request("change_request"),
    change_task("change_task"),
    incident("incident"),
    problem("problem"),
    request("sc_request"),
    requested_item("sc_req_item"),
    task("sc_task"),
    group_approval("sysapproval_group"),
    all("task");

    private final String taskType;
    TaskKey(String taskType) {
        this.taskType = taskType;
    }

    @Override
    public String toString() {
        return this.taskType;
    }
}