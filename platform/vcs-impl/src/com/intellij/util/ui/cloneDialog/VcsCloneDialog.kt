// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.ui.cloneDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.attachChild
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.cloneDialog.RepositoryUrlCloneDialogExtension.RepositoryUrlMainExtensionComponent
import java.awt.CardLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.ListSelectionListener

/**
 * Top-level UI-component for new clone/checkout dialog
 */
internal class VcsCloneDialog private constructor(private val project: Project,
                                                  initialExtensionClass: Class<out VcsCloneDialogExtension>,
                                                  private var initialVcs: Class<out CheckoutProvider>? = null) : DialogWrapper(project) {
  private lateinit var extensionList: VcsCloneDialogExtensionList
  private val cardLayout = CardLayout()
  private val mainPanel = JPanel(cardLayout)
  private val extensionComponents: MutableMap<String, VcsCloneDialogExtensionComponent> = HashMap()
  private val listModel = CollectionListModel<VcsCloneDialogExtension>(VcsCloneDialogExtension.EP_NAME.extensionList)

  private val listener = object : VcsCloneDialogComponentStateListener {
    override fun onOkActionNameChanged(name: String) = setOKButtonText(name)

    override fun onOkActionEnabled(enabled: Boolean) {
      isOKActionEnabled = enabled
    }

    override fun onListItemChanged() {
      listModel.allContentsChanged()
      pack()
    }
  }

  init {
    init()
    title = "Get From Version Control"
    JBUI.size(FlatWelcomeFrame.MAX_DEFAULT_WIDTH, FlatWelcomeFrame.DEFAULT_HEIGHT).let {
      rootPane.minimumSize = it
      rootPane.preferredSize = it
    }

    VcsCloneDialogExtension.EP_NAME.findExtension(initialExtensionClass)?.let {
      ScrollingUtil.selectItem(extensionList, it)
    }
  }

  override fun getStyle() = DialogStyle.COMPACT

  override fun createCenterPanel(): JComponent {
    extensionList = VcsCloneDialogExtensionList(listModel).apply {
      addListSelectionListener(ListSelectionListener { e ->
        val source = e.source as VcsCloneDialogExtensionList
        switchComponent(source.selectedValue)
      })
    }
    val scrollableList = ScrollPaneFactory.createScrollPane(extensionList, true).apply {

      border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    }
    return JBUI.Panels.simplePanel()
      .addToCenter(mainPanel)
      .addToLeft(scrollableList)
  }

  override fun doValidateAll(): List<ValidationInfo> {
    return getSelectedComponent()?.doValidateAll() ?: emptyList()
  }

  fun doClone() {
    getSelectedComponent()?.doClone()
  }

  private fun switchComponent(extension: VcsCloneDialogExtension) {
    val extensionId = extension.javaClass.name
    val mainComponent = extensionComponents.getOrPut(extensionId, {
      val component = extension.createMainComponent(project)
      val scrollableMainPanel = ScrollPaneFactory.createScrollPane(component.getView(), true)
      scrollableMainPanel.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
      scrollableMainPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      mainPanel.add(scrollableMainPanel, extensionId)
      disposable.attachChild(component)
      component.addComponentStateListener(listener)
      component
    })

    if (mainComponent is RepositoryUrlMainExtensionComponent) {
      initialVcs?.let { mainComponent.openForVcs(it) }
    }
    mainComponent.onComponentSelected()
    cardLayout.show(mainPanel, extensionId)
  }

  private fun getSelectedComponent(): VcsCloneDialogExtensionComponent? {
    return extensionComponents[extensionList.selectedValue.javaClass.name]
  }

  class Builder(private val project: Project) {
    fun forExtension(clazz: Class<out VcsCloneDialogExtension> = RepositoryUrlCloneDialogExtension::class.java): VcsCloneDialog {
      return VcsCloneDialog(project, clazz, null)
    }

    fun forVcs(clazz: Class<out CheckoutProvider>): VcsCloneDialog {
      return VcsCloneDialog(project, RepositoryUrlCloneDialogExtension::class.java, clazz)
    }
  }
}