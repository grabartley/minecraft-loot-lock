package com.grahambartley.client.config;

public final class ClientSettings {
  private boolean showBlockedHudNotification = false;
  private boolean confirmBeforeEnablingDelete = true;
  private boolean enableProfileCycleToast = false;
  private boolean enableToggleToast = false;
  private int uiScalePercent = 100;

  public static ClientSettings defaults() {
    return new ClientSettings();
  }

  public ClientSettings copy() {
    ClientSettings copy = new ClientSettings();
    copy.showBlockedHudNotification = showBlockedHudNotification;
    copy.confirmBeforeEnablingDelete = confirmBeforeEnablingDelete;
    copy.enableProfileCycleToast = enableProfileCycleToast;
    copy.enableToggleToast = enableToggleToast;
    copy.uiScalePercent = uiScalePercent;
    return copy;
  }

  public boolean isShowBlockedHudNotification() {
    return showBlockedHudNotification;
  }

  public void setShowBlockedHudNotification(boolean showBlockedHudNotification) {
    this.showBlockedHudNotification = showBlockedHudNotification;
  }

  public boolean isConfirmBeforeEnablingDelete() {
    return confirmBeforeEnablingDelete;
  }

  public void setConfirmBeforeEnablingDelete(boolean confirmBeforeEnablingDelete) {
    this.confirmBeforeEnablingDelete = confirmBeforeEnablingDelete;
  }

  public boolean isEnableProfileCycleToast() {
    return enableProfileCycleToast;
  }

  public void setEnableProfileCycleToast(boolean enableProfileCycleToast) {
    this.enableProfileCycleToast = enableProfileCycleToast;
  }

  public boolean isEnableToggleToast() {
    return enableToggleToast;
  }

  public void setEnableToggleToast(boolean enableToggleToast) {
    this.enableToggleToast = enableToggleToast;
  }

  public int getUiScalePercent() {
    return uiScalePercent;
  }

  public void setUiScalePercent(int uiScalePercent) {
    this.uiScalePercent = Math.max(80, Math.min(140, uiScalePercent));
  }
}
