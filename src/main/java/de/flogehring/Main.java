package de.flogehring;

import de.flogehring.peel.CodeElement;
import de.flogehring.peel.Programm;
import de.flogehring.peel.SimpleRuntime;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and Welcome to peel 👌 🍌!");

        Programm p = new Programm(List.of(
                CodeElement.assign("x", CodeElement.literal(1)),
                CodeElement.assign("y", CodeElement.literal(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        System.out.println(runtime.run(p));
    }
}