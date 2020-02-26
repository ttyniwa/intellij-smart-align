package com.niwatty.intellij.plugin.align.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.niwatty.intellij.plugin.align.settings.gui.SmartAlignConfigForm
import javax.swing.JComponent

class SmartAlignConfigurable(project: Project) : SearchableConfigurable {

    private var smartAlignConfigForm: SmartAlignConfigForm? = null
    private val pluginConfig: PluginConfig = PluginConfig.getInstance(project)

    override fun getId(): String = "com.niwatty.plugin.smart-align"

    override fun getDisplayName(): String = "Smart Align Plugin"

    override fun createComponent(): JComponent {
        smartAlignConfigForm = SmartAlignConfigForm(pluginConfig)
        return smartAlignConfigForm!!.rootPanel
    }

    override fun isModified(): Boolean = smartAlignConfigForm?.isModified ?: false

    override fun apply() {
        smartAlignConfigForm?.apply()
    }

    override fun reset() {
        smartAlignConfigForm?.reset()
    }

    override fun disposeUIResources() {
        smartAlignConfigForm = null
    }
}
