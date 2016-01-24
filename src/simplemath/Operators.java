package simplemath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Miscellaneous methods regarding operators
 *
 * Apart from the standard operators (+,-,*,/,%,^,~) whose implementation can be found in the {@code SimpleMath} class,
 * this class defines the following additional operators (that the {@code SimpleMath} class can access:
 * <ul>
 *     <li><code>op1 &lt;&lt; op2</code>: Shift op1 left by op2 (cf. <code>op1 &lt;&lt; op2</code>)<br>
 *         NOTE: op1 is cast to <code>long</code> and op2 is cast to <code>int</code></li>
 *     <li><code>op1 &gt;&gt; op2</code>: Shift op1 left by op2 (cf. <code>op1 &gt;&gt; op2</code>)<br>
 *         NOTE: op1 is cast to <code>long</code> and op2 is cast to <code>int</code></li>
 *     <li><code>op1 &lt; op2</code>: Return 1 if op1 is smaller than op2, else return 0<br>
 *         NOTE: op1 and op2 are cast to <code>long</code></li>
 *     <li><code>op1 &gt; op2</code>: Return 1 if op1 is greater than op2, else return 0<br>
 *         NOTE: op1 and op2 are cast to <code>long</code></li>
 *     <li><code>op1 & op2</code>: Return the result of "op1 & op2"<br>
 *         NOTE: op1 and op2 are cast to <code>long</code></li>
 *     <li><code>op1 | op2</code>: Return the result of "op1 | op2"<br>
 *         NOTE: op1 and op2 are cast to <code>long</code></li>
 * </ul>
 * <br>
 * <b>New operators should be added here, not in {@code SimpleMath}!</b>
 * NOTE: If you add a new operator, you also need to add an operator weight for it.
 *
 * @author Gordian
 */
class Operators {

    /** "binary" in this case refers to the fact that these operators take two operands*/
    private static final HashMap<String, BiFunction<Double, Double, Double>> additionalBinaryOperators;

    /** List of all the operators that can be used for expressions intended to be parsed by SimpleMath */
    public static final List<String> VALID_OPERATORS;
    private static final String VALID_OPERATOR_CHARS = "+-*/%^<>&|~";

    //static initialization block
    static {
        additionalBinaryOperators = new HashMap<>();
        VALID_OPERATORS = new ArrayList<>(13);
        VALID_OPERATORS.addAll(Arrays.asList("+", "-", "*", "/", "%", "^", "~"));

        additionalBinaryOperators.put("<<", (Double d1, Double d2) -> ((double)((long)(double)d1 << (int)(double)d2)));
        additionalBinaryOperators.put(">>", (Double d1, Double d2) -> ((double)((long)(double)d1 >> (int)(double)d2)));
        additionalBinaryOperators.put("<",  (Double d1, Double d2) -> (long)(double)d1 < (long)(double)d2 ? 1D : 0D);
        additionalBinaryOperators.put(">",  (Double d1, Double d2) -> (long)(double)d1 > (long)(double)d2 ? 1D : 0D);
        additionalBinaryOperators.put("&",  (Double d1, Double d2) -> ((double)((long)(double)d1 & (long)(double)d2)));
        additionalBinaryOperators.put("|",  (Double d1, Double d2) -> ((double)((long)(double)d1 | (long)(double)d2)));

        VALID_OPERATORS.addAll(additionalBinaryOperators.keySet());
    }

    /**
     * Get the weight of an operator.
     * @param operator
     *     The {@code String} that represents the operator.
     * @return
     *     A numeric value representing the operator's importance.
     */
    static int getOpWeight(String operator) {
        switch (operator) {
            case "|":
                return 5;
            case "&":
                return 10;
            case "<":
            case ">":
                return 12;
            case "<<":
            case ">>":
                return 15;
            case "-":
            case "+":
                return 21;
            case "*":
            case "/":
            case "%":
                return 42;
            case "^":
                return 84;
            case "~":
                return 100;
            default:
                throw new IllegalArgumentException(operator+" is not a valid operator.");
        }
    }

    /**
     * Get the weight of an operator.
     * @param operator
     *     {@code char} representing the operator
     * @return
     *     A numeric value representing the operator's importance.
     * @see Operators#getOpWeight(String operator)
     */
    static int getOpWeight(OperatorToken operator) {
        return getOpWeight(operator.getOperator());
    }

    static boolean isValidOperator(String operator) {
        return VALID_OPERATORS.contains(operator);
    }

    static boolean isUnaryOperator(char c) {
        return "+-~".contains(Character.toString(c));
    }

    static boolean isValidOperatorChar(char c) {
        return VALID_OPERATOR_CHARS.contains(Character.toString(c));
    }

    static BiFunction<Double,Double,Double> getOperatorFunction(OperatorToken operator) {
        return additionalBinaryOperators.get(operator.getOperator());
    }
}
