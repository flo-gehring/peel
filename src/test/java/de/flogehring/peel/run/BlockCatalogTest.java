package de.flogehring.peel.run;

import de.flogehring.peel.core.lang.*;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class BlockCatalogTest {
    private BlockCatalog catalog(String source) {
        return PeelGrammar.parseWithBlocks(source).blockCatalog();
    }

    private List<String> reachable(BlockCatalog catalog, String block) {
        return catalog.blocks().stream().filter(item -> item.name().equals(block)).findFirst().orElseThrow().reachableBlocks();
    }

    @Test void analyzesHandBuiltAstAndRejectsDuplicatesWithoutParser() {
        var block = new Expression.Block(List.of(), "a");
        assertThat(BlockCatalog.analyze(new Program(List.of(block))).names()).containsExactly("a");
        assertThatThrownBy(() -> BlockCatalog.analyze(new Program(List.of(block, block))))
                .hasMessageContaining("already declared");
    }

    @Test void retainsConditionalAndAnonymousDescendantsButExcludesSiblings() {
        var catalog = catalog("outer:{for(i in []) round:{{if(False) conditional:{1/0;}}}} sibling:{}");
        assertThat(catalog.names()).containsExactly("outer", "round", "conditional", "sibling");
        assertThat(reachable(catalog, "round")).containsExactly("round", "conditional");
        assertThat(catalog.blocks().get(2).parentName()).isEqualTo("round");
    }

    @Test void includesCalledFunctionsButNotMerelyDeclaredBodies() {
        var catalog = catalog("fun f() function:{child:{}} fun g() unused:{} round:{f();fun local() declared:{}} sibling:{}");
        assertThat(reachable(catalog, "round")).containsExactly("function", "child", "round");
    }

    @Test void recursionTerminatesAndDynamicCallsKeepAllCallableCandidates() {
        var recursive = catalog("fun f() fbody:{g();}fun g() gbody:{f();}round:{f();}");
        assertThat(reachable(recursive, "round")).containsExactly("fbody", "gbody", "round");
        var dynamic = catalog("var f=fun() closure:{};fun g() other:{} round:{f();}sibling:{}");
        assertThat(reachable(dynamic, "round")).containsExactly("closure", "other", "round");
    }

    @Test void functionValuedVariablesAndParametersCannotNarrowToSameNamedDeclaration() {
        var catalog = catalog("fun f() named:{} round:{var f=fun() closure:{};f();}");
        assertThat(reachable(catalog, "round")).contains("named", "closure");
        var parameter = catalog("fun f() named:{} var g=fun() closure:{};fun invoke(f) round:{f();}invoke(g);");
        assertThat(reachable(parameter, "round")).contains("named", "closure");
    }
}
