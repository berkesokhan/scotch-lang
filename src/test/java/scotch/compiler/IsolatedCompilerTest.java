package scotch.compiler;

import scotch.compiler.syntax.StubResolver;

public abstract class IsolatedCompilerTest extends CompilerTest<StubResolver> {

    @Override
    protected StubResolver initResolver() {
        return new StubResolver();
    }
}
