package simplemath;

import java.util.Arrays;
import java.util.List;

/**
 * Miscellaneous methods and data for SimpleMath
 *
 * @author 5hir0kur0
 */
class Misc {
    /** List of all the operators that can be used for expressions intended to be parsed by SimpleMath */
    public static final List<String> VALID_OPERATORS = Arrays.asList("+", "-", "*", "/", "%", "^");

    /**
     * Get the weight of an operator.
     * @param operator
     *     The {@code String} that represents the operator.
     * @return
     *     A numeric value representing the operator's importance.
     */
    static int getOpWeight(String operator) {
        switch (operator) {
            case "-":
            case "+":
                return 21;
            case "*":
            case "/":
            case "%":
                return 42;
            case "^":
                return 84;
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
     * @see Misc#getOpWeight(String operator)
     */
    static int getOpWeight(char operator) {
        return getOpWeight(Character.toString(operator));
    }

    static boolean isValidOperator(String operator) {
        return VALID_OPERATORS.contains(operator);
    }

    static boolean isValidOperator(char operator) {
        return isValidOperator(Character.toString(operator));
    }
}
