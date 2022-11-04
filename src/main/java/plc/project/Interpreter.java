package plc.project;

import javax.swing.*;
import javax.swing.text.html.Option;
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
        scope.defineFunction("converter", 2, args -> {
            String number = new String();
            int i, n = 0;
            ArrayList<BigInteger> quotients = new ArrayList<BigInteger>();
            ArrayList<BigInteger> remainders = new ArrayList<BigInteger>();

            BigInteger base10 = requireType(
                                        BigInteger.class,
                                        Environment.create(args.get(0).getValue())
            );

            BigInteger base = requireType(
                                    BigInteger.class,
                                    Environment.create(args.get(1).getValue())
            );

            quotients.add(base10);

            do {
                quotients.add(quotients.get(n).divide(base));
                remainders.add(quotients.get(n).subtract((quotients.get(n+1).multiply(base))));
                n++;
            } while ( quotients.get( n ).compareTo( BigInteger.ZERO ) > 0);

            for(i = 0; i < remainders.size(); i++) {
                number = remainders.get(i).toString() + number;
            }
            return Environment.create(number);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> globList = ast.getGlobals();
        List<Ast.Function> funcList = ast.getFunctions();

        for(int i =0; i < globList.size(); i++)
            visit(ast.getGlobals().get(i));


        for(int i = 0; i < funcList.size(); i++)
            visit(ast.getFunctions().get(i));


        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if(!ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        else
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args ->{
            scope = new Scope(scope);
            for(int i = 0; i < ast.getParameters().size(); i++)
                scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            try {
                for (int i = 0; i < ast.getStatements().size(); i++)
                    visit(ast.getStatements().get(i));
            }
            catch (Return val) {
                return val.value;
            }

            return Environment.NIL;

        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
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
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if(ast.getReceiver() instanceof Ast.Expression.Access) {    //TODO Still need to handle an offset: list[5]

            Ast.Expression.Access acc = (Ast.Expression.Access) ast.getReceiver();
            String name = acc.getName();

            if(!(acc.getOffset().isPresent()))
                scope.lookupVariable(name).setValue(visit(ast.getValue()));
            else {
                requireType(List.class, scope.lookupVariable(name).getValue());
                int offset = ((BigInteger) visit(acc.getOffset().get()).getValue()).intValue();
                ((List<Object>) scope.lookupVariable(name).getValue().getValue()).set(offset, visit(ast.getValue()).getValue());
            }

            return Environment.NIL;
        }
        else
            throw new ArithmeticException("Receiver is not of type: Ast.Expression.Access");


    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                if (visit(ast.getCondition()).getValue().equals(true)) {
                    List<Ast.Statement> thenStatements = ast.getThenStatements();
                    for (Ast.Statement thenStatement : thenStatements) {
                        visit(thenStatement);
                    }
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else if (!requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                List<Ast.Statement> elseStatements = ast.getElseStatements();
                for (Ast.Statement elseStatement : elseStatements) {
                    visit(elseStatement);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        scope = new Scope(scope);
        List<Ast.Statement.Case> caseStatement = ast.getCases();

        try{
            Environment.PlcObject expr = visit(ast.getCondition());
            for(int i =0; i < caseStatement.size(); i++){
                if(caseStatement.get(i).getValue().isPresent()){
                    Environment.PlcObject caseValue = visit(caseStatement.get(i).getValue().get());
                    if(expr.getValue().equals(caseValue.getValue())){
                        visit(caseStatement.get(i));
                        break;
                    }
                }
                else
                    visit(caseStatement.get(i));
            }
        }
        finally {
            scope = scope.getParent();

        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        ast.getStatements().forEach(this::visit);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while( requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally {
                scope =scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        } return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        if(Objects.equals(ast.getOperator(), "&&")) {
            if(left.getValue() instanceof Boolean) {
                if(!(right.getValue() instanceof Boolean)) {
                    throw new ArithmeticException("Both types must be boolean");
                }

                if((Boolean)left.getValue() == (Boolean)right.getValue()) {
                    return Environment.create(true);
                }
                else return Environment.create(false);
            }
            throw new ArithmeticException("Left is not a boolean");
        }

        if(Objects.equals(ast.getOperator(),"||")) {    //TODO FIX UNDEFINED CASE
            if(left.getValue() instanceof Boolean) {
                if((Boolean) left.getValue())
                    return Environment.create((Boolean) left.getValue());
                else if (right.getValue() instanceof Boolean) {
                    return Environment.create((Boolean) right.getValue());
                }
            }
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

        if(Objects.equals(ast.getOperator(), "/") || Objects.equals(ast.getOperator(), "-") || Objects.equals(ast.getOperator(), "*")) {
            if(left.getValue() instanceof BigDecimal) {
                if(!(right.getValue() instanceof BigDecimal))
                    throw new ArithmeticException("Divisor and Dividend types don't match.");

                BigDecimal one = (BigDecimal) left.getValue();  // Converting each object to bigDecimal for division
                BigDecimal two = (BigDecimal) right.getValue();
                switch (ast.getOperator()) {
                    case "/":
                        return Environment.create(one.divide(two, RoundingMode.HALF_EVEN)); // Returning division of both sides with rounding
                    case "-":
                        return Environment.create(one.subtract(two));
                    case "*":
                        return Environment.create(one.multiply(two));
                }
            }
            else {
                if(!(right.getValue() instanceof BigInteger))
                    throw new ArithmeticException("Divisor and Dividend types don't match.");

                BigInteger one = (BigInteger) left.getValue();  // Converting each object to bigInteger for division
                BigInteger two = (BigInteger) right.getValue();

                if(two.equals(BigInteger.ZERO))
                    throw new ArithmeticException("Error: Division by zero.");

                switch (ast.getOperator()) {
                    case "/":
                        return Environment.create(one.divide(two)); // Returning division of both sides with rounding
                    case "-":
                        return Environment.create(one.subtract(two)); // Returning subtraction of both sides
                    case "*":
                        return Environment.create(one.multiply(two)); // Returning multiplication of both sides
                }
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
        Environment.Variable current = scope.lookupVariable(ast.getName());
        Environment.PlcObject result;
        if (ast.getOffset().isPresent()) { // list has an offset present
            Object offset = ast.getOffset();
            if(offset.getClass().equals(BigInteger.class)) {
                Object currentValue = current.getValue().getValue();
                result = Environment.create(((List<Environment.PlcObject>) currentValue).get(Integer.parseInt(offset.toString())));
                return result;
            }
            else {
                //wtf do i do here :<
                throw new RuntimeException("Not in BigInteger");

            }
        }
        else { //regular variable
            result = Environment.create(current.getValue().getValue());
            return result;
        }
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
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //returns the list as a plcObject

        List<Object> list = new ArrayList<>();

        for(int i =0; i < ast.getValues().size();i++){
            list.add(visit(ast.getValues().get(i)).getValue());
        }
/*
for(Ast.Expression x: ast.getValues()){
            list.add(visit(x).getValue());
        }
 */


        return Environment.create(list);
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
