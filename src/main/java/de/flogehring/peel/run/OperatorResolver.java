package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.AmbiguousOperatorException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;

import java.util.Comparator;
import java.util.List;

final class OperatorResolver {

    PeelValue resolveAndApply(String symbol, PeelValue argument, List<OperatorDef> candidates) {
        List<OperatorDef> matchingCandidates = candidates.stream()
                .filter(candidate -> candidate.matches(argument))
                .toList();
        if (matchingCandidates.isEmpty()) {
            throw new NoFunctionFoundException(symbol, argument);
        } else if (matchingCandidates.size() > 1) {
            List<OperatorDef> bestMatches = getBestMatches(argument, matchingCandidates);
            if (bestMatches.size() > 1) {
                throw new AmbiguousOperatorException(symbol, argument, bestMatches);
            }
            return bestMatches.getFirst().apply(argument);
        } else {
            return matchingCandidates.getFirst().apply(argument);
        }
    }

    PeelValue resolveAndApply(String symbol, PeelValue lhs, PeelValue rhs, List<OperatorDef> candidates) {
        List<OperatorDef> matchingCandidates = candidates.stream()
                .filter(candidate -> candidate.matches(lhs, rhs))
                .toList();
        if (matchingCandidates.isEmpty()) {
            throw new NoFunctionFoundException(symbol, lhs, rhs);
        } else if (matchingCandidates.size() > 1) {
            List<OperatorDef> bestMatches = getBestMatches(lhs, rhs, matchingCandidates);
            if (bestMatches.size() > 1) {
                throw new AmbiguousOperatorException(symbol, lhs, rhs, bestMatches);
            }
            return bestMatches.getFirst().apply(lhs, rhs);
        } else {
            return matchingCandidates.getFirst().apply(lhs, rhs);
        }
    }

    private List<OperatorDef> getBestMatches(PeelValue argument, List<OperatorDef> matchingCandidates) {
        PeelValueKind actualKind = PeelValueKind.fromValue(argument);
        int bestScore = matchingCandidates.stream()
                .mapToInt(candidate -> typeDistance(actualKind, PeelValueKind.fromDeclaredType(candidate.lhsType())))
                .max()
                .orElseThrow();
        return matchingCandidates.stream()
                .filter(candidate -> typeDistance(actualKind, PeelValueKind.fromDeclaredType(candidate.lhsType())) == bestScore)
                .sorted(Comparator.comparing(def -> def.lhsType().getName()))
                .toList();
    }

    private List<OperatorDef> getBestMatches(PeelValue lhs, PeelValue rhs, List<OperatorDef> matchingCandidates) {
        PeelValueKind lhsKind = PeelValueKind.fromValue(lhs);
        PeelValueKind rhsKind = PeelValueKind.fromValue(rhs);
        int bestScore = matchingCandidates.stream()
                .mapToInt(candidate -> score(candidate, lhsKind, rhsKind))
                .max()
                .orElseThrow();
        return matchingCandidates.stream()
                .filter(candidate -> score(candidate, lhsKind, rhsKind) == bestScore)
                .sorted(Comparator.comparing(def -> def.lhsType().getName() + "|" + def.rhsType().getName()))
                .toList();
    }

    private int score(OperatorDef candidate, PeelValueKind lhs, PeelValueKind rhs) {
        PeelValueKind declaredLhs = PeelValueKind.fromDeclaredType(candidate.lhsType());
        PeelValueKind declaredRhs = PeelValueKind.fromDeclaredType(candidate.rhsType());
        return typeDistance(lhs, declaredLhs) + typeDistance(rhs, declaredRhs);
    }

    private int typeDistance(PeelValueKind actual, PeelValueKind declared) {
        return declared.distance(actual);
    }
}
