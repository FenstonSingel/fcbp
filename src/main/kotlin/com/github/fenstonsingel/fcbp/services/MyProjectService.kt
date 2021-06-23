package com.github.fenstonsingel.fcbp.services

import com.github.fenstonsingel.fcbp.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
