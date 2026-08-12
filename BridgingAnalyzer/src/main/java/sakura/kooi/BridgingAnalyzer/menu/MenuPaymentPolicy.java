package sakura.kooi.BridgingAnalyzer.menu;

/** Pure validation for potentially inconsistent economy-provider withdrawal results. */
final class MenuPaymentPolicy {
    private static final double EPSILON = 0.000_001D;

    private MenuPaymentPolicy() {
    }

    static Decision assess(double expectedCost, MenuEconomy.Payment payment) {
        if (payment == null) {
            return new Decision(Outcome.FAILED, 0.0D);
        }
        double debited = payment.debitedAmount();
        double refundable = Double.isFinite(debited) && debited > 0.0D ? debited : 0.0D;
        if (!payment.successful()) {
            return new Decision(Outcome.FAILED, refundable);
        }
        if (!Double.isFinite(debited) || Math.abs(debited - expectedCost) > EPSILON) {
            return new Decision(Outcome.AMOUNT_MISMATCH, refundable);
        }
        return new Decision(Outcome.PROCEED, 0.0D);
    }

    enum Outcome {
        PROCEED,
        FAILED,
        AMOUNT_MISMATCH
    }

    record Decision(Outcome outcome, double refundAmount) {
    }
}
