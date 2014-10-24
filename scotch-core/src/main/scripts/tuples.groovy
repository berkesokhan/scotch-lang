import static java.lang.Integer.parseInt

def groupId = project.properties['groupId']
def artifactId = project.properties['artifactId']
def javaDir = project.properties['javaDir']
def tupleRange = (1..(parseInt(project.properties['tupleRange']) ?: 64) - 1)
def sourcePath = new File("${javaDir}/scotch/data/tuple")
def generated = "@Generated(value = \"${groupId}:${artifactId}:/src/main/scripts/tuples.groovy\", date = \"${new Date()}\")"

def tupleFiles = tupleRange.collect {
    new File(sourcePath, "Tuple${it + 1}.java")
}

sourcePath.deleteDir()
sourcePath.mkdirs()

def util = [
    'package scotch.data.tuple;',
    '',
    'import java.util.function.Function;',
    'import javax.annotation.Generated;',
    '',
    generated,
    '@SuppressWarnings({ "unused", "unchecked" })',
    'public final class TupleValues {',
]

tupleRange.each {
    def size = it + 1
    def range = (0..it)
    def generics = ['<', range.collect { t -> "        T${t}" }.join(',\n'), '    >'].join('\n')

    util << ''
    util << "    public static ${generics} Tuple${size}${generics} tuple${size}("
    util << range.collect { t -> "        T${t} _${t}" }.join(',\n')
    util << '    ) {'
    util << "        return new Tuple${size}<>("
    util << range.collect { t -> "            _${t}" }.join(',\n')
    util << "        );"
    util << '    }'
}

util << ''
util << '    private TupleValues() {'
util << '        // intentionally empty'
util << '    }'
util << '}'
util << ''

def utilFile = new File(sourcePath, "TupleValues.java")
utilFile.delete()
utilFile.write(util.join('\n'))

tupleRange.each {
    def size = it + 1;
    def range = (0..it)
    def content = [
        'package scotch.data.tuple;',
        '',
        'import java.util.Objects;',
        'import javax.annotation.Generated;',
        'import org.apache.commons.lang.builder.EqualsBuilder;',
        '',
    ]

    content << generated
    content << "public final class Tuple${size}<"
    content << range.collect { t -> "    T${t}" }.join(',\n')
    content << "> {"

    content << ''
    range.each { t ->
        content << "    private final T${t} _${t};"
    }

    content << ''
    content << "    public Tuple${size}("
    content << range.collect { t -> "        T${t} _${t}" }.join(',\n')
    content << '    ) {'
    range.each { t ->
        content << "        this._${t} = _${t};"
    }
    content << "    }"

    content << ''
    content << '    @Override'
    content << '    public boolean equals(Object o) {'
    content << '        if (o == this) {'
    content << '            return true;'
    content << "        } else if (o instanceof Tuple${size}) {"
    content << "            Tuple${size} other = (Tuple${size}) o;"
    content << '            return new EqualsBuilder()'
    range.each { t ->
        content << "                .append(_${t}, other._${t})"
    }
    content << '                .isEquals();'
    content << '        } else {'
    content << '            return false;'
    content << '        }'
    content << '    }'

    range.each { t ->
        content << ''
        content << "    public T${t} get_${t}() {"
        content << "        return _${t};"
        content << "    }"
    }

    content << ''
    content << '    @Override'
    content << '    public int hashCode() {'
    content << '        return Objects.hash('
    content << range.collect { t -> "            _${t}" }.join(',\n')
    content << '        );'
    content << "    }"
    content << ''

    if (it == 0) {
        content << '    @Override'
        content << '    public String toString() {'
        content << '        return "(" + _0 + ",)";'
        content << '    }'
    } else {
        content << '    @Override'
        content << '    public String toString() {'
        content << '        return "(" + '
        content << range.collect { t -> "            _${t} +" }.join(' ", " +\n')
        content << '        ")";'
        content << '    }'
    }

    content << ''
    content << "    public <T${size}> T${size} into("
    content << "        Deconstruct${size}<"
    content << range.collect { t -> "            T${t}" }.join(',\n') + ','
    content << "            T${size}"
    content << '        > deconstructor'
    content << '    ) {'
    content << '        return deconstructor.apply('
    content << range.collect { t -> "            _${t}" }.join(',\n')
    content << '        );'
    content << '    }'

    content << ''
    content << '    @FunctionalInterface'
    content << "    public interface Deconstruct${size}<"
    content << range.collect { t -> "        T${t}" }.join(',\n') + ','
    content << "        T${size}"
    content << "    > {"
    content << "        T${size} apply("
    content << range.collect { t -> "            T${t} _${t}" }.join(',\n')
    content << "        );"
    content << '    }'

    content << '}'
    content << ''

    tupleFiles.get(it - 1).delete()
    tupleFiles.get(it - 1).write(content.join('\n'))
}
