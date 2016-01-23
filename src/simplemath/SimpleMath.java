package simplemath;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.function.BiPredicate;

/**
 * Simple parser for arithmetic expressions
 * Only simple expressions are supported (i.e. you can't create or access variables, functions, ...)
 * NOTE: Unary pluses and minuses must be at the start of the expression or after an opening bracket.
 * @author 5hir0kur0
 */
public class SimpleMath {

    /**
     * Evaluate a simple arithmetic expression.
     * <br>
     * This method supports:
     * <ul>
     *     <li>Simple arithmetic expressions such as 1+1 <br>
     *         Supported operators: '+', '-', '*', '/', '^', '%', '~'<br>
     *         Additional operators defined in class {@code Operators}: '&lt;&lt;', '&gt;&gt;', '&lt;', '&gt;', '|', '&'
     *     </li>
     *     <li>(Nested) Brackets</li>
     * </ul>
     * @param expression
     *     The expression to be evaluated in infix notation.
     * @throws IllegalArgumentException
     *     When the expression is invalid
     * @throws NumberFormatException
     *     When an operand can't be parsed as a {@code double} by {@code Double.parseDouble()}
     * @throws RuntimeException Can throw other {@code RuntimeException}s when there is an internal error.
     * @return
     *     The result of the evaluation as a {@code double}
     */
    public static double evaluate(String expression) {
        return evaluate(new Tokenizer(expression));
    }

    /**
     * Convert an arithmetic expression in infix notation (e.g 1+2) to postfix notation (e.g 12+)
     * Only simple expressions not containing brackets are supported.
     * <br>
     * A description of the algorithm used to accomplish this task can be found at:
     *     http://scriptasylum.com/tutorials/infix_postfix/algorithms/infix-postfix/index.htm
     * <br>
     * Basically the list of {@code Tokens} is being scanned from left to right, all operands (numbers) are pushed onto
     * a {@code Stack}. If the {@code Token} is an operator it is compared to the operator on top of the {@code Stack}.
     * If the operator on top of the {@code Stack} has a higher or equal weight than the current operator, the top of
     * the {@code Stack} is pushed to the postfix list. Therefore the operators with higher weight are going to be
     * executed first when the expression is evaluated. This process continues until the top of the {@code Stack} has a
     * lower weight than the current operator or the {@code Stack} is empty. Then the current operator is pushed onto
     * the {@code Stack}.
     *
     * @param tokenizer
     *     The {@code Tokenizer} which will be converted to postfix. It must not be {@code null}.
     *
     * @return
     *     The resulting {@code List} of {@code Token}s in postfix order.
     */
    private static List<Token> postfix(Tokenizer tokenizer) {
        Stack<Token> stack = new Stack<>();
        List<Token> postfix = new LinkedList<>();
        BiPredicate<Token, Token> hasHigherOrEqualWeight = (t1, t2) ->
            Operators.getOpWeight(t1.getOperator()) - Operators.getOpWeight(t2.getOperator()) >= 0;

        while (tokenizer.hasNext()) {
            Token t = tokenizer.next();
            switch (t.getType()) {
                case NUMBER:
                    postfix.add(t);
                    break;
                case OPERATOR:
                    if (!stack.isEmpty() && hasHigherOrEqualWeight.test(stack.peek(), t)) {
                        while (!stack.isEmpty() && hasHigherOrEqualWeight.test(stack.peek(), t)) {
                            postfix.add(stack.pop());
                        }
                        stack.push(t);
                    } else {
                        stack.push(t);
                    }
                    break;
                case OPENING_BRACKET:
                case CLOSING_BRACKET:
                    throw new IllegalArgumentException("Cannot create postfix expression if the source contains brackets.");
            }
        }

        while (!stack.isEmpty()) postfix.add(stack.pop());

        return postfix;
    }

    /**
     * Evaluate an expression in postfix notation. The expression must not contain brackes.
     *
     * The algorithm used to perform the evaluation can be found at:
     *     algorithm from http://scriptasylum.com/tutorials/infix_postfix/algorithms/postfix-evaluation/index.htm
     *
     * Basically the expression is iterated over from left to right, all number tokens ({@code double}) are pushed onto
     * a {@code Stack} and all operator tokens are evaluated using the elements on the {@code Stack}.
     * Note that the operators '+' and '-' are special as they can be unary (as opposed to binary) operators, which
     * means it is possible for them to only operate on one operand.
     *
     * @param postfix
     *     The postfix expression to evaluate. It must not be {@code null} and it must not contain brackets.
     *
     * @return
     *     The result of the evaluation as a {@code double}
     */
    private static double evaluatePostfix(List<Token> postfix) {
        Stack<Double> stack = new Stack<>();
        for (Token t : postfix) {
            switch (t.getType()) {
                case NUMBER:
                    stack.push(t.getNumber());
                    break;
                case OPERATOR:
                    Double t1, t2;
                    t2 = stack.pop();
                    Double result = 0.0;
                    switch (t.getOperator()) {
                        case "+":
                            if (stack.isEmpty()) result = +t2;
                            else {
                                t1 = stack.pop();
                                result = t1 + t2;
                            } break;
                        case "-":
                            if (stack.isEmpty()) result = -t2;
                            else {
                                t1 = stack.pop();
                                result = t1 - t2;
                            } break;
                        case "*":
                            t1 = stack.pop();
                            result = t1 * t2; break;
                        case "/":
                            t1 = stack.pop();
                            result = t1 / t2; break;
                        case "%":
                            t1 = stack.pop();
                            result = t1 % t2; break;
                        case "^":
                            t1 = stack.pop();
                            result = Math.pow(t1, t2); break;
                        case "~":
                            result = (double)~(long)(double)t2; break;
                        default:
                            t1 = stack.pop();
                            result = Operators.getOperatorFunction((OperatorToken)t).apply(t1, t2);
                    }
                    stack.push(result);
                    break;
                case OPENING_BRACKET:
                case CLOSING_BRACKET:
                    throw new IllegalArgumentException("Cannot evaluate postfix expression if it contains brackets.");
            }
        }
        return stack.pop();
    }

    /**
     * Recursive method to evaluate an arithmetic expression.
     * Every bracket results in a new call. The bracket's expression is replaced by its result.
     * @param tokenizer
     *     {@code Tokenizer} to be evaluated.
     * @return
     *     Result of the evaluation as {@code double}
     */
    private static double evaluate(Tokenizer tokenizer) {
        while (tokenizer.hasNext()) {
            Token t = tokenizer.next();
            if (t.isOpeningBracket())
                tokenizer.replaceUntilMatchingBracket(evaluate(tokenizer.subTokenizerUntilMatchingBracket()));
        }
        tokenizer.resetIndex();
        return evaluatePostfix(postfix(tokenizer));
    }
}
