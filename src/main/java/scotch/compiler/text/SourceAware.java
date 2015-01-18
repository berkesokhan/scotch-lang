package scotch.compiler.text;

public interface SourceAware<T extends SourceAware> {

    T withSourceRange(SourceRange sourceRange);
}
