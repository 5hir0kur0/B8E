package simplemath;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * This class holds a list of {@code Token}s of an arithmetic expression.
 * <br>
 * The constructor can create such a list from a {@code String} containing an arithmetic expression. Whitespace is
 * ignored. Only simple expressions are supported (i.e. you can't create or access variables, functions, ...)
 *
 * @author 5hir0kur0
 */
class Tokenizer {
    private final ArrayList<Token> tokens;
    private ListIterator<Token> tokenIterator;

    private Tokenizer(List<Token> tokenList) {
        this.tokens = new ArrayList<>(tokenList.size());
        tokenList.forEach(this.tokens::add);
        this.tokenIterator = tokens.listIterator();
        checkIntegrity();
    }

    /**
     * Parse a list of {@code Token}s from a {@code String}.
     *
     * @param s
     *     The {@code String} to parse. It must not be {@code null} or empty.
     */
    Tokenizer(String s) {
        s = s.replaceAll("\\s", "");
        s = s.replaceAll("\\[", "(");
        s = s.replaceAll("\\]", ")");
        s = s.replaceAll("\\{", "(");
        s = s.replaceAll("\\}", ")");

        char[] chars = s.toCharArray();

        this.tokens = new ArrayList<>(chars.length / 2);

        StringBuilder tmp = new StringBuilder(16);
        for (int i = 0; i < chars.length; ++i) {
            if (!Character.isBmpCodePoint(chars[i])) throw new IllegalArgumentException("Only BMP supported.");
            switch (getType(chars[i])) {
                case NUMBER:
                    while (i < chars.length && getType(chars[i]) == TokenType.NUMBER) {
                        tmp.append(chars[i++]);
                    }
                    --i;
                    tokens.add(new NumberToken(Double.parseDouble(tmp.toString())));
                    tmp.setLength(0);
                    break;
                case OPENING_BRACKET:
                    tokens.add(new Token(getType(chars[i])));
                    break;
                case CLOSING_BRACKET:
                    tokens.add(new Token(getType(chars[i])));
                    break;
                case OPERATOR:
                    tokens.add(new OperatorToken(chars[i]));
                    break;
            }
        }
        this.tokenIterator = this.tokens.listIterator();
        checkIntegrity();
    }

    Token next() {
        return tokenIterator.next();
    }

    boolean hasNext() {
        return this.tokenIterator.hasNext();
    }

    /**
     * Returns a sub-{@code Tokenizer} up to the matching bracket to the current index.
     * The generated {@code Tokenizer} will not contain the enclosing brackets.
     * @return A new {@code Tokenizer}.
     */
    Tokenizer subTokenizerUntilMatchingBracket() {
        if (!tokens.get(tokenIterator.previousIndex()).isOpeningBracket())
            throw new IllegalArgumentException("subTokenizerUntilMatchingBracket() can only be called if "+
                                               "the current token is an opening bracket.");
        int brackets = 1;
        int oldIndex = this.tokenIterator.nextIndex();
        int lastBracketIndex = 0;

        LinkedList<Token> tmpTokenList = new LinkedList<>();
        Token tmpTok;

        while (brackets > 0) {
            tmpTok = next();
            switch (tmpTok.getType()) {
                case OPENING_BRACKET:
                    tmpTokenList.add(tmpTok);
                    ++brackets;
                    break;
                case CLOSING_BRACKET:
                    tmpTokenList.add(tmpTok);
                    --brackets;
                    break;
                default:
                    tmpTokenList.add(tmpTok);
            }
        }

        tmpTokenList.pollLast();

        if (tmpTokenList.isEmpty()) throw new IllegalStateException("Empty brackets are not allowed.");

        this.tokenIterator = this.tokens.listIterator(oldIndex);

        return new Tokenizer(tmpTokenList);
    }

    /**
     * Replace all tokens until the matching closing bracket of the current index.
     * This method alters the index of the iterator. The new index will be the one after the inserted value.
     * @param val
     *     The {@code double} the tokens will be replaced with.
     */
    void replaceUntilMatchingBracket(double val) {
        int brackets = 1;
        tokenIterator.previous(); //the current position is the bracket (which should also be replaced)
        Token tmpTok;
        while (brackets > 0) {
            tokenIterator.remove();
            tmpTok = next();
            if (tmpTok.isOpeningBracket()) ++brackets;
            else if (tmpTok.isClosingBracket()) --brackets;
        }
        tokenIterator.remove();
        tokenIterator.add(new NumberToken(val));
    }

    /** Set the index of the internal ListIterator to 0 */
    void resetIndex() {
        tokenIterator = tokens.listIterator();
    }

    public String toString() {
        return "Tokenizer["+tokens.toString()+"]";
    }

    private TokenType getType(char c) {
        //The point is needed, because of the parsed numbers are doubles.
        if (Character.isDigit(c) || c == '.') return TokenType.NUMBER;
        if (c == '(') return TokenType.OPENING_BRACKET;
        if (c == ')') return TokenType.CLOSING_BRACKET;
        if (Misc.isValidOperator(c)) return TokenType.OPERATOR;
        throw new IllegalArgumentException("'"+c+"' is not a valid character for a mathematical expression.");
    }

    /**
     * Check whether there are illegal sequences of operators in '{@code tokens}'.
     * @throws IllegalArgumentException
     */
    private void checkIntegrity() throws IllegalArgumentException {
        resetIndex();
        if (!tokenIterator.hasNext())
            throw new IllegalArgumentException("Empty expression.");
        Token current = null;
        Token previous;
        Predicate<Token> isPlusOrMinus = t -> (t.isOperator() && t.getOperator() == '+' || t.getOperator() == '-');
        int brackets = 0;
        int numbers = 0;
        while (tokenIterator.hasNext()) {
            previous = current;
            current = tokenIterator.next();
            if (previous == null) {
                switch (current.getType()) {
                    case OPERATOR:
                        if (!isPlusOrMinus.test(current))
                            throw new IllegalArgumentException("Illegal start of expression (non-unary operator):"
                                                               +current.getOperator());
                        break;
                    case CLOSING_BRACKET:
                        throw new IllegalArgumentException("Illegal start of expression (closing bracket)");
                    case OPENING_BRACKET: ++brackets; break;
                    case NUMBER: ++numbers; break;
                }
                continue;
            }
            switch (current.getType()) {
                case OPERATOR:
                    if (!isPlusOrMinus.test(current)) {
                        if (!previous.isNumber() && !previous.isClosingBracket())
                            throw new IllegalArgumentException("Illegal expression before binary operator "
                                                               +current.getOperator()+": "+previous);
                    } else {
                        if (previous.isOperator())
                            throw new IllegalArgumentException("Illegal expression: sequence of two operators: "
                                                               +previous.getOperator()+current.getOperator());
                    }
                    break;
                case OPENING_BRACKET:
                    if (!previous.isOperator() && !previous.isOpeningBracket())
                        throw new IllegalArgumentException("Illegal expression (token before opening bracket is not an"
                                                           +"operator or an opening bracket): "+previous+"(");
                    ++brackets; break;
                case CLOSING_BRACKET:
                    if (previous.isOpeningBracket())
                        throw new IllegalArgumentException("Illegal sequence of brackets: ()");
                    else if (previous.isOperator())
                        throw new IllegalArgumentException("Illegal expression: "+previous.getOperator()+")");
                    --brackets;
                    break;
                case NUMBER: ++numbers; break;
            }
        }
        if (null != current && (current.isOperator() || current.isOpeningBracket()))
            throw new IllegalArgumentException("Illegal end of expression: "+current);
        if (brackets != 0) throw new IllegalArgumentException("Illegal use of brackets: number of opening brackets"
                                                              +"does not match number of closing brackets.");
        if (numbers == 0) throw new IllegalArgumentException("Illegal expression: no numbers.");
        resetIndex();
    }
}
