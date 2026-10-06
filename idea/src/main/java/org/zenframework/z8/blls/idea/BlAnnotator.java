package org.zenframework.z8.blls.idea;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.zenframework.z8.blls.idea.psi.BlIdentifierDefinitionReference;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class BlAnnotator implements Annotator {

  private static final TextAttributesKey DEFINITION_REFERENCE = createTextAttributesKey("BL_DEFINITION_REFERENCE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);

  @Override
  public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder) {
    if (psiElement instanceof BlIdentifierDefinitionReference) {
      annotationHolder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(psiElement)
        .textAttributes(DEFINITION_REFERENCE)
        .create();
    }
  }
}
