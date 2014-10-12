package scotch.lang;

public interface InstanceProviderFactory {

    InstanceProvider getProvider(Class clazz);
}
