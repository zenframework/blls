package org.zenframework.z8.blls.idea;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.zenframework.z8.blls.idea.psi.BlTypes;

public final class BlParserDefinition implements ParserDefinition {

  private static final IFileElementType FILE = new IFileElementType(
    BlLanguage.INSTANCE
  );
  private static final TokenSet COMMENTS = TokenSet.create(
    BlTypes.COMMENT,
    BlTypes.COMMENT_BLOCK,
    BlTypes.COMMENT_DOC
  );
  private static final TokenSet STRINGS = TokenSet.create(BlTypes.STRING);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new FlexAdapter(new BlFlexLexer(null));
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new BlParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return STRINGS;
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return BlTypes.Factory.createElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new PsiFileBase(viewProvider, BlLanguage.INSTANCE) {
      @Override
      public @NotNull FileType getFileType() {
        return BlFileType.INSTANCE;
      }
    };
  }
}
