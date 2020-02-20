package com.vmware.connectors.msPlanner.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.utils.JsonParser

const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

/**
 * TaskInfo Object
 *
 * @property taskObj Task Object
 */
data class TaskInfo(
        /**
         * comments entered by the User Related to Task
         */
        val comments: String?,
        /**
         * Serialized Task Object
         */
        private val task: String
) {
    val taskObj = JsonParser.deserialize<Task>(this.task)
}

/**
 * UserRoles Object
 */
object UserRoles {
    /**
     * General User
     */
    const val USER = "user"
    /**
     * Manager Role
     */
    const val MANAGER = "manager"
}

/**
 * Task object
 * @property userId id of this user.
 * @property groupId id of the group this task belongs to.
 * @property url url which points to the task.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Task(
        /**
         *
         * e-tag property used for updating the task details.
         */
        @JsonProperty(value = "@odata.etag", access = JsonProperty.Access.READ_WRITE)
        val eTag: String,
        /**
         *  ID of the task. It is 28 characters long and case-sensitive.
         */
        @JsonProperty("id")
        val id: String,
        /**
         *
         * Plan ID to which the task belongs.
         */
        @JsonProperty("planId")
        val planId: String,
        /**
         *
         * Bucket ID to which the task belongs.
         * The bucket needs to be in the plan that the task is in.
         * It is 28 characters long and case-sensitive.
         */
        @JsonProperty("bucketId")
        val bucketId: String,
        /**
         * Thread ID of the conversation on the task.
         * This is the ID of the conversation thread object created in the group.
         */
        @JsonProperty("conversationThreadId")
        val conversationThreadId: String?,
//        /**
//         * This sets the type of preview that shows up on the task.
//         * Possible values are: automatic, noPreview, checklist, description, reference.
//         */
////        @JsonProperty("previewType")
//        val previewType: String,
        /**
         * Date and time at which the task is created.
         * The Timestamp type represents date and time information using ISO 8601 format and is always in UTC time.
         * For example, midnight UTC on Jan 1, 2014 would look like this: '2014-01-01T00:00:00Z'
         */
        @JsonProperty("createdDateTime")
        val createdDateTime: String,
        /**
         * Date and time at which the task is started.
         */
        @JsonProperty("startDateTime")
        val startDateTime: String?,
        /**
         * Date and time at which the task is due.
         * The Timestamp type represents date and time information using ISO 8601 format and is always in UTC time.
         * For example, midnight UTC on Jan 1, 2014 would look like this: '2014-01-01T00:00:00Z'
         */
        @JsonProperty("dueDateTime")
        val dueDateTime: String?,
        /**
         * Title of the task.
         */
        @JsonProperty("title")
        val title: String,
        /**
         * Percentage of task completion. When set to 100, the task is considered completed.
         */
        @JsonProperty("percentComplete")
        val percentComplete: Int,
        /**
         * priority of the task
         * 1 -> Urgent,
         * 3 -> Important,
         * 5 -> Medium,
         * 9 -> Low
         */
        @JsonProperty("priority")
        val priority: Int,
        /**
         * Number of external references that exist on the task.
         */
        @JsonProperty("referenceCount")
        val referenceCount: Int,
        /**
         *
         * The set of assignees the task is assigned to.
         */
        @JsonProperty("assignments")
        val assignments: Map<String, PlannerAssignments>,
        /**
         * Identity of the user that created the task.
         */
        @JsonProperty("createdBy")
        val createdBy: IdentitySet,
        /**
         * MetaInfo of the Task.
         */
        @JsonProperty("taskMetaInfo")
        val taskMetaInfo: TaskMetaInfo?,
        val userId: String = "",
        /**
         * 	The categories to which the task has been applied.
         */
        @JsonProperty("appliedCategories")
        val appliedCategories: Map<String, String>,
        /**
         * describes the labels of the plan.
         */
        @JsonProperty("categoryMap")
        val categoryMap: Map<String, String?> = emptyMap()
) {
    val groupId = taskMetaInfo?.groupId ?: ""
    val url = Endpoints.taskUrl(groupId, planId, id)
}

/**
 * PlannerAssignments Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlannerAssignments(
        /**
         * The identity of the user that performed the assignment of the task, i.e. the assignor.
         */
        @JsonProperty("assignedBy")
        val assignedBy: IdentitySet,
        /**
         * The time at which the task was assigned.
         * The Timestamp type represents date and time information using ISO 8601 format and is always in UTC time.
         * For example, midnight UTC on Jan 1, 2014 would look like this: '2014-01-01T00:00:00Z'
         */
        @JsonProperty("assignedDateTime")
        val assignedDateTime: String
)

/**
 * IdentitySet Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentitySet(
        /**
         * Optional. The team or channel associated with this action.
         */
        @JsonProperty("conversation")
        val conversation: Identity?,
        /**
         * Optional. The user associated with this action.
         */
        @JsonProperty("user")
        val user: Identity
)

/**
 * Identity Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Identity(
        /**
         * The identity's display name.
         */
        @JsonProperty("displayName")
        val displayName: String?,
        /**
         * Unique identifier for the identity.
         */
        @JsonProperty("id")
        val id: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskDetails(
        /**
         * Read-only. ID of the task details. It is 28 characters long and case-sensitive.
         */
        @JsonProperty("id")
        val id: String,
        /**
         * Description of the task
         */
        @JsonProperty("description")
        val description: String,
        /**
         * The collection of references on the task.
         */
        @JsonProperty("references")
        val references: Map<String, ExternalReference>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalReference(
        /**
         * A name alias to describe the reference.
         */
        @JsonProperty("alias")
        val alias: String
        /**
         * Read-only. User ID by which this is last modified.
         */
//        @JsonProperty("lastModifiedBy")
//        val lastModifiedBy: IdentitySet,
        /**
         * Used to set the relative priority order in which the reference will be shown as a preview on the task.
         */
//        @JsonProperty("previewPriority")
//        val previewPriority: String,
        /**
         * Used to describe the type of the reference. Types include: PowerPoint, Word, Excel, Other.
         */
//        @JsonProperty("type")
//        val type: String
)

/**
 * TaskMetaInfo Object.
 */
data class TaskMetaInfo(
        /**
         * ID of the Group.
         */
        @JsonProperty("groupId")
        val groupId: String,
        /**
         *
         * Bucket ID to which the task belongs.
         * The bucket needs to be in the plan that the task is in.
         * It is 28 characters long and case-sensitive.
         */
        @JsonProperty("bucketId")
        val bucketId: String,
        /**
         * Name of the plan.
         */
        @JsonProperty("planName")
        val planName: String,
        /**
         * Name of the bucket.
         */
        @JsonProperty("bucketName")
        val bucketName: String
)
