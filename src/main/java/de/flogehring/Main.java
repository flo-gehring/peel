package de.flogehring;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.parse.PeelGrammar;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and Welcome to peel 👌 🍌!");

        Program p = new Program(new Expression.Block(List.of(
                ExpressionFactoryMethods.assign("x", ExpressionFactoryMethods.integer(1)),
                ExpressionFactoryMethods.assign("y", ExpressionFactoryMethods.integer(1)),
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        )));
        Runtime runtime = RuntimeFactory.defaultLanguage();
        System.out.println(runtime.run(p));
        Program parse = PeelGrammar.parse("""
                 a = 1 + 2
                 b = 5 + 2
                 a + b
                """);
        System.out.println(runtime.run(parse));
    }
}
