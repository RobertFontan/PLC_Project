package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args -> {
            if( !( args.get(0).getValue() instanceof BigDecimal ) ) {
                throw new RuntimeException("Expected a BigDecimal, received " +
                        args.get(0).getValue().getClass().getName() + ".");
            }

            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();

            BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));

            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));

            return Environment.create(result);


        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional optional = ast.getValue();
        Boolean present = optional.isPresent();
        if( present ) {
            Ast.Expression expr = (Ast.Expression) optional.get();

            scope.defineVariable(ast.getName(), true, visit(expr));
        }
        else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return scope.lookupVariable(ast.getName()).getValue();
    } //TODO

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        } return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        if(Objects.equals(ast.getOperator(), "&&")) return Environment.create(left.getValue().equals(right.getValue())); //TODO: INCORRECT IMPLEMENTATION OF &&

        if(Objects.equals(ast.getOperator(),"||")) {    //TODO: IMPLEMENT ||
            throw new UnsupportedOperationException();
        }

        if(Objects.equals(ast.getOperator(), "<") || Objects.equals(ast.getOperator(), ">") || Objects.equals(ast.getOperator(), "<=") || Objects.equals(ast.getOperator(), ">=")) {
            if(left.getValue() instanceof BigInteger) {
                if(!(right.getValue() instanceof BigInteger))
                    throw new ArithmeticException("Non matching types");

                BigInteger one = (BigInteger) left.getValue();
                BigInteger two = (BigInteger) right.getValue();

                switch (ast.getOperator()) {
                    case "<":
                        if (one.compareTo(two) == -1) return Environment.create(true);
                        else return Environment.create(false);
                    case ">":
                        if (one.compareTo(two) == 1) return Environment.create(true);
                        else return Environment.create(false);
                    case "<=":
                        if (one.compareTo(two) == 0 || one.compareTo(two) == -1) return Environment.create(true);
                        else return Environment.create(false);
                    case">=":
                        System.out.println(one.compareTo(two));
                        System.out.println(one + "\t" + two);
                        if (one.compareTo(two) == 0 || one.compareTo(two) == 1) return Environment.create(true);
                        else return Environment.create(false);
                }
            }
            else if(left.getValue() instanceof BigDecimal) {
                if(!(right.getValue() instanceof BigDecimal))
                    throw new ArithmeticException("Non matching types");

                BigDecimal one = (BigDecimal) left.getValue();
                BigDecimal two = (BigDecimal) right.getValue();

                if (one.compareTo(two) == -1) return Environment.create(true);
                else return Environment.create(false);
            }
            throw new ArithmeticException("Non valid types");
        }

        if(Objects.equals(ast.getOperator(), "+")) {
            if(left.getValue() instanceof String) {
                if(!(right.getValue() instanceof String))
                    throw new ArithmeticException("Not matching types");

                String one = (String) left.getValue();
                String two = (String) right.getValue();

                return Environment.create(one + two);
            }
            else if(left.getValue() instanceof BigInteger) {
                if(!(right.getValue() instanceof BigInteger))
                    throw new ArithmeticException("Not matching types");

                BigInteger one = (BigInteger) left.getValue();
                BigInteger two = (BigInteger) right.getValue();

                return Environment.create(one.add(two));
            }
            else if(left.getValue() instanceof BigDecimal) {
                if(!(right.getValue() instanceof BigDecimal))
                    throw new ArithmeticException("Not matching types");

                BigDecimal one = (BigDecimal) left.getValue();
                BigDecimal two = (BigDecimal) right.getValue();

                return Environment.create(one.add(two));
            }
            throw new ArithmeticException("Not using valid types");
        }

        if(Objects.equals(ast.getOperator(), "/")) {
            if(left.getValue() instanceof BigDecimal) {
                if(!(right.getValue() instanceof BigDecimal))
                    throw new ArithmeticException("Divisor and Dividend types don't match.");

                BigDecimal one = (BigDecimal) left.getValue();  // Converting each object to bigDecimal for division
                BigDecimal two = (BigDecimal) right.getValue();

                return Environment.create(one.divide(two, RoundingMode.HALF_EVEN)); // Returning division of both sides with rounding
            }
            else {
                if(!(right.getValue() instanceof BigInteger))
                    throw new ArithmeticException("Divisor and Dividend types don't match.");

                BigInteger one = (BigInteger) left.getValue();  // Converting each object to bigInteger for division
                BigInteger two = (BigInteger) right.getValue();

                if(two.equals(BigInteger.ZERO))
                    throw new ArithmeticException("Error: Division by zero.");

                return Environment.create(one.divide(two)); // Returning division of both sides with rounding
            }


        }

        if(Objects.equals(ast.getOperator(), "==")) {
            return Environment.create(left.getValue().equals(right.getValue()));
        }

        if(Objects.equals(ast.getOperator(), "!=")) {
            return Environment.create(!(left.getValue().equals(right.getValue())));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject temp;
        if (ast.getOffset().isPresent())
            return visit(ast.getOffset().get());

        return scope.lookupVariable(ast.getName()).getValue();

        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        List<Environment.PlcObject> arguments = new ArrayList<>();
        scope = new Scope(scope);
        for(int i = 0; i < ast.getArguments().size() ;i++){
            arguments.add(visit(ast.getArguments().get(i)));
        }

        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        return function.invoke(arguments);

        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //returns the list as a plcObject
       // List<Environment.PlcObject> list = new ArrayList<>();
        List<Ast.Expression> plcList = ast.getValues();
        for(int i = 0; i < ast.getValues().size() ;i++){
           plcList.add(ast.getValues().get(i));
        }
/*
        Environment.PlcObject list
        return list;
*/


       throw new UnsupportedOperationException(); //TODO

    }

    /*
      Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
