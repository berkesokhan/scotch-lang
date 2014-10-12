package scotch.lang;

import java.lang.Class;
import java.util.List;

public interface InstanceProvider {

    <T> T getInstance(List<Type> arguments, Class<T> instanceType);
}
