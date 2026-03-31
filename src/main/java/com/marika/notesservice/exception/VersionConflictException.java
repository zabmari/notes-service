package com.marika.notesservice.exception;

public class VersionConflictException extends RuntimeException {

  private final int currentVersion;

  public VersionConflictException(int currentVersion) {
    super("Version conflict");
    this.currentVersion = currentVersion;
  }

  public int getCurrentVersion() {
    return currentVersion;
  }
}