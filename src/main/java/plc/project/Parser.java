package plc.project;

import java.awt.print.PrinterAbortException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
            List<Ast.Global> global = new ArrayList<>();
            List<Ast.Function> function = new ArrayList<>();

            while (peek("LIST") || peek("VAR") || peek("VAL")) {
                    global.add(parseGlobal());
            }
            while (peek("FUN")) {
                function.add(parseFunction());
            }
            return new Ast.Source(global, function);

    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global global;
        if (match("LIST")) {
            global = parseList();
            if(match(";"))
                return global;
        }
        else if (match("VAR")) {
            global = parseMutable();
            if(match(";"))
                return global;
        }
        else if (match("VAL")) {
            global = parseImmutable();
            if(match(";"))
                return global;
        }
        throw new ParseException("No semicolon: ", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        //TODO, in progress
        //list ::= 'LIST' identifier         ':' identifier         '=' '[' expression (',' expression)* ']'

        match("LIST");
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        String name = tokens.get(-1).getLiteral();

        if(!match(":"))
            throw new ParseException("Expected ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        else if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        if(!match("="))
            throw new ParseException("Expected '='", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        else if(!match("["))
            throw new ParseException("Expected '['", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        List<Ast.Expression> expr = new ArrayList<Ast.Expression>();
        expr.add(parseExpression());

        while(!peek("]")) {
            if(match(",")) {
                if(peek("]"))
                    throw new ParseException("Trailing comma", tokens.get(-1).getIndex());
                expr.add(parseExpression());
            }
        }
        match("]");
        return new Ast.Global(name,false, Optional.of(new Ast.Expression.PlcList(expr)));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //TODO, in progress
        //mutable ::= 'VAR'  identifier           ':' identifier        ('=' expression)?

        String typeName = null;
        match("VAR");

        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("MISSING IDENTIFIER", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        String name = tokens.get(-1).getLiteral();

        if (!match(":"))
            throw new ParseException("Expected colon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        else if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        typeName = tokens.get(-1).getLiteral();

        if(match("=")) {
            Ast.Expression expr = parseExpression();
            return new Ast.Global(name, typeName, true, Optional.of(expr));
        }

        return new Ast.Global(name, typeName, true, Optional.empty());
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        //TODO, in progress
        //immutable ::= 'VAL' identifier       ':' identifier        '=' expression

        String typeName = null;

        match("VAL");
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        String name = tokens.get(-1).getLiteral();

        if (!match(":"))
            throw new ParseException("Expected colon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        else if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        typeName = tokens.get(-1).getLiteral();

        if(!match("="))
            throw new ParseException("Expected Operator '='", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        Ast.Expression expr = parseExpression();

        return new Ast.Global(name, typeName, false, Optional.of(expr));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //TODO, in progress
        //function ::= 'FUN' identifier '(' (identifier       ':' identifier        (',' identifier       ':' identifier        )* )? ')' (        ':' identifier)?        'DO'
        List<String> parameterList = new ArrayList<String>();
        List<String> parameterTypeNames = new ArrayList<String>();
        Optional<String> returnTypeName = Optional.empty();

        match("FUN");
        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        String functionName = tokens.get(-1).getLiteral();

        if(!match("("))
            throw new ParseException("Expected opening parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        if(match(Token.Type.IDENTIFIER)) {
            parameterList.add(tokens.get(-1).getLiteral());

            if (!match(":"))
                throw new ParseException("Expected colon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            else if (!match(Token.Type.IDENTIFIER))
                throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

            parameterTypeNames.add(tokens.get(-1).getLiteral());
            while (match(",")) {
                if (!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

                parameterList.add(tokens.get(-1).getLiteral());

                if (!match(":"))
                    throw new ParseException("Expected colon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                else if (!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

                parameterTypeNames.add(tokens.get(-1).getLiteral());
            }
        }
            if(!match(")"))
                throw new ParseException("Expected ')'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

            else if(match(":")) {
                if(!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Expected Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

                returnTypeName = Optional.ofNullable(tokens.get(-1).getLiteral());
            }

        if(!match("DO"))
            throw new ParseException("Expected 'DO'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        List<Ast.Statement> block = parseBlock();

        return new Ast.Function(functionName, parameterList, parameterTypeNames, returnTypeName, block);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {

            List<Ast.Statement> statements = new ArrayList<>();
                while (!(match("END") || peek("ELSE") || peek("DEFAULT"))) {
                    statements.add(parseStatement());
                }


            //statements.add()
            return statements;

    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            match("LET");
            return parseDeclarationStatement();
        } else if (peek("SWITCH")) {
            match("SWITCH");
            return parseSwitchStatement();
        } else if (peek("IF")) {
            match("IF");
            return parseIfStatement();
        } else if (peek("WHILE")) {
            match("WHILE");
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            match("RETURN");
            return parseReturnStatement();
        } else {
            Ast.Expression expr = parseExpression();
            if (peek("=")) {
                match("=");
                Ast.Expression val = parseExpression();
                if (peek(";")) {
                    match(";");
                    return new Ast.Statement.Assignment(expr, val);
                } else {
                    throw new ParseException("Missing semicolon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            } else {
                if (match(";")) {
                    return new Ast.Statement.Expression(expr);
                } else {
                    throw new ParseException("Missing semicolon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        //TODO, in progress
        //'LET' identifier        (':' identifier)?             ('=' expression)? ';'
        // LET name = expr;
        // LET name;
        // 'LET' identifier ('=' expression)? ';'
        Ast.Expression expr = null;
        Optional<String> typeName = Optional.empty();

        if(!match(Token.Type.IDENTIFIER))
            throw new ParseException("Missing Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        String name = tokens.get(-1).getLiteral();

        //MODIFIED PARSER =============================
        if(match(":"))  {
            if(!match(Token.Type.IDENTIFIER))
                throw new ParseException("Missing Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
            typeName = Optional.ofNullable(tokens.get(-1).getLiteral());
        }
        //=============================================

        if(match("=")){
            expr = parseExpression();
            if(!match(";"))
                throw new ParseException("Missing Declarative Semicolon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            return new Ast.Statement.Declaration(name, Optional.of(expr));
        }

        else if(!match(";"))
            throw new ParseException("Missing Declarative Semicolon", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        return new Ast.Statement.Declaration(name, typeName, Optional.empty());
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // 'IF' expression 'DO' block ('ELSE' block)? 'END' |
        // IF expr DO stmt; END
        List<Ast.Statement> thenStates = new ArrayList<>();
        List<Ast.Statement> elseStates = new ArrayList<>();

        Ast.Expression expr = parseExpression();
        int stringLeng = expr.getClass().toString().length() - 34;
        //tokens.get(0).getLiteral().length();
        try {
            if (!match("DO")) {
                throw new ParseException("Expected DO at: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            } else {
                //match("DO");
                thenStates = parseBlock();
            }
        }
        catch (ArrayIndexOutOfBoundsException e){
            //System.out.print(stringLeng);
            throw new ParseException("Expected Do at: " + (tokens.get(-1).getIndex() + stringLeng), tokens.get(-1).getIndex() + stringLeng);
        }

        if(match("ELSE")){
            elseStates = parseBlock();
        }

        return new Ast.Statement.If(expr, thenStates, elseStates);

    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        //    'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END'
        Ast.Expression expr = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();

        while (peek("CASE")){
            cases.add(parseCaseStatement());
        }
        // default is a case statement without a value
        if(peek("DEFAULT")){
            cases.add(parseCaseStatement());
        }
        else
            throw new ParseException("Expected Default at: " + tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);

        return new Ast.Statement.Switch(expr, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        //('CASE' expression  ':' block)* 'DEFAULT' block 'END'
        List<Ast.Statement> statements = new ArrayList<>();

        if(match("CASE")) {
            Ast.Expression expr = parseExpression();
            if (peek(":")) {
                match(":");
                statements = parseBlock();
            } else
                throw new ParseException("Missing colon at: " + tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length(), tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
            return new Ast.Statement.Case(Optional.of(expr),statements);
        }
        else if(match("DEFAULT")){
            statements = parseBlock();
            return new Ast.Statement.Case(Optional.empty(), statements);
        }
        throw new ParseException("Could not find CASE or DEFAULT", tokens.get(0).getIndex());
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // 'while' expression 'do' block 'end'
        //WHILE expr DO stmt; END
        System.out.println("Found While");
        List<Ast.Statement> states = new ArrayList<>();

        Ast.Expression expr = parseExpression();
        try {
            if (peek("DO")) {
                match("DO");
                System.out.println("Found Do");
                states = parseBlock();
            } else
                throw new ParseException("Expected DO at: " + tokens.get(-1).getIndex(), tokens.get(-1).getIndex());
        }
        catch(ArrayIndexOutOfBoundsException e){
            throw new ParseException("Expected End at: " + (tokens.get(-1).getIndex() + 1), (tokens.get(-1).getIndex() + 1));
        }
        //List<Ast.Statement> statements = new ArrayList<>();


        System.out.println("Found end");
        return new Ast.Statement.While(expr, states);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        //RETURN expr ;
        // 'RETURN' expression ';' |expression ('=' expression)? ';'
        System.out.println("Found return");
        Ast.Expression expr = parseExpression();
        //Ast.Expression extra = parseExpression();

        if(!match(";"))
            throw new ParseException("Missing Semicolon in return" , tokens.get(0).getIndex());
        //System.out.println(toString(Ast.Statement.Return(expr)));
        return new Ast.Statement.Return(expr);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression(); //TODO write parse exception
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException { // logical_expression ::= comparison_expression (('&&' | '||') comparison_expression)*
        Ast.Expression expr = parseComparisonExpression();

        while(match("&&")||match("||")) {   // Kleene closure: (('&' | '||') comparison_expression)*
            String operator_logical = tokens.get(-1).getLiteral();  // Gets && or || from token
            Ast.Expression right = parseComparisonExpression();     // Gets the expression to the right of the operator
            expr = new Ast.Expression.Binary(operator_logical, expr, right);    // Creates binary expression combining previous parts
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException { // additive_expression (('<' | '>' | '==' | '!=') additive_expression)*
        Ast.Expression expr = parseAdditiveExpression();

        while(match("<") || match(">") || match("==") || match("!=")) { //(('<' | '>' | '==' | '!=') additive_expression)*
            String operator_comparison = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            expr = new Ast.Expression.Binary(operator_comparison, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException { // multiplicative_expression (('+' | '-') multiplicative_expression)*
        Ast.Expression expr = parseMultiplicativeExpression();

        while (match("+") || match("-")) { // (('+' | '-') multiplicative_expression)*
            String operator_additive = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            expr = new Ast.Expression.Binary(operator_additive,expr,right);
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException { // primary_expression (('*' | '/' | '^') primary_expression)*
        Ast.Expression expr = parsePrimaryExpression();

        while(match("*") || match("/") || match("^")) { // (('*' | '/' | '^') primary_expression)*
            String operator_multiplicative = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            expr = new Ast.Expression.Binary(operator_multiplicative, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) return new Ast.Expression.Literal(null);
        else if (match("TRUE")) return new Ast.Expression.Literal(true);
        else if (match("FALSE")) return new Ast.Expression.Literal(false);
        else if (match(Token.Type.INTEGER)) return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        else if (match(Token.Type.DECIMAL)) return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        else if (match(Token.Type.CHARACTER)) {
            String temp = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral().charAt(1));
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String temp = tokens.get(-1).getLiteral();
            //function call expression
            List<Ast.Expression> expr = new ArrayList<Ast.Expression>();
            if(match("(")){
                while(!match(")")){
                    if(peek(",")) {
                        match(",");
                        if (peek(")")) {
                            throw new ParseException("Trailing Comma at: " + (tokens.get(-1).getIndex()), tokens.get(-1).getIndex());
                        }
                    }
                    expr.add(parseExpression());
                }
                return new Ast.Expression.Function(temp, expr);
            }
            else if(match("[")) {
                Ast.Expression.Access offset = (Ast.Expression.Access) parseExpression();
                Ast.Expression.Access access = new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), offset.getName())), temp);
                match("]");
                return access;
            }
            return new Ast.Expression.Access(Optional.empty(), temp);
        }
        else if(match(Token.Type.STRING)){
            String temp = tokens.get(-1).getLiteral();
            temp = temp.substring(1, temp.length()-1);
            // needs all escape char
            if(temp.contains("\\")){
                temp = temp.replace("\\n", "\n")
                        .replace("\\b", "\b");
            }
            return new Ast.Expression.Literal(temp);
        }
        else if (match("(")) {
            Ast.Expression expr = parseExpression();    //Inner Expression
            if(match(")"))
                return new Ast.Expression.Group(expr);
            else {
                String temp = tokens.get(-1).getLiteral();
                throw new ParseException("No closing parenthesis: " + (tokens.get(-1).getIndex()), tokens.get(-1).getIndex());
            }
        }
        throw new ParseException("Invalid Expression", tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if(!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}