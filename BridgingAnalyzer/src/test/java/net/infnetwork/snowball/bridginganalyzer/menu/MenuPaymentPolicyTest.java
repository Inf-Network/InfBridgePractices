package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MenuPaymentPolicyTest {
    @Test
    void exactSuccessfulDebitMayCommitAction() {
        MenuPaymentPolicy.Decision decision = MenuPaymentPolicy.assess(
                100.0D, new MenuEconomy.Payment(true, 100.0D, "ok"));

        assertEquals(MenuPaymentPolicy.Outcome.PROCEED, decision.outcome());
        assertEquals(0.0D, decision.refundAmount());
    }

    @Test
    void failedPartialDebitMustBeRefunded() {
        MenuPaymentPolicy.Decision decision = MenuPaymentPolicy.assess(
                500.0D, new MenuEconomy.Payment(false, 125.0D, "partial"));

        assertEquals(MenuPaymentPolicy.Outcome.FAILED, decision.outcome());
        assertEquals(125.0D, decision.refundAmount());
    }

    @Test
    void successfulButWrongDebitMustNotCommit() {
        MenuPaymentPolicy.Decision decision = MenuPaymentPolicy.assess(
                100.0D, new MenuEconomy.Payment(true, 99.0D, "wrong"));

        assertEquals(MenuPaymentPolicy.Outcome.AMOUNT_MISMATCH, decision.outcome());
        assertEquals(99.0D, decision.refundAmount());
    }

    @Test
    void nonFiniteDebitIsNeverRefundedAsANumber() {
        MenuPaymentPolicy.Decision decision = MenuPaymentPolicy.assess(
                100.0D, new MenuEconomy.Payment(true, Double.NaN, "broken"));

        assertEquals(MenuPaymentPolicy.Outcome.AMOUNT_MISMATCH, decision.outcome());
        assertEquals(0.0D, decision.refundAmount());
    }
}
