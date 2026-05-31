package com.grahambartley.client.config;

public final class ClientSettings {
  private boolean showBlockedHudNotification = false;
  private boolean showActionbarFallback = true;
  private boolean confirmBeforeEnablingDelete = true;
  private boolean enableProfileCycleToast = false;
  private int uiScalePercent = 100;

  public static ClientSettings defaults() {
    return new ClientSettings();
  }

  public ClientSettings copy() {
    ClientSettings copy = new ClientSettings();
    copy.showBlockedHudNotification = showBlockedHudNotification;
    copy.showActionbarFallback = showActionbarFallback;
    copy.confirmBeforeEnablingDelete = confirmBeforeEnablingDelete;
    copy.enableProfileCycleToast = enableProfileCycleToast;
    copy.uiScalePercent = uiScalePercent;
    return copy;
  }

  public boolean isShowBlockedHudNotification() {
    return showBlockedHudNotification;
  }

  public void setShowBlockedHudNotification(boolean showBlockedHudNotification) {
    this.showBlockedHudNotification = showBlockedHudNotification;
  }

  public boolean isShowActionbarFallback() {
    return showActionbarFallback;
  }

  public void setShowActionbarFallback(boolean showActionbarFallback) {
    this.showActionbarFallback = showActionbarFallback;
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

  public int getUiScalePercent() {
    return uiScalePercent;
  }

  public void setUiScalePercent(int uiScalePercent) {
    this.uiScalePercent = Math.max(80, Math.min(140, uiScalePercent));
  }
}
