package scotch.compiler.generator;

public class GeneratedClass {

    private final String className;
    private final byte[] bytes;

    public GeneratedClass(String className, byte[] bytes) {
        this.className = className;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getClassName() {
        return className;
    }
}
