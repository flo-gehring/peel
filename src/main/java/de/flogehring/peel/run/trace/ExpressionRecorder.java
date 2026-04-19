package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.function.Supplier;

public class ExpressionRecorder implements TraceRecorder {

    private TraceRecorder traceRecorder;

    public BlockTraceRecorder recordBlock() {
        return createAndAssign(BlockTraceRecorder::new);
    }

    public ReturnExpressionRecorder recordReturn() {
        return createAndAssign(ReturnExpressionRecorder::new);
    }

    public AssignmentRecorder recordAssignment() {
        return createAndAssign(AssignmentRecorder::new);
    }

    @Override
    public TraceExpression traceExpression() {
        return traceRecorder.traceExpression();
    }

    public BinaryOpRecorder recordBinary() {
        return createAndAssign(BinaryOpRecorder::new);
    }

    public LiteralRecorder literalRecorder() {
        return createAndAssign(LiteralRecorder::new);
    }

    public ListLiteralRecorder recordListLiteral() {
        return createAndAssign(ListLiteralRecorder::new);
    }

    public VariableNameRecorder recordVarName() {
        return createAndAssign(VariableNameRecorder::new);
    }

    public FunctionCallRecorder recordFunctionCall() {
        return createAndAssign(FunctionCallRecorder::new);
    }

    public IfElseRecorder recordElseIf() {
        return createAndAssign(IfElseRecorder::new);
    }

    public UnaryRecorder recordUnary() {
        return createAndAssign(UnaryRecorder::new);
    }

    public ForEachRecorder recordForEachLoop() {
        return createAndAssign(ForEachRecorder::new);
    }

    public MapLiteralRecorder recordMapLiteral() {
        return createAndAssign(MapLiteralRecorder::new);
    }

    public SelectorRecorder recordSelector() {
        return createAndAssign(SelectorRecorder::new);
    }

    public FunctionDeclarationRecorder recordFunctionDeclaration() {
        return createAndAssign(FunctionDeclarationRecorder::new);
    }

    public WhileLoopRecorder recordWhileLoop() {
        return createAndAssign(WhileLoopRecorder::new);
    }

    private <T extends TraceRecorder> T createAndAssign(Supplier<T> creator) {
        T recorder = creator.get();
        traceRecorder = recorder;
        return recorder;
    }
}
