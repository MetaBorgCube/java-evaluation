package mb.java.benchmark_generator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.io.Files;

import mb.nabl2.util.Tuple2;

public class Generator {

    private enum Expr {
        CALL, PLUS, MINUS
    }

    private Expr expr = Expr.CALL;

    private int packages = 100;
    private int classes = 1;
    private int fields = 50;
    private int methods = 100;

    private int exprs = 50;
    private int exprsVar = 10;

    private int args = 5;
    private int argsVar = 3;

    private static final String class_tmpl = "package %s;%n%npublic class C%d {%n%s}";
    private static final String field_tmpl = "\tprivate int f%d = %d;%n";
    private static final String method_tmpl = "\tprivate int %s(%s) {%n%s\t\treturn %s;%n\t};%n%n";
    private static final String call_tmpl = "\t\tint v%d = %s(%s);%n";
    private static final String plusminus_tmpl = "\t\tint v%d = %s;%n";

    private final Random random = new Random();

    private final Set<String> fieldNames = new HashSet<>();
    private final List<Tuple2<String, Integer>> methodSignatures = new ArrayList<>();

    public static void main(String[] args) {
        final Generator generator = new Generator();
        File outputDir = null;
        String ext = "java";

        final Iterator<String> it = Iterators.forArray(args);
        OPTS: while(it.hasNext()) {
            final String arg = it.next();
            switch(arg) {
                case "-e":
                case "--ext":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    ext = it.next();
                    break;
                case "-p":
                case "--packages":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.packages = Integer.parseInt(it.next());
                    break;
                case "-c":
                case "--classes":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.classes = Integer.parseInt(it.next());
                    break;
                case "-f":
                case "--fields":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.fields = Integer.parseInt(it.next());
                    break;
                case "-m":
                case "--methods":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.methods = Integer.parseInt(it.next());
                    break;
                case "-i":
                case "--invokes":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.exprs = Integer.parseInt(it.next());
                    break;
                case "-iv":
                case "--invokes-variance":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.exprsVar = Integer.parseInt(it.next());
                    break;
                case "-a":
                case "--args":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.args = Integer.parseInt(it.next());
                    break;
                case "-av":
                case "--args-variance":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.argsVar = Integer.parseInt(it.next());
                    break;
                case "-b":
                case "--body":
                    if(!it.hasNext()) {
                        System.err.println("Missing parameter to " + arg);
                        return;
                    }
                    generator.expr = Expr.valueOf(it.next().toUpperCase());
                    break;
                default:
                    outputDir = new File(arg);
                    break OPTS;
            }
        }
        if(outputDir == null) {
            System.err.println("Missing output directory.");
            return;
        } else if(it.hasNext()) {
            System.err.println(
                    "Found extra parameters: " + Streams.stream(it).collect(Collectors.joining("\" \"", "\"", "\"")));
            return;
        }

        final File targetDir = outputDir;
        final String _ext = ext;

        final Map<String, String> sources = generator.generate();
        final String readme = "# Generator settings\n" //
                + "- packages = " + generator.packages + "\n" //
                + "- classes = " + generator.classes + "\n" //
                + "- fields = " + generator.fields + "\n" //
                + "- methods = " + generator.methods + "\n" //
                + "- calls = " + generator.exprs + " ± " + generator.exprsVar + "\n" //
                + "- args = " + args + " ± " + generator.argsVar + "\n";

        try {
            Files.createParentDirs(targetDir);
            for(Map.Entry<String, String> name_body : sources.entrySet()) {
                try {
                    File targetFile = new File(targetDir, name_body.getKey() + "." + _ext);
                    Files.createParentDirs(targetFile);
                    Files.asCharSink(targetFile, Charset.defaultCharset()).write(name_body.getValue());
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Files.asCharSink(new File(targetDir, "readme.md"), Charset.defaultCharset()).write(readme);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> generate() {
        Map<String, String> result = new HashMap<>();
        for(int p = 0; p < packages; p++) {
            Map<String, String> pkgMembers = generatePackage(String.format("pkg%d", p));
            result.putAll(pkgMembers);
        }
        return result;
    }

    private Map<String, String> generatePackage(String pkg) {
        Map<String, String> result = new HashMap<>();
        for(int c = 0; c < classes; c++) {
            Tuple2<String, String> name_body = generateClass(pkg, c);
            result.put(name_body._1(), name_body._2());
        }
        return result;
    }

    private Tuple2<String, String> generateClass(String pkg, int c) {
        StringBuilder bodyBuilder = new StringBuilder();
        for(int i = 0; i < fields; i++) {
            bodyBuilder.append(generateField(i));
        }

        generateMethodSignatures();
        bodyBuilder.append("\n");
        for(Tuple2<String, Integer> method : methodSignatures) {
            bodyBuilder.append(generateMethod(method._1(), method._2()));
        }

        String name = String.format("%s/C%d", pkg, c);
        String body = String.format(class_tmpl, pkg, c, bodyBuilder);

        methodSignatures.clear();
        fieldNames.clear();

        return Tuple2.of(name, body);
    }

    private String generateField(int i) {
        fieldNames.add(String.format("f%d", i));
        return String.format(field_tmpl, i, random.nextInt());
    }

    private void generateMethodSignatures() {
        for(int i = 0; i < methods; i++) {
            methodSignatures.add(Tuple2.of(String.format("m%d", i), random.nextInt(2 * argsVar + 1) + args));
        }
    }

    private String generateMethod(String name, Integer argCount) {
        List<String> vars = IntStream.range(0, argCount).<String>mapToObj(i -> String.format("p%d", i))
                .collect(Collectors.toCollection(ArrayList::new));

        String args = vars.stream().map(var -> String.format("int %s", var)).collect(Collectors.joining(", "));
        vars.addAll(fieldNames);

        StringBuilder bodyBuilder = new StringBuilder();
        int callCount = random.nextInt(2 * exprsVar + 1) + exprs - exprsVar;

        for(int i = 0; i < callCount; i++) {
            bodyBuilder.append(generateExpr(i, vars));
            vars.add(String.format("v%d", i));
        }

        return String.format(method_tmpl, name, args, bodyBuilder, vars.get(vars.size() - 1));
    }

    private String generateExpr(int targetVarId, List<String> vars) {
        switch(expr) {
            case CALL: {
                Tuple2<String, Integer> method = randomElement(methodSignatures);
                final Stream<String> args = IntStream.range(0, method._2()).mapToObj(i -> randomElement(vars));
                return String.format(call_tmpl, targetVarId, method._1(), args.collect(Collectors.joining(", ")));
            }
            case PLUS: {
                int argCount = random.nextInt(2 * argsVar + 1) + args;
                final Stream<String> args = IntStream.range(0, argCount).mapToObj(i -> randomElement(vars));
                return String.format(plusminus_tmpl, targetVarId, args.collect(Collectors.joining(" + ")));
            }
            case MINUS: {
                int argCount = random.nextInt(2 * argsVar + 1) + args;
                final Stream<String> args = IntStream.range(0, argCount).mapToObj(i -> randomElement(vars));
                return String.format(plusminus_tmpl, targetVarId, args.collect(Collectors.joining(" - ")));
            }
            default:
                throw new IllegalStateException("Unexpected expression type " + expr);
        }


    }

    private <T> T randomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

}
