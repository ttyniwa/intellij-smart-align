package com.github.ttyniwa.intellij.plugin.align.settings.gui;

import com.github.ttyniwa.intellij.plugin.align.settings.Config;
import com.github.ttyniwa.intellij.plugin.align.settings.Config__DeepCopyKt;
import com.github.ttyniwa.intellij.plugin.align.settings.PluginConfig;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;

public class SmartAlignConfigForm {
    private JPanel rootPanel;
    private JBTextField numSpaceForPaddingTetField;
    private PluginConfig pluginConfig;

    public SmartAlignConfigForm(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        reset();
    }

    public void reset() {
        // ignore pattern
        Config clone = Config__DeepCopyKt.deepCopy(pluginConfig.getConfig());

        // num space for padding
        if (numSpaceForPaddingTetField != null) {
            numSpaceForPaddingTetField.setText(String.valueOf(clone.getNumSpaceForPadding()));
        }
    }

    public void apply() {
        pluginConfig.setConfig(getConfig());
    }

    public boolean isModified() {
        return !pluginConfig.getConfig().equals(getConfig());
    }

    public Config getConfig() {
        Config config = new Config();

        config.setNumSpaceForPadding(Integer.parseInt(numSpaceForPaddingTetField.getText()));

        return config;
    }
}
