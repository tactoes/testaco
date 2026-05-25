package org.testaco.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class PgFixturesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("testacoAddColumn", AddColumnTask::class.java)
        project.tasks.register("testacoRemoveColumn", RemoveColumnTask::class.java)
        project.tasks.register("testacoAddTable", AddTableTask::class.java)
        project.tasks.register("testacoRemoveTable", RemoveTableTask::class.java)
    }
}
