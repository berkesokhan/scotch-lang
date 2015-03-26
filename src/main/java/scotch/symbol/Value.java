package scotch.symbol;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static scotch.symbol.Value.Fixity.NONE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Value {

    String memberName();

    Fixity fixity() default NONE;

    int precedence() default 7;

    public enum Fixity {
        LEFT_INFIX,
        RIGHT_INFIX,
        PREFIX,
        NONE,
    }
}
