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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        } else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
            return null;
        } else if (ast.getLiteral() instanceof BigInteger) {
            if(((BigInteger) ast.getLiteral()).bitCount() > 32)
                throw new RuntimeException("BigInteger too large (>32 bits)");
            ast.setType(Environment.Type.INTEGER);
            return null;
        } else if (ast.getLiteral() instanceof BigDecimal) {
            if(((BigDecimal) ast.getLiteral()).doubleValue() == Double.POSITIVE_INFINITY || ((BigDecimal) ast.getLiteral()).doubleValue() == Double.NEGATIVE_INFINITY)
                throw new RuntimeException("BigDecimal is too large (>64 bits)");
            ast.setType(Environment.Type.DECIMAL);
            return null;
        } else {
            ast.setType(Environment.Type.NIL);
            return null;
        }
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        if(!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("NOT BINARY");

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
        //throw new UnsupportedOperationException();  // TODO
        //its being weird
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator().toString();
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();
        visit(ast.getLeft());
        visit(ast.getRight());

        if(operator.toString().equals("&&") || operator.toString().equals("||")) {
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

        //Environment.PlcObject result = null;




        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        /*
        List <Ast.Expression> list = ast.getValues();

        for(int i = 0; ){
            visit(Ast.Expression.get
        }

         */

        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
