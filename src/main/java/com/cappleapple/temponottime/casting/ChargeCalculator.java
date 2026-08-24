package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.TempoNotTime;

public final class ChargeCalculator {
    private record CachedFormula(String expression, ChargeRequirementFormula.Compiled compiled) {
    }

    private static final ChargeRequirementFormula.Compiled DEFAULT_FORMULA =
            ChargeRequirementFormula.compile(ChargeRequirementFormula.DEFAULT_EXPRESSION);
    private static volatile CachedFormula cachedFormula =
            new CachedFormula(ChargeRequirementFormula.DEFAULT_EXPRESSION, DEFAULT_FORMULA);
    private static volatile String lastInvalidEvaluation;

    private ChargeCalculator() {
    }

    public static int maxCharges(double capacity, double cost, int minimum, int maximum, double zeroCostFallback) {
        return maxCharges(capacity, cost, minimum, maximum, zeroCostFallback,
                ChargeRequirementFormula.DEFAULT_EXPRESSION);
    }

    public static int maxCharges(double capacity, double cost, int minimum, int maximum, double zeroCostFallback,
                                 String formulaExpression) {
        int safeMinimum = Math.max(1, minimum);
        int safeMaximum = Math.max(safeMinimum, maximum);
        double safeCapacity = finiteNonNegative(capacity);
        double safeCost = Double.isFinite(cost) && cost > 0.0 ? cost : Math.max(0.0001, finitePositive(zeroCostFallback, 1.0));
        String requestedFormula = formulaExpression == null
                ? ChargeRequirementFormula.DEFAULT_EXPRESSION
                : formulaExpression;

        ChargeRequirementFormula.Compiled formula = resolveFormula(requestedFormula);
        int affordable = calculateAffordable(safeCapacity, safeCost, safeMaximum, formula);
        if (affordable < 0) {
            if (!requestedFormula.equals(lastInvalidEvaluation)) {
                lastInvalidEvaluation = requestedFormula;
                TempoNotTime.LOGGER.warn("Charge requirement formula '{}' produced an invalid or non-increasing requirement; using '{}'",
                        requestedFormula, ChargeRequirementFormula.DEFAULT_EXPRESSION);
            }
            affordable = calculateAffordable(safeCapacity, safeCost, safeMaximum, DEFAULT_FORMULA);
        }
        return Math.clamp(affordable, safeMinimum, safeMaximum);
    }

    private static int calculateAffordable(double capacity, double castingDraw, int maximum,
                                           ChargeRequirementFormula.Compiled formula) {
        int affordable = 0;
        double previousRequirement = 0.0;
        for (int charge = 1; charge <= maximum; charge++) {
            double requirement = formula.evaluate(capacity, castingDraw, charge);
            if (!Double.isFinite(requirement) || requirement <= 0.0
                    || charge > 1 && requirement <= previousRequirement + 1.0e-7) {
                return -1;
            }
            if (requirement > capacity + 1.0e-7) break;
            affordable = charge;
            previousRequirement = requirement;
        }
        return affordable;
    }

    private static ChargeRequirementFormula.Compiled resolveFormula(String expression) {
        String requested = expression == null ? ChargeRequirementFormula.DEFAULT_EXPRESSION : expression;
        CachedFormula cached = cachedFormula;
        if (cached.expression().equals(requested)) return cached.compiled();
        synchronized (ChargeCalculator.class) {
            cached = cachedFormula;
            if (cached.expression().equals(requested)) return cached.compiled();
            ChargeRequirementFormula.Compiled compiled;
            try {
                compiled = ChargeRequirementFormula.compile(requested);
            } catch (IllegalArgumentException exception) {
                TempoNotTime.LOGGER.warn("Invalid charge requirement formula '{}'; using '{}': {}",
                        requested, ChargeRequirementFormula.DEFAULT_EXPRESSION, exception.getMessage());
                compiled = DEFAULT_FORMULA;
            }
            cachedFormula = new CachedFormula(requested, compiled);
            return compiled;
        }
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0 ? value : fallback;
    }
}
