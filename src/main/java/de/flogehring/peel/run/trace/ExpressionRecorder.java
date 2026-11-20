package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.run.ExecutionBudget;

import java.util.function.Supplier;

public class ExpressionRecorder implements TraceRecorder {

    private final ExecutionBudget budget;
    private TraceRecorder traceRecorder;

    public ExpressionRecorder() {
        this(ExecutionBudget.unlimited());
    }

    public ExpressionRecorder(ExecutionBudget budget) {
        this.budget = budget;
        budget.trace(1);
    }

    public BlockTraceRecorder recordBlock() {
        return createAndAssign(() -> new BlockTraceRecorder(budget));
    }

    public ReturnExpressionRecorder recordReturn() {
        return createAndAssign(() -> new ReturnExpressionRecorder(budget));
    }

    public AssignmentRecorder recordAssignment() {
        return createAndAssign(() -> new AssignmentRecorder(budget));
    }

    @Override
    public TraceExpression traceExpression() {
        budget.checkpoint();
        return traceRecorder.traceExpression();
    }

    public BinaryOpRecorder recordBinary() {
        return createAndAssign(() -> new BinaryOpRecorder(budget));
    }

    public LiteralRecorder literalRecorder() {
        return createAndAssign(() -> new LiteralRecorder(budget));
    }

    public ListLiteralRecorder recordListLiteral() {
        return createAndAssign(() -> new ListLiteralRecorder(budget));
    }

    public VariableNameRecorder recordVarName() {
        return createAndAssign(() -> new VariableNameRecorder(budget));
    }

    public FunctionCallRecorder recordFunctionCall() {
        return createAndAssign(() -> new FunctionCallRecorder(budget));
    }

    public IfElseRecorder recordElseIf() {
        return createAndAssign(() -> new IfElseRecorder(budget));
    }

    public UnaryRecorder recordUnary() {
        return createAndAssign(() -> new UnaryRecorder(budget));
    }

    public ForEachRecorder recordForEachLoop() {
        return createAndAssign(() -> new ForEachRecorder(budget));
    }

    public MapLiteralRecorder recordMapLiteral() {
        return createAndAssign(() -> new MapLiteralRecorder(budget));
    }

    public SelectorRecorder recordSelector() {
        return createAndAssign(() -> new SelectorRecorder(budget));
    }

    public FunctionDeclarationRecorder recordFunctionDeclaration() {
        return createAndAssign(FunctionDeclarationRecorder::new);
    }

    public WhileLoopRecorder recordWhileLoop() {
        return createAndAssign(() -> new WhileLoopRecorder(budget));
    }

    private <T extends TraceRecorder> T createAndAssign(Supplier<T> creator) {
        T recorder = creator.get();
        traceRecorder = recorder;
        return recorder;
    }
}
