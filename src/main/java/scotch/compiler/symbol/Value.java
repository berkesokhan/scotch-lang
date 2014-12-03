package scotch.compiler.symbol;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static scotch.compiler.symbol.Value.Fixity.NONE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Value {

    String memberName();

    Fixity fixity() default NONE;

    int precedence() default -1;

    enum Fixity {
        LEFT_INFIX,
        RIGHT_INFIX,
        PREFIX,
        NONE,
    }
}
