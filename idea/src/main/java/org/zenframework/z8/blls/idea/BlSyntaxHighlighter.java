package org.zenframework.z8.blls.idea;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.zenframework.z8.blls.idea.psi.BlTypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class BlSyntaxHighlighter implements SyntaxHighlighter {

  private static final TextAttributesKey COMMENT =
    createTextAttributesKey("BL_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT),
    COMMENT_BLOCK = createTextAttributesKey("BL_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    COMMENT_DOC = createTextAttributesKey("BL_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT),
    NUMBER = createTextAttributesKey("BL_NUMBER", DefaultLanguageHighlighterColors.NUMBER),
    STRING = createTextAttributesKey("BL_STRING", DefaultLanguageHighlighterColors.STRING),
    KEYWORD = createTextAttributesKey("BL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

  private static final Map<IElementType, TextAttributesKey> TOKEN_TO_ATTRIBUTES = new HashMap<>(
    Map.of(
      BlTypes.COMMENT,
      COMMENT,
      BlTypes.COMMENT_BLOCK,
      COMMENT_BLOCK,
      BlTypes.COMMENT_DOC,
      COMMENT_DOC,
      BlTypes.INTEGER,
      NUMBER,
      BlTypes.DECIMAL,
      NUMBER,
      BlTypes.STRING,
      STRING,
      BlTypes.SPECIAL,
      STRING
    )
  );

  static {
    Arrays.stream(BlTypes.class.getFields())
      .filter(it -> it.getName().startsWith("KEYWORD_"))
      .forEach(it -> {
        try {
          TOKEN_TO_ATTRIBUTES.put((IElementType) it.get(null), KEYWORD);
        } catch (IllegalAccessException _) {
          throw new AssertionError();
        }
      });
  }

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new FlexAdapter(new BlFlexLexer(null));
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    var result = TOKEN_TO_ATTRIBUTES.get(tokenType);
    return result == null
      ? new TextAttributesKey[0]
      : new TextAttributesKey[]{result};
  }
}
