package scotch.compiler.syntax.builder;

import scotch.compiler.text.SourceLocation;

public interface SyntaxBuilder<T> {

    T build();

    SyntaxBuilder<T> withSourceLocation(SourceLocation sourceLocation);
}
