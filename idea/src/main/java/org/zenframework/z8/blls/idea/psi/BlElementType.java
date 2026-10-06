package org.zenframework.z8.blls.idea.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.zenframework.z8.blls.idea.BlLanguage;

public final class BlElementType extends IElementType {

  public BlElementType(@NonNls @NotNull String debugName) {
    super(debugName, BlLanguage.INSTANCE);
  }
}
