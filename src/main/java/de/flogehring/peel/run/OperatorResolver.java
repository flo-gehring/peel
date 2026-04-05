package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.AmbiguousOperatorException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;

import java.util.*;

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
        int bestScore = matchingCandidates.stream()
                .mapToInt(candidate -> typeDistance(argument.getClass(), candidate.lhsType()))
                .max()
                .orElseThrow();
        return matchingCandidates.stream()
                .filter(candidate -> typeDistance(argument.getClass(), candidate.lhsType()) == bestScore)
                .sorted(Comparator.comparing(def -> def.lhsType().getName()))
                .toList();
    }

    private List<OperatorDef> getBestMatches(PeelValue lhs, PeelValue rhs, List<OperatorDef> matchingCandidates) {
        int bestScore = matchingCandidates.stream()
                .mapToInt(candidate -> score(candidate, lhs, rhs))
                .max()
                .orElseThrow();
        return matchingCandidates.stream()
                .filter(candidate -> score(candidate, lhs, rhs) == bestScore)
                .sorted(Comparator.comparing(def -> def.lhsType().getName() + "|" + def.rhsType().getName()))
                .toList();
    }

    private int score(OperatorDef candidate, PeelValue lhs, PeelValue rhs) {
        return typeDistance(lhs.getClass(), candidate.lhsType()) + typeDistance(rhs.getClass(), candidate.rhsType());
    }

    private int typeDistance(Class<?> actual, Class<?> declared) {
        if (!declared.isAssignableFrom(actual)) {
            return Integer.MIN_VALUE;
        }
        if (actual == declared) {
            return 10_000;
        }
        int distance = minTypeDistance(actual, declared);
        return 10_000 - distance;
    }

    private int minTypeDistance(Class<?> actual, Class<?> declared) {
        record Node(Class<?> type, int distance) {
        }

        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(new Node(actual, 0));
        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (!visited.add(current.type())) {
                continue;
            }
            if (current.type() == declared) {
                return current.distance();
            }
            Class<?> superClass = current.type().getSuperclass();
            if (superClass != null) {
                queue.add(new Node(superClass, current.distance() + 1));
            }
            for (Class<?> implementedInterface : current.type().getInterfaces()) {
                queue.add(new Node(implementedInterface, current.distance() + 1));
            }
        }
        return Integer.MAX_VALUE / 4;
    }
}
