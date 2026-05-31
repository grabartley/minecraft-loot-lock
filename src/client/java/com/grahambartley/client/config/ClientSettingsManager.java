package com.grahambartley.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grahambartley.LootLock;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientSettingsManager {
  private final Path configPath;
  private final Gson gson;
  private ClientSettings settings;

  public ClientSettingsManager(Path configPath) {
    this(configPath, new GsonBuilder().setPrettyPrinting().create());
  }

  ClientSettingsManager(Path configPath, Gson gson) {
    this.configPath = configPath;
    this.gson = gson;
    this.settings = ClientSettings.defaults();
  }

  public void load() {
    if (!Files.exists(configPath)) {
      settings = ClientSettings.defaults();
      save();
      return;
    }

    try (Reader reader = Files.newBufferedReader(configPath)) {
      ClientSettings loaded = gson.fromJson(reader, ClientSettings.class);
      settings = loaded == null ? ClientSettings.defaults() : loaded;
      settings.setUiScalePercent(settings.getUiScalePercent());
    } catch (IOException | RuntimeException exception) {
      LootLock.LOGGER.warn("Failed to load client settings, using defaults", exception);
      settings = ClientSettings.defaults();
      save();
    }
  }

  public void save() {
    try {
      Files.createDirectories(configPath.getParent());
      try (Writer writer = Files.newBufferedWriter(configPath)) {
        gson.toJson(settings, writer);
      }
    } catch (IOException exception) {
      LootLock.LOGGER.warn("Failed to save client settings", exception);
    }
  }

  public ClientSettings getSettingsCopy() {
    return settings.copy();
  }

  public void replaceAndSave(ClientSettings updatedSettings) {
    if (updatedSettings == null) {
      return;
    }
    updatedSettings.setUiScalePercent(updatedSettings.getUiScalePercent());
    settings = updatedSettings.copy();
    save();
  }
}
