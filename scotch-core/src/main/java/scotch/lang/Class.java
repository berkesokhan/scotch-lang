package scotch.lang;

import java.util.List;

public interface Class {

    List<Type> getArguments();

    List<ClassMember> getMembers();
}
