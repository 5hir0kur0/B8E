package simplemath;

import java.util.*;

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

    private Tokenizer(List<String> tokenStringList, List<TokenType> tokenTypeList) {
        if (tokenStringList.size() != tokenTypeList.size())
            throw new IllegalArgumentException("Attempting to construct Tokenizer from lists of different length.");

        this.tokens = new ArrayList<Token>(tokenStringList.size());

        ListIterator<String> tokenStringsIterator = tokenStringList.listIterator();
        ListIterator<TokenType> typesIterator = tokenTypeList.listIterator();
        while (tokenIterator.hasNext() && typesIterator.hasNext()) {
            String tmpString = tokenStringsIterator.next();
            TokenType tmpType = typesIterator.next();
            switch (tmpType) {
                case OPERATOR:
                    if (tmpString.length() > 1)
                        throw new IllegalArgumentException("Invalid operator (longer than one char): "+tmpString);
                    this.tokens.add(new OperatorToken(tmpString.charAt(0)));
                    break;
                case NUMBER:
                    this.tokens.add(new NumberToken(Double.parseDouble(tmpString)));
                    break;
                case OPENING_BRACKET:
                case CLOSING_BRACKET:
                    this.tokens.add(new Token(tmpType));
                    break;
            }
        }
        this.tokenIterator = this.tokens.listIterator();
    }

    private Tokenizer(List<Token> tokenList) {
        this.tokens = new ArrayList<>(tokenList.size());
        tokenList.forEach(this.tokens::add);
        this.tokenIterator = tokens.listIterator();
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
        int brackets = 0;
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
                    ++brackets;
                    tokens.add(new Token(getType(chars[i])));
                    break;
                case CLOSING_BRACKET:
                    --brackets;
                    tokens.add(new Token(getType(chars[i])));
                    break;
                case OPERATOR:
                    tokens.add(new OperatorToken(chars[i]));
                    break;
            }
        }
        if (brackets != 0)
            throw new IllegalArgumentException("Invalid use of brackets. (missing opening or closing bracket(s))");
        this.tokenIterator = this.tokens.listIterator();
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
}
