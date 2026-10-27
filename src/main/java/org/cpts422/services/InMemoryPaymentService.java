package org.cpts422.services;

import org.cpts422.models.User;

import java.util.HashMap;
import java.util.Map;

public class InMemoryPaymentService implements PaymentService {
    private final Map<Integer, Integer> balances = new HashMap<>();

    @Override
    public int getOutstandingBalance(User user) {
        return balances.getOrDefault(user.userId(), 0);
    }

    @Override
    public void chargeLateFee(User user, int amountInCents) {
        balances.merge(user.userId(), amountInCents, Integer::sum);
    }
}
