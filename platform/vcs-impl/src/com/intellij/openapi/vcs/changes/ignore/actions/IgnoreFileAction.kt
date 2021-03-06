// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.changes.ignore.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vcs.changes.IgnoredBeanFactory
import com.intellij.openapi.vcs.changes.actions.ScheduleForAdditionAction
import com.intellij.openapi.vcs.changes.ignore.psi.util.addNewElements
import com.intellij.openapi.vcs.changes.ui.ChangesListView
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.isEmpty
import com.intellij.vcsUtil.VcsUtil
import kotlin.streams.toList

class IgnoreFileAction(private val ignoreFile: VirtualFile) : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val vcs = VcsUtil.getVcsFor(project, ignoreFile) ?: return
    val exactlySelectedFiles = e.getData(ChangesListView.EXACTLY_SELECTED_FILES_DATA_KEY)?.toList()

    val ignored =
      (exactlySelectedFiles ?: e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList())
        .filter { VcsUtil.getVcsFor(project, it) == vcs }
        .map { IgnoredBeanFactory.ignoreFile(it, project) }

    addNewElements(project, ignoreFile, ignored)
    ChangeListManagerImpl.getInstanceImpl(project).scheduleUnversionedUpdate()
    OpenFileDescriptor(project, ignoreFile).navigate(true)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isVisible = !ScheduleForAdditionAction.getUnversionedFiles(e, project).isEmpty()
  }
}