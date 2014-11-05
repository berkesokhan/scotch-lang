package scotch.compiler.syntax;

import static scotch.compiler.util.TextUtil.normalizeQualified;
import static scotch.compiler.util.TextUtil.splitQualified;

import java.util.Objects;
import scotch.compiler.util.TextUtil;

public abstract class Symbol {

    public static Symbol fromString(String name) {
        return splitQualified(name).into(
            (optionalModuleName, memberName) -> optionalModuleName
                .map(moduleName -> qualified(moduleName, memberName))
                .orElseGet(() -> unqualified(memberName))
        );
    }

    public static Symbol qualified(String moduleName, String memberName) {
        return new QualifiedSymbol(moduleName, memberName);
    }

    public static Symbol unqualified(String memberName) {
        return new UnqualifiedSymbol(memberName);
    }

    private Symbol() {
        // intentionally empty
    }

    public abstract <T> T accept(SymbolVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract String getMemberName();

    @Override
    public abstract int hashCode();

    public abstract Symbol qualifyWith(String moduleName);

    public String quote() {
        return TextUtil.quote(toString());
    }

    @Override
    public abstract String toString();

    public interface SymbolVisitor<T> {

        T visit(QualifiedSymbol symbol);

        T visit(UnqualifiedSymbol symbol);
    }

    public static class QualifiedSymbol extends Symbol {

        private final String moduleName;
        private final String memberName;

        public QualifiedSymbol(String moduleName, String memberName) {
            this.moduleName = moduleName;
            this.memberName = memberName;
        }

        @Override
        public <T> T accept(SymbolVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof QualifiedSymbol) {
                QualifiedSymbol other = (QualifiedSymbol) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(memberName, other.memberName);
            } else {
                return false;
            }
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        public String getModuleName() {
            return moduleName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, memberName);
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberName);
        }

        @Override
        public String toString() {
            return normalizeQualified(moduleName, memberName);
        }
    }

    public static class UnqualifiedSymbol extends Symbol {

        private final String memberName;

        public UnqualifiedSymbol(String memberName) {
            this.memberName = memberName;
        }

        @Override
        public <T> T accept(SymbolVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof UnqualifiedSymbol && Objects.equals(memberName, ((UnqualifiedSymbol) o).memberName);
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberName);
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberName);
        }

        @Override
        public String toString() {
            return memberName;
        }
    }
}
