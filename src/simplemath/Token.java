package simplemath;

/**
 * This class represents a single token of an arithmetic expression.
 *
 * @author Gordian
 */
class Token {

    private final TokenType type;

    /**
     * Construct a {@code Token}.
     * This constructor is only needed by the extending classes and to construct bracket tokens which are neither
     * operators nor numbers.
     * @param type
     *     The {@code Token}'s {@code TokenType}
     */
    protected Token(TokenType type) {
        this.type = type;
    }


    /** @return the {@code char} representing the operator (if the {@code Token} is an {@code OperatorToken})*/
    public String getOperator() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("getOperator() was called on Token.");
    }

    /** @return the number ({@code double}) held by the {@code Token} (if the {@code Token} is a {@code NumberToken} )*/
    public double getNumber() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("getNumer() was called on Token.");
    }

    public TokenType getType() { return this.type; }

    public boolean isOpeningBracket() { return this.type == TokenType.OPENING_BRACKET; }

    public boolean isClosingBracket() { return this.type == TokenType.CLOSING_BRACKET; }

    public boolean isNumber() { return this.type == TokenType.NUMBER; }

    public boolean isOperator() { return this.type == TokenType.OPERATOR; }

    @Override
    public String toString() {
        return "Token["+type+"]";
    }
}

/**
 * This class represents a number (stored as double) in an arithmetic expression.
 *
 * @author Gordian
 */
class NumberToken extends Token {

    private final double number;

    NumberToken(double number) {
        super(TokenType.NUMBER);
        this.number = number;
    }

    /** If the {@code Token} is a {@code NumberToken} return the number ({@code double}) it holds.*/
    @Override
    public double getNumber() { return this.number; }

    @Override
    public String toString() {
        return "NumberToken["+this.number+"]";
    }
}

/**
 * This class represents an operator in an arithmetic expression.
 *
 * @author Gordian
 */
class OperatorToken extends Token {

    private final String operator;

    /**
     * @param operator
     *     Only the operators described in Operators.VALID_OPERATORS can be stored using this class.
     */
    OperatorToken(String operator) {
        super(TokenType.OPERATOR);
        this.operator = operator;
        if (!Operators.isValidOperator(operator))
            throw new IllegalArgumentException("Invalid operator: "+operator);
    }

    /** If the {@code Token} is an {@code OperatorToken}, return the {@code char} representing the operator.*/
    @Override
    public String getOperator() throws UnsupportedOperationException {
        return this.operator;
    }

    @Override
    public String toString() {
        return "OperatorToken["+this.operator+"]";
    }
}
