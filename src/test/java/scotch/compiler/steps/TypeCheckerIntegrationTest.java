package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.text.SourceLocation.source;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.ignore;
import static scotch.compiler.util.TestUtil.matcher;
import static scotch.compiler.util.TestUtil.ordinalField;
import static scotch.compiler.util.TestUtil.ordinalStruct;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Unification.mismatch;

import java.util.Optional;
import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.ClassLoaderResolver;
import scotch.compiler.Compiler;
import scotch.compiler.CompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.symbol.type.Type;

public class TypeCheckerIntegrationTest extends CompilerTest<ClassLoaderResolver> {

    @Test
    public void shouldHaveTypeOfTuple3OfInts() {
        compile(
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.tuple",
            "",
            "tuple = (1, 2, 3)"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.tuple", sum("scotch.data.tuple.(,,)", asList(intType, intType, intType)));
    }

    @Test
    public void shouldHaveError_whenStringIsHeterogeneous() {
        compile(
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.list",
            "import scotch.data.string",
            "",
            "list = [1, 2, \"oops\"]"
        );
        shouldHaveErrors(typeError(
            mismatch(intType, stringType),
            source("test://shouldHaveError_whenStringIsHeterogeneous", point(107, 6, 15), point(113, 6, 21))
        ));
    }

    @Test
    public void shouldDetermineTypeOfSuccessfulChainedMaybe() {
        compile(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.maybe",
            "import scotch.data.num",
            "",
            "addedStuff = do",
            "    x <- Just 3",
            "    y <- Just 2",
            "    return $ x + y"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.addedStuff", sum("scotch.data.maybe.Maybe", asList(intType)));
    }

    @Test
    public void shouldDetermineTypeOfFailedChainedMaybe() {
        compile(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.maybe",
            "import scotch.data.num",
            "",
            "addedStuff = do",
            "    x <- Just 3",
            "    y <- Nothing",
            "    return $ x + y"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.addedStuff", sum("scotch.data.maybe.Maybe", asList(intType)));
    }

    @Ignore("WIP")
    @Test
    public void shouldDestructureTuple() {
        compile(
            "module scotch.test",
            "import scotch.data.int",
            "",
            "second :: (Int, Int) -> Int",
            "second (_, b) = b"
        );
        shouldNotHaveErrors();
        Type tuple = sum("scotch.data.tuple.(,)", asList(intType, intType));
        shouldHaveValue("scotch.test.second", matcher(
            "scotch.test.(second#0)", fn(tuple, intType), arg("#0", tuple),
            pattern("scotch.test.(second#0#0)",
                asList(ordinalStruct("#0", "scotch.data.tuple.(,)", intType, asList(
                    ordinalField("#0", "_0", intType, ignore(intType)),
                    ordinalField("#0", "_1", intType, capture("#0#_1", "b", intType))))),
                arg("b", intType)
            )
        ));
    }

    @Override
    protected Function<Compiler, DefinitionGraph> compile() {
        return Compiler::checkTypes;
    }

    @Override
    protected ClassLoaderResolver initResolver() {
        return new ClassLoaderResolver(Optional.empty(), getClass().getClassLoader());
    }
}
