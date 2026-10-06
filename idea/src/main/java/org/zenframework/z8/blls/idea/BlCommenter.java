package org.zenframework.z8.blls.idea;

import com.intellij.codeInsight.generation.IndentedCommenter;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zenframework.z8.blls.idea.psi.BlTypes;

public final class BlCommenter
  implements CodeDocumentationAwareCommenter, IndentedCommenter {

  @Override
  public @NotNull Boolean forceIndentedLineComment() {
    return true;
  }

  @Override
  public @NotNull IElementType getLineCommentTokenType() {
    return BlTypes.COMMENT;
  }

  @Override
  public @NotNull IElementType getBlockCommentTokenType() {
    return BlTypes.COMMENT_BLOCK;
  }

  @Override
  public @NotNull IElementType getDocumentationCommentTokenType() {
    return BlTypes.COMMENT_DOC;
  }

  @Override
  public @NotNull String getDocumentationCommentPrefix() {
    return "/**";
  }

  @Override
  public @Nullable String getDocumentationCommentLinePrefix() {
    return null;
  }

  @Override
  public @NotNull String getDocumentationCommentSuffix() {
    return "*/";
  }

  @Override
  public boolean isDocumentationComment(PsiComment element) {
    return false;
  }

  @Override
  public @NotNull String getLineCommentPrefix() {
    return "//";
  }

  @Override
  public @NotNull String getBlockCommentPrefix() {
    return "/*";
  }

  @Override
  public @NotNull String getBlockCommentSuffix() {
    return "*/";
  }

  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }
}
