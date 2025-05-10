package de.flogehring;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.lang.CodeElement;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.SimpleRuntime;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and Welcome to peel 👌 🍌!");

        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.literal(1)),
                CodeElement.assign("y", CodeElement.literal(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        System.out.println(runtime.run(p));
        Program parse = PeelGrammar.parse("""
         a = 1 + 2
         b = 5 + 2
         a + b
        """);
        System.out.println(runtime.run(parse));
    }
}