package org.zenframework.z8.blls.idea;

import com.intellij.lang.Language;

public final class BlLanguage extends Language {

  public static final Language INSTANCE = new BlLanguage();

  private BlLanguage() {
    super("BL");
  }
}
