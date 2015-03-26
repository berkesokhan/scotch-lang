package scotch.compiler.text;

public interface SourceAware<T extends SourceAware> {

    T withSourceLocation(SourceLocation sourceLocation);
}
