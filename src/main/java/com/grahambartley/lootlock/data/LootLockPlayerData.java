package com.grahambartley.lootlock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LootLockPlayerData {
  public static final int CURRENT_SCHEMA_VERSION = 1;

  private int schemaVersion;
  private UUID playerUuid;
  private UUID activeProfileId;
  private List<LootLockProfile> profiles;
  private boolean clientCanEdit;
  private long revision;

  public LootLockPlayerData() {
    this.schemaVersion = CURRENT_SCHEMA_VERSION;
    this.playerUuid = new UUID(0L, 0L);
    LootLockProfile defaultProfile = LootLockProfile.createDefault();
    this.activeProfileId = defaultProfile.getId();
    this.profiles = new ArrayList<>(List.of(defaultProfile));
    this.clientCanEdit = true;
    this.revision = 0L;
  }

  public static LootLockPlayerData createDefault(UUID playerUuid) {
    LootLockPlayerData data = new LootLockPlayerData();
    data.setPlayerUuid(playerUuid);
    return data;
  }

  public void compileProfiles() {
    for (LootLockProfile profile : profiles) {
      if (profile != null) {
        profile.compileRules();
      }
    }
  }

  public Optional<LootLockProfile> getActiveProfile() {
    if (activeProfileId == null) {
      return Optional.empty();
    }

    for (LootLockProfile profile : profiles) {
      if (profile != null && activeProfileId.equals(profile.getId())) {
        return Optional.of(profile);
      }
    }

    return Optional.empty();
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  public void setPlayerUuid(UUID playerUuid) {
    this.playerUuid = playerUuid == null ? new UUID(0L, 0L) : playerUuid;
  }

  public UUID getActiveProfileId() {
    return activeProfileId;
  }

  public void setActiveProfileId(UUID activeProfileId) {
    this.activeProfileId = activeProfileId;
  }

  public List<LootLockProfile> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<LootLockProfile> profiles) {
    if (profiles == null || profiles.isEmpty()) {
      LootLockProfile defaultProfile = LootLockProfile.createDefault();
      this.profiles = new ArrayList<>(List.of(defaultProfile));
      this.activeProfileId = defaultProfile.getId();
      return;
    }

    this.profiles = new ArrayList<>(profiles);
    compileProfiles();
  }

  public boolean isClientCanEdit() {
    return clientCanEdit;
  }

  public void setClientCanEdit(boolean clientCanEdit) {
    this.clientCanEdit = clientCanEdit;
  }

  public long getRevision() {
    return revision;
  }

  public void setRevision(long revision) {
    if (revision < 0) {
      throw new IllegalArgumentException("revision must be >= 0");
    }
    // Monotonic revision is a server-side invariant for authoritative mutable
    // player state. Client snapshots replace whole objects per sync.
    if (revision < this.revision) {
      throw new IllegalArgumentException(
          "revision cannot decrease: current=" + this.revision + ", requested=" + revision);
    }
    this.revision = revision;
  }

  public void incrementRevision() {
    revision++;
  }

  public void setEnabledForAll(boolean enabled) {
    for (LootLockProfile profile : profiles) {
      if (profile != null) {
        profile.setEnabled(enabled);
      }
    }
  }

  public boolean isGloballyEnabled() {
    // True when every profile is enabled. Empty profile list reports true, since there is nothing
    // disabled. The master toggle writes to every profile, so once it is used they all match.
    for (LootLockProfile profile : profiles) {
      if (profile != null && !profile.isEnabled()) {
        return false;
      }
    }
    return true;
  }
}
