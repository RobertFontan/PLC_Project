package plc.project;

import sun.awt.PeerEvent;

import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        // empty list of tokens
        List<Token> tokens = new ArrayList<Token>();
        /* while there are things to lex */
        while(chars.has(0)) {
            // if white space skip
            if (match("[ \b\n\r\t]")) {
                chars.skip();
            }
            else {
                tokens.add(lexToken());
            }
        }
        return tokens;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // could maybe be a switch statement
        if(peek("(@|[A-Za-z])[A-Za-z0-9_-]*")){
            return lexIdentifier();
        }
        else if (peek("[-]|[0-9]")){ //have to update it for number
            return lexNumber();
        }
        else if(peek("'")){
            return lexCharacter();
        }
        else if(peek("\"")){
            return lexString();
        }
        // defined as any other character
        else
            return lexOperator();
        //throw new UnsupportedOperationException(); //TODO

    }

    public Token lexIdentifier() {
        System.out.println("identifier found");
        if(peek("[A-Za-z]"))            //Checks initial state (no digit, underscore, or hyphen)
            match("[A-Za-z]");
        while(peek("[A-Za-z0-9_-]"))    //Checks the rest of the token, including digits, underscores, and hyphens
            match("[A-Za-z0-9_-]");
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek("[-]?"))   // MATCH IF NEGATIVE IS PRESENT
            match("-");

        if (peek("\\.")) {  // MATCH ON LEADING DECIMAL
            match("\\.");
            if(peek("[0-9]"))
                while (match("[0-9]"));
                return chars.emit(Token.Type.DECIMAL);
        }

        if(peek("0[0-9]+")) {  //INVALID LEADING ZERO FOLLOWED BY MORE INTEGERS
            throw new ParseException("INVALID: LEADING ZERO", chars.index);
        }

        if (peek("[0-9]")) {
            //System.out.println("last number");
            while (match("[0-9]"));

            if (peek("\\.")) {
                match("\\.");
                if (peek("[0-9]+")) {
                    while (match("[0-9]"));
                    return chars.emit(Token.Type.DECIMAL);
                }
                else {
                    throw new ParseException("Invalid: Trailing decimal", chars.index);
                }
            }
            else
                return chars.emit(Token.Type.INTEGER);
        }
        else
            throw new ParseException("Invalid Number", chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }


    public Token lexCharacter() {
        System.out.println("Character Found");
        if(peek("'")) {
            match("'");

            if(peek("'"))
                throw new ParseException("Empty Char", chars.index);

            if (peek("[^'\\\\]")) {   // CHECKS FOR ANY CHARACTER OTHER THAN
                match("[^'\\\\]");
            }
        }
        if (match("\\\\")) {
            if(peek("[bnrt'\"\\\\]")) {
                lexEscape();
            }
        }
        if(peek("'")) {
            match("'");
            return chars.emit(Token.Type.CHARACTER);
        }
        else
            throw new ParseException("Missing Single Quote", chars.index);

    }

    // roberts section
    public Token lexString() {
        System.out.println("String Found");
        if(peek("\"")) {
            match("\"");
            while (peek("[^\"|\n]")) {
                if (match("\\\\")){
                 lexEscape();
            }
                else
                    match(".");
            }
            if (peek("\""))
                match("\"");
            else
                throw new ParseException("Invalid Missing End Quotes", chars.index);
        }
        else
            throw new ParseException("Invalid", chars.index);
        // end quotes

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        System.out.println("Escape Found");
        if (peek("[bnrt'\"\\\\]"))
            match("[bnrt'\"\\\\]");
        else {
            peek(".?");
            System.out.println(chars.index);
            throw new ParseException("Invalid", chars.index);
        }
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        System.out.println("Found Operator");
        if (peek("[!=]","="))
            match("[!=]","=");
        else if(peek("&","&"))
            match("&","&");
        else if (peek("|","|"))
            match("|","|");
        else
            match(".");

        return chars.emit(Token.Type.OPERATOR);
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for (int i = 0; i < patterns.length; i++) {

            if (!chars.has(i) ||
                    !String.valueOf(chars.get(i)).matches(patterns[i])) {

                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if(peek) {

            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }

        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}