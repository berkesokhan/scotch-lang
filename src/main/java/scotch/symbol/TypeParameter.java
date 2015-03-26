package scotch.symbol;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

@Documented
@Retention(RUNTIME)
public @interface TypeParameter {

    String name();

    String[] constraints() default { };
}
