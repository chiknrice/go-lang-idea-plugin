// This is a generated file. Not intended for manual editing.
package com.goide.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface GoArrayOrSliceType extends GoType {

  @Nullable
  GoExpression getExpression();

  @Nullable
  GoType getType();

  @NotNull
  PsiElement getLbrack();

  @Nullable
  PsiElement getRbrack();

  @Nullable
  PsiElement getTripleDot();

}
