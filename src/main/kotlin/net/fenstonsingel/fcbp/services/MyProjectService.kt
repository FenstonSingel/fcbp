package net.fenstonsingel.fcbp.services

import net.fenstonsingel.fcbp.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }

}
