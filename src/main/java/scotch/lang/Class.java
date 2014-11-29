package scotch.lang;

import java.util.List;
import scotch.compiler.syntax.Type;

public interface Class {

    List<Type> getArguments();

    List<ClassMember> getMembers();
}
