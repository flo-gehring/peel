package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.trace.*;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class VariableCaptureTest {
    record Example(String name, String source, String block, String variable, List<String> expected) {
        @Override public String toString() { return name; }
    }
    static Example e(String name, String source, String block, String variable, String... expected) {
        return new Example(name, source, block, variable, List.of(expected));
    }
    static Stream<Example> examples() {
        return Stream.of(
            e("K01 last assignment", "var a=1; var b=9; teil:{a=2;a=3;}", "teil","a","3"),
            e("K02 shadow", "var wert=10; abschnitt:{var wert=20; detail:{var wert=30;wert=31;}}","abschnitt","wert","20"),
            e("K03 outer write", "var x=1; a:{b:{x=7;}}","a","x","7"),
            e("K04 while snapshots", "var jahr=0; while (!(jahr==3)) runde:{var lokal=jahr+1;jahr=lokal;lokal=3;} jahr=99;","runde","jahr","1","2","3"),
            e("K05 conditional shadow", "var zins=2;for(jahr in [1,2]) runde:{if(jahr==1) sonderfall:{var zins=5;} zins=zins+1;}","runde","zins","3","4"),
            e("K06 foreach assignment", "for(jahr in [1,2]) runde:{jahr=jahr+10;}","runde","jahr","11","12"),
            e("K07 parameters", "fun f(x) funktion:{x=x+1;x;} var x=100;for(jahr in [1,2]){f(jahr);}","funktion","x","2","3"),
            e("K08 return unwinding", "fun f(x) funktion:{inner:{var lokal=7;x=2;return x+lokal;}x=99;} f(1);","funktion","x","2"),
            e("K09 recursion", "fun summe(n) funktion:{var lokal=n*10;if(n==1){return 1;}return n+summe(n-1);}summe(3);","funktion","lokal","30","20","10"),
            e("K10 closure", "var x=7;var f=fun() closure:{x;}; aufrufer:{var x=100;f();}","closure","x","7"),
            e("K11 program root", "var x=1;{var y=2;x=3;}","","x","3"),
            e("K12 skipped", "if(False) nie:{var x=99;}while(False) leer:{var y=99;}for(i in []) auchNie:{i;}","nie","x"),
            e("K13 empty", "var x=4;leer:{}for(i in [1]) wirklichLeer:{}","leer","x","4"),
            e("K14 none", "fun f(){return;}teil:{var n=f();}","teil","n","none"),
            e("K16 globals shadow", "teil:{var eingabe=20;}","teil","eingabe","20"),
            e("K18 unreached", "fun f(x) teil:{return x;var spaeter=99;} f(2);","teil","spaeter","missing"),
            e("K20 later declaration", "fun f() funktion:{x;}var x=100;f();","funktion","x","7")
        );
    }
    @ParameterizedTest(name="{0}") @MethodSource("examples")
    void documentsCaptureSemantics(Example example) {
        var bindings = RequestBindings.builder().put("x",PeelValue.integer(7)).put("eingabe",PeelValue.integer(10)).build();
        var trace = RuntimeFactory.defaultLanguage().run(PeelGrammar.parse(example.source()), bindings);
        Map<String,Object> mapped = TraceMapOutput.fromProgram(trace);
        List<Map<String,Object>> blocks = new ArrayList<>();
        if(example.block().isEmpty()) blocks.add(mapped);
        else collect(mapped,example.block(),blocks);
        assertThat(blocks.stream().map(block -> value(block,example.variable())).toList()).containsExactlyElementsOf(example.expected());
        switch (example.name().substring(0,3)) {
            case "K01" -> {
                assertThat(value(blocks.getFirst(),"b")).isEqualTo("9");
                assertThat(value(mapped,"a")).isEqualTo("3");
                assertThat(value(mapped,"b")).isEqualTo("9");
            }
            case "K02" -> {
                assertThat(value(mapped,"wert")).isEqualTo("10");
                assertThat(value(blocks(mapped,"detail").getFirst(),"wert")).isEqualTo("31");
                assertThat(((Map<?,?>)blocks.getFirst().get("value")).get("value")).isEqualTo(31);
            }
            case "K03" -> assertThat(value(mapped,"x")).isEqualTo("7");
            case "K04" -> {
                assertThat(value(mapped,"jahr")).isEqualTo("99");
                assertThat(value(mapped,"lokal")).isEqualTo("missing");
                assertThat(blocks.stream().map(block->value(block,"lokal"))).containsExactly("3","3","3");
            }
            case "K05" -> {
                assertThat(value(mapped,"zins")).isEqualTo("4");
                assertThat(value(mapped,"jahr")).isEqualTo("missing");
                assertThat(value(blocks(mapped,"sonderfall").getFirst(),"zins")).isEqualTo("5");
            }
            case "K06" -> {
                assertThat(value(mapped,"jahr")).isEqualTo("missing");
                var loop=(TraceExpression.ForEachLoop)trace.expressions().getFirst();
                assertThat(loop.iterations().stream().map(i->i.binding().value())).containsExactly(new TraceValue.IntegerValue(1),new TraceValue.IntegerValue(2));
            }
            case "K07" -> {
                assertThat(value(mapped,"x")).isEqualTo("100");
                assertThat(blocks).allSatisfy(block->assertThat(value(block,"jahr")).isEqualTo("missing"));
            }
            case "K08" -> {
                assertThat(value(blocks.getFirst(),"lokal")).isEqualTo("missing");
                assertThat(value(blocks(mapped,"inner").getFirst(),"lokal")).isEqualTo("7");
                assertThat(trace.result()).isEqualTo(new TraceValue.IntegerValue(9));
            }
            case "K09" -> assertThat(blocks.stream().map(block->value(block,"n"))).containsExactly("3","2","1");
            case "K10" -> assertThat(value(blocks(mapped,"aufrufer").getFirst(),"x")).isEqualTo("100");
            case "K11" -> {
                assertThat(value(mapped,"y")).isEqualTo("missing");
                assertThat(((TraceExpression.Block)trace.expressions().get(1)).variablesAtExit()).containsEntry("y",new TraceValue.IntegerValue(2));
            }
            case "K12" -> {
                assertThat(blocks(mapped,"leer")).isEmpty();
                assertThat(blocks(mapped,"auchNie")).isEmpty();
                assertThat(((TraceExpression.WhileLoop)trace.expressions().get(1)).iterations().getLast().body()).isNull();
            }
            case "K14" -> assertThat(value(blocks.getFirst(),"unbekannt")).isEqualTo("missing");
            case "K13" -> {
                assertThat(value(blocks(mapped,"wirklichLeer").getFirst(),"i")).isEqualTo("1");
                assertThat(value(mapped,"i")).isEqualTo("missing");
            }
            case "K16" -> assertThat(value(mapped,"eingabe")).isEqualTo("10");
            case "K20" -> {
                assertThat(trace.result()).isEqualTo(new TraceValue.IntegerValue(7));
                assertThat(value(mapped,"x")).isEqualTo("100");
            }
            default -> { }
        }
    }
    static List<Map<String,Object>> blocks(Object trace,String name) {
        List<Map<String,Object>> result=new ArrayList<>();collect(trace,name,result);return result;
    }
    @SuppressWarnings("unchecked")
    static String value(Map<String,Object> block,String name) {
        var variables=(Map<String,Object>)block.get("variablesAtExit");
        if(!variables.containsKey(name))return "missing";
        var value=(Map<String,Object>)variables.get(name);
        return "none".equals(value.get("type"))?"none":String.valueOf(value.get("value"));
    }
    @SuppressWarnings("unchecked")
    static void collect(Object object,String name,List<Map<String,Object>> result) {
        if(object instanceof Map<?,?> map) {
            if("block".equals(map.get("type")) && name.equals(map.get("name")))result.add((Map<String,Object>)map);
            map.forEach((key,item)->{if(!key.equals("variablesAtExit"))collect(item,name,result);});
        } else if(object instanceof List<?> list) list.forEach(item->collect(item,name,result));
    }
    @Test void K15_freezesCollectionsAndMaps() {
        var list=new ArrayList<PeelValue>(); list.add(PeelValue.integer(1));
        var trace=RuntimeFactory.defaultLanguage().run(PeelGrammar.parse("teil:{daten;}"),
            RequestBindings.builder().put("daten",PeelValue.list(list)).build());
        list.add(PeelValue.integer(2));
        var value=(TraceValue.ListValue)trace.variablesAtExit().get("daten");
        assertThat(value.items()).hasSize(1);
        assertThatThrownBy(()->trace.variablesAtExit().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(()->value.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        var nested=new LinkedHashMap<de.flogehring.peel.core.values.Primitives,PeelValue>();
        nested.put(PeelValue.text("items"),PeelValue.list(list));
        var second=RuntimeFactory.defaultLanguage().run(PeelGrammar.parse("teil:{daten;}"),
                RequestBindings.builder().put("daten",PeelValue.Collection.peelMap(nested)).build());
        list.clear();nested.clear();
        var captured=(TraceValue.MapValue)second.variablesAtExit().get("daten");
        assertThat(((TraceValue.ListValue)captured.entries().getFirst().value()).items()).hasSize(2);
        assertThatThrownBy(()->captured.entries().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void K17_serializesValuesAndCallableWithoutEnvironment() {
        var trace=RuntimeFactory.defaultLanguage().run(PeelGrammar.parse("var f=fun(){1;};var m={\"a\":[1,2]};var b=True;var d=1.25;var t=\"text\";"),
                RequestBindings.builder().put("floating",new de.flogehring.peel.core.values.Number.Float(1.5f)).build());
        var json=de.flogehring.peel.convenience.output.TraceOutput.asJson(trace);
        assertThat(json).contains("variablesAtExit","callable_ref","decimal","float","text","bool","map","list").doesNotContain("EvaluationEnvironment");
        assertThat(trace.variablesAtExit().get("floating")).isEqualTo(new TraceValue.FloatValue(1.5f));
    }
    @Test void K19_independentRunsAfterErrors() {
        var runtime=RuntimeFactory.defaultLanguage();
        var program=PeelGrammar.parse("var x=eingabe;");
        var first=runtime.run(program,RequestBindings.builder().put("eingabe",PeelValue.integer(1)).build());
        assertThatThrownBy(()->runtime.run(PeelGrammar.parse("a:{var x=[][0];}"))).isInstanceOf(RuntimeException.class);
        var second=runtime.run(program,RequestBindings.builder().put("eingabe",PeelValue.integer(2)).build());
        assertThat(first.variablesAtExit().get("x")).isEqualTo(new TraceValue.IntegerValue(1));
        assertThat(second.variablesAtExit().get("x")).isEqualTo(new TraceValue.IntegerValue(2));
        var scopes=new ArrayList<Scope>();
        var environment=new EvaluationEnvironment(Scope.empty(),Optional.empty(),scopes);
        var evaluator=new Evaluator(environment);
        assertThatThrownBy(()->evaluator.evaluate(PeelGrammar.parse("a:{var x=[][0];}").programm(),new de.flogehring.peel.run.trace.ExpressionRecorder())).isInstanceOf(RuntimeException.class);
        assertThat(scopes).isEmpty();
        assertThatThrownBy(()->evaluator.evaluate(PeelGrammar.parse("fun f(){var x=[][0];} f();").programm(),new de.flogehring.peel.run.trace.ExpressionRecorder())).isInstanceOf(RuntimeException.class);
        assertThat(evaluator).extracting("environment").isSameAs(environment);
        assertThat(scopes).isEmpty();
        evaluator.evaluate(PeelGrammar.parse("fun f(){while(True){return 7;}}f();").programm(),new de.flogehring.peel.run.trace.ExpressionRecorder());
        assertThat(scopes).isEmpty();
    }
}
