package scotch.lang;

import java.lang.Class;
import java.util.List;
import scotch.compiler.ast.Type;

public interface InstanceProvider {

    <T> T getInstance(List<Type> arguments, Class<T> instanceType);
}
