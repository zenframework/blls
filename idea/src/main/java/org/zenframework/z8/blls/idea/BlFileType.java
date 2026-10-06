package org.zenframework.z8.blls.idea;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class BlFileType extends LanguageFileType {

  public static final LanguageFileType INSTANCE = new BlFileType();

  private BlFileType() {
    super(BlLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "BL";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "BL";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "bl";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return Icons.BL;
  }
}
