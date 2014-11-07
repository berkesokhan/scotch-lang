package scotch.compiler.syntax;

public interface SourceAware<T extends SourceAware> {

    T withSourceRange(SourceRange sourceRange);
}
