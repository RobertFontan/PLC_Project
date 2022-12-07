package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast)
                visit((Ast) object);
            else
                writer.write(object.toString());
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++)
            writer.write("    ");

    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(indent);
        indent++;
        newline(indent);
        for (Ast.Global global : ast.getGlobals()) {
            print(global);
            newline(indent);
        }
        print("public static void main(String[] args) {");
        newline(indent + 1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");

        for (Ast.Function function : ast.getFunctions()) {
            indent--;
            newline(indent);
            indent++;
            newline(indent);
            print(function);
        }
        indent--;
        newline(indent);
        newline(indent);
        print("}");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        String typeName = Environment.getType(ast.getTypeName()).getJvmName();
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList)
            print(typeName, "[] ", ast.getName());
        else if (ast.getMutable())
            print(typeName, " ", ast.getName());
        else
            print("final ", typeName, " ", ast.getName());


        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());

        print(";");

        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName() + " ");
            print(ast.getParameters().get(i));
            if (i != ast.getParameters().size() - 1)
                print(", ");
        }
        print(") {");
        if (ast.getStatements().size() == 0)
            print("}");
        else {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                visit(ast.getStatements().get(i));
                //print(";");
            }
            print(";");
            indent--;
            newline(indent);
            print("}");
        }

        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        print(ast.getVariable().getType().getJvmName() + " " + ast.getName());

        if(ast.getValue().isPresent()){
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");

        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");

        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        if(!ast.getThenStatements().isEmpty()) {
            for(Ast.Statement statements : ast.getThenStatements()) {
                newline(1);
                visit(statements);
            }
            newline(0);
            print("}");
        }
        if(!ast.getElseStatements().isEmpty()) {
            print(" else {");
            for(Ast.Statement statements : ast.getElseStatements()) {
                newline(1);
                visit(statements);
            }
            newline(0);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch ");
        print("(");
        visit(ast.getCondition());
        print(") {");

        indent++;
        for (Ast.Statement.Case cases : ast.getCases()){
            newline(indent);
            visit(cases);
        }
        newline(0);
        print("}");



        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                visit(ast.getStatements().get(i));
            }
            newline(indent);
            print("break;");

            indent--;
        }
        else {
            print("default:");
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                visit(ast.getStatements().get(i));
            }
            //newline(indent);
           // print("break;");

            indent--;
        }


        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral().equals(true))
            print("true");
        else if(ast.getLiteral().equals(false))
            print("false");
        else if(ast.getType().equals(Environment.Type.STRING)) {
            String literal = "\"" + ast.getLiteral().toString() + "\"";
            print(literal);
        }
        else if(ast.getType().equals(Environment.Type.CHARACTER)) {
            String literal = "'" + ast.getLiteral().toString() + "'";
            print(literal);
        }
        else if(ast.getType().equals(Environment.Type.INTEGER)) {//TODO USE BIG INTEGER CLASS
            String literal = ast.getLiteral().toString();
            print(literal);
        }
        else if(ast.getType().equals(Environment.Type.DECIMAL)) {//TODO USE BIG DECIMAL CLASS FOR PRECISION
            String literal = ast.getLiteral().toString();
            print(literal);
        }
        else if(ast.getType().equals(Environment.Type.NIL))
            print("null");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        print(" ");
        print(ast.getOperator().toString());
        print(" ");
        visit(ast.getRight());
        System.out.println("LEFT: " + ast.getLeft().toString()+ "RIGHT: " + ast.getRight().toString() + "OPERATOR: " + ast.getOperator().toString());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()) {   // LIST ACCESS
            String listAccess = ast.getVariable().getJvmName() + "[" + ast.getOffset().toString() + "]";
            print(listAccess);
        }
        else {  // VARIABLE ACCESS
            print(ast.getVariable().getJvmName());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        String name = ast.getFunction().getJvmName();
        List<Ast.Expression> arguments = ast.getArguments();
        print(name);
        print("(");

        for(Ast.Expression argument : arguments) {
            visit(argument);
            if(arguments.size() != 1)
                print(", ");
        }
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");

        for (int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i));
            if (i != ast.getValues().size() - 1) {
                print(", ");
            }
        }

        print("}");
        return null;
    }

}
