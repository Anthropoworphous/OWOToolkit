package main.math;

import org.apache.commons.math3.util.FastMath;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScientificCalculation {
    public static class Function {
        private final List<Component> content = new ArrayList<>();

        public Function(String input) throws NumberFormatException {
            String pattern = "(%s)|(%s)|(%s)|(%s)|(%s)".formatted(
                    Number.PATTERN,
                    Constant.PATTERN,
                    UniOperator.PATTERN,
                    MergeOperator.PATTERN,
                    SpecialCharacter.PATTERN);
            Matcher m = Pattern.compile(pattern).matcher(input.replaceAll("\\s", ""));
            
            while (m.find()) {
                if (m.group(1) != null) {
                    content().add(new Number(Double.parseDouble(m.group(1))));
                } else if (m.group(2) != null) {
                    content().add(Arrays.stream(Constant.values())
                            .filter(v -> List.of(v.syntax()).contains(m.group(2)))
                            .findFirst().orElseThrow().value());
                } else if (m.group(3) != null) {
                    content().add(Arrays.stream(UniOperator.values())
                            .filter(v -> List.of(v.syntax()).contains(m.group(3)))
                            .findFirst().orElseThrow());
                } else if (m.group(4) != null) {
                    content().add(Arrays.stream(MergeOperator.values())
                            .filter(v -> List.of(v.syntax()).contains(m.group(4)))
                            .findFirst().orElseThrow());
                } else if (m.group(5) != null) {
                    content().add(Arrays.stream(SpecialCharacter.values())
                            .filter(v -> v.c.equals(m.group(5)))
                            .findFirst().orElseThrow());
                } else {
                    throw new NumberFormatException();
                }
            }
        }

        public List<Component> content() {
            return content;
        }
    }

    public static class Calculator {
        private final Stack topStack;
        
        public Calculator(Function function) throws NumberFormatException {
            topStack = stacking(function.content());
        }

        // the end
        public double solve() { return Stack.eval(topStack.components()).number(); }

        // static
        private static Stack stacking(List<Component> allComponents) throws NumberFormatException {
            Stack.Builder stackBuilder = new Stack.Builder();
            for (Component v : allComponents) {
                if (v instanceof SpecialCharacter) {
                    if (((SpecialCharacter) v).c.equals("(")) {
                        stackBuilder.startSub();
                    } else if (((SpecialCharacter) v).c.equals(")")) {
                        stackBuilder.endSub();
                    } else {
                        throw new NumberFormatException();
                    }
                } else {
                    stackBuilder.append(v);
                }
            }
            return stackBuilder.build();
        }

        // class
        private static class Stack implements Component {
            private final List<Component> components;

            public Stack(List<Component> components) {
                this.components = components;
            }

            // getter
            public List<Component> components() {
                return components;
            }

            // static
            public static Number eval(List<Component> components) throws NumberFormatException {
                // handle subStack
                components.replaceAll(c -> {
                    if (c instanceof Stack s) {
                        return eval(s.components);
                    }
                    return c;
                });

                // handle one number
                if (components.size() == 1 && components.get(0) instanceof Number n) { return n; }

                // handle uniOp
                List<UniOperator> unis = new ArrayList<>();
                for (int i = 0; i < components.size(); i++) {
                    Component c = components.get(i);
                    if (c instanceof UniOperator) {
                        unis.add((UniOperator) components.remove(i));
                        if (i < components.size()) {
                            // break point
                            if (components.get(i) instanceof Number n) {
                                Collections.reverse(unis);
                                components.set(i, new PendingNumber(n, unis));
                                unis.clear();
                            } else if (!(components.get(i) instanceof UniOperator)) {
                                throw new NumberFormatException("uniOp pointing to invalid object: type: %s || value: %s"
                                        .formatted(components.get(i).getClass().getSimpleName(), components.get(i))
                                );
                            }
                        } else {
                            throw new NumberFormatException("uniOp pointing to null");
                        }
                    }
                }

                // handle mergeOp
                Map<MergeOperator, Number> pending = new LinkedHashMap<>();
                List<Number> numbers = components.stream()
                        .<Number>mapMulti((c, oc) -> {
                            if (c instanceof Number n) { oc.accept(n); }
                        }).collect(Collectors.toCollection(ArrayList::new));
                List<MergeOperator> ops = components.stream()
                        .<MergeOperator>mapMulti((c, oc) -> {
                            if (c instanceof MergeOperator n) { oc.accept(n); }
                        }).collect(Collectors.toCollection(ArrayList::new));
                if (numbers.size()-1 != ops.size()) { throw new NumberFormatException(
                        "number size: %d, operation size: %d".formatted(numbers.size(), ops.size())
                ); }
                for (int i = 0; i < ops.size(); i++) {
                    MergeOperator op = ops.get(i);
                    // find cutting point (priority > next op's priority),
                    // backtrack till list's head
                    // won't get any cutting point cause those are cleared at last cycle
                    if (i+1 >= ops.size() || op.priority() > ops.get(i+1).priority()) {
                        List<MergeOperator> subList = ops.subList(0, i+1);
                        ops = ops.subList(i, ops.size());
                        i = 0;
                        for (int j = subList.size()-1; j >= 0 ; j--) {
                            pending.put(subList.get(j), numbers.remove(j));
                        }
                        numbers.set(0, new PendingNumber(numbers.get(0), pending));
                        pending.clear();
                    }
                }

                // only number is allowed at the end
                if (numbers.size() > 1) { throw new NumberFormatException(); }
                return numbers.get(0);
            }

            // class
            public static class PendingNumber extends Number {
                public PendingNumber(Number base, List<UniOperator> ops) {
                    super(apply(base, ops));
                }
                private static double apply(Number base, List<UniOperator> ops) {
                    for (UniOperator op : ops) {
                        op.operation().accept(base);
                    }
                    return base.number();
                }

                public PendingNumber(Number base, Map<MergeOperator, Number> ops) {
                    super(apply(base, ops));
                }
                private static double apply(Number base, Map<MergeOperator, Number> ops) {
                    for (Map.Entry<MergeOperator, Number> entry : ops.entrySet()) {
                        entry.getKey().operation().accept(base, entry.getValue());
                    }
                    return base.number();
                }
            }

            public static class Builder {
                private final List<Component> components = new ArrayList<>();
                private Builder subBuilder = null;

                public void append(Component c) {
                    if (subBuilder == null) {
                        components.add(c);
                    } else {
                        subBuilder.append(c);
                    }
                }

                public void startSub() {
                    if (subBuilder == null) {
                        subBuilder = new Builder();
                    } else {
                        subBuilder.startSub();
                    }
                }
                public void endSub() throws NumberFormatException {
                    if (subBuilder != null) {
                        if (subBuilder.subBuilder != null) {
                            subBuilder.endSub();
                        } else {
                            components.add(subBuilder.build());
                            subBuilder = null;
                        }
                    } else {
                        throw new NumberFormatException();
                    }
                }

                public Stack build() throws NumberFormatException {
                    if (subBuilder != null) { throw new NumberFormatException(); }
                    return new Stack(components);
                }
            }
        }
    }

    public static class Number implements Component {
        public static final String PATTERN = "(?:(?<!\\d)-)?\\d+(?:\\.\\d+)?";

        private Double number;

        public Number(Double number) {
            this.number(number);
        }
        public Number(String strNumber) throws NumberFormatException {
            this.number(Double.parseDouble(strNumber));
        }

        public void edit(UnaryOperator<Double> operation) {
            number(operation.apply(number()));
        }

        public Double number() {
            return number;
        }
        public void number(Double number) {
            this.number = number;
        }

        @Override
        public String toString() { return number.toString(); }
    }

    public enum SpecialCharacter implements Component {
        OPEN_PARENTHESES("("),
        CLOSE_PARENTHESES(")");

        public static final String PATTERN = "\\(|\\)";
        public final String c;
        SpecialCharacter(String c) {
            this.c = c;
        }

    }
    
    public enum Constant implements Component {
        E(Math.E, "e"),
        PI(Math.PI, "pi"),
        POSITIVE_INFINITY(Double.POSITIVE_INFINITY, "inf"),
        NEGATIVE_INFINITY(Double.NEGATIVE_INFINITY, "ninf");

        public static final String PATTERN = Arrays.stream(Constant.values())
                .flatMap(v -> Arrays.stream(v.syntax()))
                .collect(Collectors.joining("|"));
        private final Number value;
        private final String[] syntax;

        Constant(double value, String... syntax) {
            this.value = new Number(value);
            this.syntax = syntax;
        }
        
        public String[] syntax() { return syntax; }
        public Number value() { return value; }

    }

    public enum UniOperator implements Component {
        ABSOLUTE(num -> num.edit(FastMath::abs), "abs"),
        SQRT(num -> num.edit(FastMath::sqrt), "sqrt"),
        LOG(num -> num.edit(FastMath::log10), "log"),
        NATURAL_LOG(num -> num.edit(FastMath::log), "ln"),
        SIN(num -> num.edit(FastMath::sin), "sin"),
        COS(num -> num.edit(FastMath::cos), "cos"),
        TAN(num -> num.edit(FastMath::tan), "tan"),
        ARC_SIN(num -> num.edit(FastMath::asin), "asin", "arcsin"),
        ARC_COS(num -> num.edit(FastMath::acos), "acos", "arccos"),
        ARC_TAN(num -> num.edit(FastMath::atan), "atan", "arctan"),
        HYPERBOLIC_SIN(num -> num.edit(FastMath::sinh), "sinh"),
        HYPERBOLIC_COS(num -> num.edit(FastMath::cosh), "cosh"),
        HYPERBOLIC_TAN(num -> num.edit(FastMath::tanh), "tanh"),
        HYPERBOLIC_ARC_SIN(num -> num.edit(FastMath::asinh), "asinh"),
        HYPERBOLIC_ARC_COS(num -> num.edit(FastMath::acosh), "acosh"),
        HYPERBOLIC_ARC_TAN(num -> num.edit(FastMath::atanh), "atanh");

        public static final String PATTERN = Arrays.stream(UniOperator.values())
                .flatMap(v -> Arrays.stream(v.syntax))
                .collect(Collectors.joining("|"));
        private final Consumer<Number> operation;
        private final String[] syntax;

        UniOperator(Consumer<Number> operation, String... syntax) {
            this.operation = operation;
            this.syntax = syntax;
        }

        public String[] syntax() { return syntax; }
        public Consumer<Number> operation() { return operation; }

    }

    public enum MergeOperator implements Component {
        ADD((n1, n2) -> n1.edit(d -> d + n2.number()), 1, "+", "add"),
        MINUS((n1, n2) -> n1.edit(d -> d - n2.number()), 1, "-", "minus"),
        MULTIPLY((n1, n2) -> n1.edit(d -> d * n2.number()), 2, "*", "x", "times"),
        DIVIDE((n1, n2) -> n1.edit(d -> d / n2.number()), 2, "/", "divide"),
        MODULUS((n1, n2) -> n1.edit(d -> d % n2.number()), 2, "%", "mod"),
        POWER((n1, n2) -> n1.edit(d -> FastMath.pow(n2.number(), d)), 3, "^", "pow");

        public static final String PATTERN = Arrays.stream(MergeOperator.values())
                .flatMap(v -> Arrays.stream(v.syntax))
                .map(v -> v.replaceAll("\\W.*", "\\\\$0"))
                .collect(Collectors.joining("|"));
        private final BiConsumer<Number, Number> operation;
        private final int priority;
        private final String[] syntax;

        MergeOperator(BiConsumer<Number, Number> operation, int priority, String... syntax) {
            this.operation = operation;
            this.priority = priority;
            this.syntax = syntax;
        }

        public String[] syntax() { return syntax; }

        public BiConsumer<Number, Number> operation() {
            return operation;
        }

        public int priority() {
            return priority;
        }
    }

    private interface Component {}
}
