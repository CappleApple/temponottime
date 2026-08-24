package com.cappleapple.temponottime.casting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Safely parses the server-configured reserve requirement for a numbered charge slot. */
public final class ChargeRequirementFormula {
    public static final String DEFAULT_EXPRESSION = "casting_draw * 2 ^ (charge - 1)";

    @FunctionalInterface
    public interface Compiled {
        double evaluate(double castingReserve, double castingDraw, int charge);
    }

    @FunctionalInterface
    private interface Node {
        double evaluate(Context context);
    }

    private record Context(double castingReserve, double castingDraw, int charge) {
    }

    private ChargeRequirementFormula() {
    }

    public static Compiled compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Charge requirement formula cannot be blank");
        }
        Parser parser = new Parser(expression);
        Node root = parser.parseExpression();
        parser.requireEnd();
        return (castingReserve, castingDraw, charge) ->
                root.evaluate(new Context(castingReserve, castingDraw, charge));
    }

    public static boolean isValidConfigValue(Object value) {
        if (!(value instanceof String expression)) return false;
        try {
            compile(expression);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static final class Parser {
        private final String expression;
        private int position;

        private Parser(String expression) {
            this.expression = expression;
        }

        private Node parseExpression() {
            Node left = parseTerm();
            while (true) {
                if (take('+')) {
                    Node right = parseTerm();
                    Node previous = left;
                    left = context -> previous.evaluate(context) + right.evaluate(context);
                } else if (take('-')) {
                    Node right = parseTerm();
                    Node previous = left;
                    left = context -> previous.evaluate(context) - right.evaluate(context);
                } else {
                    return left;
                }
            }
        }

        private Node parseTerm() {
            Node left = parseUnary();
            while (true) {
                if (take('*')) {
                    Node right = parseUnary();
                    Node previous = left;
                    left = context -> previous.evaluate(context) * right.evaluate(context);
                } else if (take('/')) {
                    Node right = parseUnary();
                    Node previous = left;
                    left = context -> previous.evaluate(context) / right.evaluate(context);
                } else if (take('%')) {
                    Node right = parseUnary();
                    Node previous = left;
                    left = context -> previous.evaluate(context) % right.evaluate(context);
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            if (take('+')) return parseUnary();
            if (take('-')) {
                Node value = parseUnary();
                return context -> -value.evaluate(context);
            }
            return parsePower();
        }

        private Node parsePower() {
            Node base = parsePrimary();
            if (!take('^')) return base;
            Node exponent = parseUnary();
            return context -> Math.pow(base.evaluate(context), exponent.evaluate(context));
        }

        private Node parsePrimary() {
            skipWhitespace();
            if (take('(')) {
                Node value = parseExpression();
                require(')');
                return value;
            }
            if (position < expression.length()
                    && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                double number = parseNumber();
                return context -> number;
            }
            String identifier = parseIdentifier().toLowerCase(Locale.ROOT);
            if (take('(')) {
                List<Node> arguments = new ArrayList<>();
                if (!peek(')')) {
                    do {
                        arguments.add(parseExpression());
                    } while (take(','));
                }
                require(')');
                return function(identifier, arguments);
            }
            return variable(identifier);
        }

        private Node variable(String identifier) {
            return switch (identifier) {
                case "casting_reserve" -> Context::castingReserve;
                case "casting_draw" -> Context::castingDraw;
                case "charge" -> context -> context.charge();
                case "pi" -> context -> Math.PI;
                case "e" -> context -> Math.E;
                default -> throw error("Unknown variable '" + identifier + "'");
            };
        }

        private Node function(String identifier, List<Node> arguments) {
            return switch (identifier) {
                case "pow" -> binary(identifier, arguments, Math::pow);
                case "min" -> binary(identifier, arguments, Math::min);
                case "max" -> binary(identifier, arguments, Math::max);
                case "abs" -> unary(identifier, arguments, Math::abs);
                case "sqrt" -> unary(identifier, arguments, Math::sqrt);
                case "floor" -> unary(identifier, arguments, Math::floor);
                case "ceil" -> unary(identifier, arguments, Math::ceil);
                case "log" -> unary(identifier, arguments, Math::log);
                case "log2" -> unary(identifier, arguments, value -> Math.log(value) / Math.log(2.0));
                default -> throw error("Unknown function '" + identifier + "'");
            };
        }

        private Node unary(String name, List<Node> arguments, java.util.function.DoubleUnaryOperator function) {
            if (arguments.size() != 1) throw error(name + " requires one argument");
            Node argument = arguments.getFirst();
            return context -> function.applyAsDouble(argument.evaluate(context));
        }

        private Node binary(String name, List<Node> arguments, java.util.function.DoubleBinaryOperator function) {
            if (arguments.size() != 2) throw error(name + " requires two arguments");
            Node first = arguments.get(0);
            Node second = arguments.get(1);
            return context -> function.applyAsDouble(first.evaluate(context), second.evaluate(context));
        }

        private double parseNumber() {
            skipWhitespace();
            int start = position;
            boolean exponent = false;
            while (position < expression.length()) {
                char current = expression.charAt(position);
                if (Character.isDigit(current) || current == '.') {
                    position++;
                } else if ((current == 'e' || current == 'E') && !exponent) {
                    exponent = true;
                    position++;
                    if (position < expression.length()
                            && (expression.charAt(position) == '+' || expression.charAt(position) == '-')) {
                        position++;
                    }
                } else {
                    break;
                }
            }
            try {
                return Double.parseDouble(expression.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private String parseIdentifier() {
            skipWhitespace();
            int start = position;
            while (position < expression.length()) {
                char current = expression.charAt(position);
                if (!Character.isLetterOrDigit(current) && current != '_') break;
                position++;
            }
            if (start == position) throw error("Expected a number, variable, function, or '('");
            return expression.substring(start, position);
        }

        private boolean take(char expected) {
            skipWhitespace();
            if (position >= expression.length() || expression.charAt(position) != expected) return false;
            position++;
            return true;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return position < expression.length() && expression.charAt(position) == expected;
        }

        private void require(char expected) {
            if (!take(expected)) throw error("Expected '" + expected + "'");
        }

        private void requireEnd() {
            skipWhitespace();
            if (position != expression.length()) throw error("Unexpected trailing input");
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) position++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + position + " in '" + expression + "'");
        }
    }
}
