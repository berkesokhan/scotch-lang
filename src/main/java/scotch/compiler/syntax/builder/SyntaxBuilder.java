package scotch.compiler.syntax.builder;

import scotch.compiler.text.SourceRange;

public interface SyntaxBuilder<T> {

    T build();

    SyntaxBuilder<T> withSourceRange(SourceRange sourceRange);
}
