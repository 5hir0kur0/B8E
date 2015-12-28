package assembler;

/**
 * @author Noxgrim
 */
public class MnemonicNameToken extends Token {

    public MnemonicNameToken(String value) {
        super(value, TokenType.MNEMONIC_NAME);
    }
}
