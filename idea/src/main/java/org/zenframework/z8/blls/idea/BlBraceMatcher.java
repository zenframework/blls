package org.zenframework.z8.blls.idea;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zenframework.z8.blls.idea.psi.BlTypes;

import java.util.Optional;

public final class BlBraceMatcher implements PairedBraceMatcher {

  private static final BracePair[] PAIRS = {
    new BracePair(BlTypes.BRACE_L, BlTypes.BRACE_R, true),
    new BracePair(BlTypes.BRACKET_L, BlTypes.BRACKET_R, false),
    new BracePair(BlTypes.PAREN_L, BlTypes.PAREN_R, false),
  };

  @Override
  public BracePair @NotNull [] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(
    @NotNull IElementType lbraceType,
    @Nullable IElementType contextType
  ) {
    return true;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return Optional.ofNullable(file.findElementAt(openingBraceOffset))
      .map(PsiElement::getParent)
      .map(PsiElement::getTextOffset)
      .orElse(openingBraceOffset);
  }
}
