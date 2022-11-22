package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);

        if(!scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER))
            throw new RuntimeException("not int");

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        String name = ast.getName();
        if(ast.getValue().isPresent()){
            Ast.Expression value = ast.getValue().get();
            if(value instanceof Ast.Expression.PlcList)
                ((Ast.Expression.PlcList) value).setType(Environment.getType(ast.getTypeName()));
            visit(value);
            requireAssignable(Environment.getType(ast.getTypeName()), value.getType());
        }

        scope.defineVariable(name, name, Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        ast.setVariable(scope.lookupVariable(name));
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> TypeList = new ArrayList<>();
        String name = ast.getName();

        for(int i = 0; i < ast.getParameterTypeNames().size(); i++){
            TypeList.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }


        scope.defineFunction(name, name, TypeList, Environment.getType(ast.getReturnTypeName().orElse("NIL")), args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));

        scope = new Scope(scope);

        for (int j= 0; j < ast.getParameters().size(); j++){
            scope.defineVariable(ast.getParameters().get(j), ast.getParameters().get(j), Environment.getType(ast.getParameterTypeNames().get(j)), true, Environment.NIL);
        }

        //function = Environment.getType(ast.getReturnTypeName().orElse("NIL"));
        //Environment.Type functionType = ast.getFunction().getReturnType();
        //functionType = Environment.getType(ast.getReturnTypeName().orElse("NIL"));
        //ast.getStatements().forEach(this::visit);
        //functionType = null;
        //Environment.Type functionType = Environment.getType(ast.getReturnTypeName().orElse("NIL"));
        ast.getStatements().forEach(this::visit);

        //function = null;


        scope = scope.getParent();
        return null;

       // throw new UnsupportedOperationException();  // TODO
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
        visit(receiver);
        visit(value);

        if(!(receiver instanceof Ast.Expression.Access))
            throw new RuntimeException("Not an access expression");

        requireAssignable(receiver.getType(), value.getType());

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
        visit(condition);

        if(cases.get(cases.size() - 1).getValue().isPresent())
            throw new RuntimeException("Default case has a value");

        // Loop through each case statement, checking for exception conditions,
        // visiting the value, and then visiting the case itself:
        for(Ast.Statement.Case cases_ : cases) {
            if(cases_.getValue().isPresent()) {
                visit(cases_.getValue().get());
                requireAssignable(condition.getType(), cases_.getValue().get().getType());
            }

            scope = new Scope(scope);
            visit(cases_);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;

    }


    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        if(!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN)))
            throw new RuntimeException("Condition does not evaluate to boolean");

        List<Ast.Statement> statements = ast.getStatements();
        scope = new Scope(scope);
        for(Ast.Statement stmt : statements) {
            visit(stmt);
        }
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(function.getFunction().getReturnType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) ast.setType(Environment.Type.BOOLEAN);
        else if (ast.getLiteral() instanceof String) ast.setType(Environment.Type.STRING);
        else if (ast.getLiteral() instanceof Character) ast.setType(Environment.Type.CHARACTER);
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
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator().toString();
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        switch (operator) {
            case "&&":
            case "||":
                if (leftType.equals(rightType) && leftType.equals(Environment.Type.BOOLEAN)) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                }
                throw new RuntimeException("Mismatched types");
            case "<":
            case ">":
            case "==":
            case "!=":
                if (leftType.equals(rightType) && (leftType.equals(Environment.Type.INTEGER) || leftType.equals(Environment.Type.DECIMAL)
                        || leftType.equals(Environment.Type.CHARACTER) || leftType.equals(Environment.Type.STRING))) {
                    ast.setType(Environment.Type.COMPARABLE);
                    return null;
                }
                throw new RuntimeException("Mismatched types");
            case "+":

                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if (leftType.equals(Environment.Type.INTEGER)) {
                    if (!(rightType.equals(Environment.Type.INTEGER)))
                        throw new RuntimeException("Mismatched types");
                    ast.setType(Environment.Type.INTEGER);
                } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (!(ast.getRight().getType().equals(Environment.Type.DECIMAL)))
                        throw new RuntimeException("Mismatched types");
                    ast.setType(Environment.Type.DECIMAL);
                }

                return null;
            case "-":
            case "*":
            case "/":
                if (!(leftType.equals(rightType)))
                    throw new RuntimeException("Mismatched types");
                ast.setType(leftType);
                return null;
            case "^":
                if (leftType.equals(rightType) && leftType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                    return null;
                }
                throw new RuntimeException("Mismatched types");
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        if(ast.getOffset().isPresent()) {
            Ast.Expression offset = ast.getOffset().get();
            visit(offset);
            if (!offset.getType().equals(Environment.Type.INTEGER))
                throw new RuntimeException("Not INT");

        }

        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
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


        for (Ast.Expression expression : valueList) {
            visit(expression);
            requireAssignable(ast.getType(), expression.getType());
        }

        return null;
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
