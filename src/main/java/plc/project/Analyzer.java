package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        if(!(ast.getExpression() instanceof Ast.Expression.Function))
            throw new RuntimeException("FUNCTION");

        visit(ast.getExpression());

        return null;
        //throw new UnsupportedOperationException();// TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException();  // TODO

        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration statement must have a type or value.");
        }

        Environment.Type type = null;

        if(ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            if(type == null)
                type = ast.getValue().get().getType();

            requireAssignable(type, ast.getValue().get().getType());
        }
        ast.setVariable(scope.defineVariable(ast.getName(),
                                             ast.getName(),
                                             type,
                                             true,
                                             Environment.NIL)
                );

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Ast.Expression value = ast.getValue();

        if(!(receiver instanceof Ast.Expression.Access))
            throw new RuntimeException("Not an access expression");
        else {
            visit(receiver);
            visit(value);

            if(!(receiver.getType().equals(value.getType())))
                throw new RuntimeException("Assigning incorrect type");
                //TODO: IN PROGRESS, CONTINUE ONCE REQUIREASSIGNABLE() IS FINISHED.
                // THIS IS NEEDED TO ENSURE THAT THE VALUE IS A SUBTYPE OF THE RECEIVER.

        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) { //TODO: Add Scoping
        List<Ast.Statement> elseStmts = ast.getElseStatements();
        List<Ast.Statement> thenStmts = ast.getThenStatements();
        Ast.Expression condition = ast.getCondition();
        visit(condition);

        if(!(condition.getType().equals(Environment.Type.BOOLEAN)))
            throw new RuntimeException("Condition does not evaluate to type boolean");
        else if (thenStmts.isEmpty())
            throw new RuntimeException("Empty statement");
        for(Ast.Statement stmt : thenStmts) {
            System.out.println(".");
            visit(stmt);
        }
        if(!elseStmts.isEmpty()) {
            for (Ast.Statement stmt : elseStmts) {
                scope = new Scope(scope.getParent());
                visit(stmt);
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        Ast.Expression condition = ast.getCondition();
        List<Ast.Statement.Case> cases = ast.getCases();

        for(Ast.Statement.Case cases_ : cases) {

        }


        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        if(!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN)))
            throw new RuntimeException("Condition does not evaluate to boolean");

        List<Ast.Statement> statements = ast.getStatements();
        for(Ast.Statement stmt : statements) {
            scope = new Scope(scope);   //TODO: REALLY NOT SURE IF THIS LINE IS RIGHT
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) ast.setType(Environment.Type.BOOLEAN);
        else if (ast.getLiteral() instanceof String) ast.setType(Environment.Type.STRING);
        else if (ast.getLiteral() instanceof BigInteger) {
            if(((BigInteger) ast.getLiteral()).bitCount() > 32)
                throw new RuntimeException("BigInteger too large (>32 bits)");
            ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            if(((BigDecimal) ast.getLiteral()).doubleValue() == Double.POSITIVE_INFINITY || ((BigDecimal) ast.getLiteral()).doubleValue() == Double.NEGATIVE_INFINITY)
                throw new RuntimeException("BigDecimal is too large (>64 bits)");
            ast.setType(Environment.Type.DECIMAL);
        }
        else if (ast.getLiteral() == null) ast.setType(Environment.Type.NIL);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        if(!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("NOT BINARY");

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator().toString();
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        if(operator.equals("&&") || operator.equals("||")) {
            if(leftType.equals(rightType) && leftType.equals(Environment.Type.BOOLEAN)) {
                ast.setType(Environment.Type.BOOLEAN);
                return null;
            }
            throw new RuntimeException("Mismatched types");
        }

        else if (operator.equals("<") || operator.equals(">") || operator.equals("==") || operator.equals("!=")) {
            if(leftType.equals(rightType) && (leftType.equals(Environment.Type.INTEGER) || leftType.equals(Environment.Type.DECIMAL)
                    || leftType.equals(Environment.Type.CHARACTER) || leftType.equals(Environment.Type.STRING))) {
                ast.setType(Environment.Type.COMPARABLE);
                return null;
            }
                throw new RuntimeException("Mismatched types");
        }

        else if (operator.equals("+")) {

            if(leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            }
            else if (leftType.equals(Environment.Type.INTEGER)) {
                if(!(rightType.equals(Environment.Type.INTEGER)))
                    throw new RuntimeException("Mismatched types");
                ast.setType(Environment.Type.INTEGER);
            }
            else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if(!(ast.getRight().getType().equals(Environment.Type.DECIMAL)))
                    throw new RuntimeException("Mismatched types");
                ast.setType(Environment.Type.DECIMAL);
            }

            return null;
        }

        else if (operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if(!(leftType.equals(rightType)))
                throw new RuntimeException("Mismatched types");
            ast.setType(leftType);
            return null;
        }

        else if (operator.equals("^")) {
            if(leftType.equals(rightType) && leftType.equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
                return null;
            }
            throw new RuntimeException("Mismatched types");
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        Environment.PlcObject result = null;

        if(ast.getOffset().isPresent()) {
            Ast.Expression offset = ast.getOffset().get();
            visit(offset);
            if (!offset.getType().equals(Environment.Type.INTEGER))
                throw new RuntimeException("Not INT");

        }

        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List <Ast.Expression> arguments = ast.getArguments();
        //scope.lookupVariable(name, arguments.size());
        ast.setFunction(scope.lookupFunction(name, arguments.size()));
        for(int i =0; i < arguments.size(); i++){
            visit(arguments.get(i));
            // List<Environment.Type> parametersType = ast.getFunction().getParameterTypes();
            requireAssignable(ast.getFunction().getParameterTypes().get(i), arguments.get(i).getType());
        }
        return null;
        //scope.defineFunction(name, name, args, )


        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {


        List <Ast.Expression> valueList = ast.getValues();


        for(int i = 0; i < valueList.size(); i++){
            visit(valueList.get(i));
            requireAssignable(ast.getType(), valueList.get(i).getType());
        }

        return null;



        //throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(!target.equals(Environment.Type.ANY)) {  // IF TYPE IS ANY, IT WILL MATCH ON ANY TYPE, SO THIS ONLY CHECKS FOR OTHER TYPES

            if(target.equals(Environment.Type.COMPARABLE)) { //Integer, Decimal, Character, and String
                if(!(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL)
                        || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)))
                            throw new RuntimeException("Type is not Comparable");
            }
            else if(!(target.equals(type))) //EVERY OTHER TYPE SHOULD MATCH, OTHERWISE EXCEPTION IS THROWN
                throw new RuntimeException("Mismatched Types");
        }

    }

}
