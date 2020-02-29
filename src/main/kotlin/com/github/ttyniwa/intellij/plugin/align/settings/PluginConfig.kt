package com.github.ttyniwa.intellij.plugin.align.settings

import com.bennyhuo.kotlin.deepcopy.annotations.DeepCopy
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
        name = "PluginSettingState",
        storages = [Storage("SmartAlignPluginState.xml")]
)
class PluginConfig : PersistentStateComponent<Config> {

    var config: Config = Config()

    override fun getState(): Config {
        return config
    }

    override fun loadState(config: Config) {
        XmlSerializerUtil.copyBean(config, this.config)
    }

    companion object {
        fun getInstance(project: Project): PluginConfig {
            return ServiceManager.getService(project, PluginConfig::class.java)
        }
    }
}

@DeepCopy
data class Config(var numSpaceForPadding: Int = 1)
