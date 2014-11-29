package scotch.compiler.syntax.builder;

import scotch.compiler.syntax.SourceRange;

public interface SyntaxBuilder<T> {

    T build();

    SyntaxBuilder<T> withSourceRange(SourceRange sourceRange);
}
