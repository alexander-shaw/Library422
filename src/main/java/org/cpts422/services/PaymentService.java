package org.cpts422.services;

import org.cpts422.models.User;

// All amounts are in cents to avoid floating point rounding errors with money
public interface PaymentService {
    int getOutstandingBalance(User user);
    void chargeLateFee(User user, int amountInCents);
}
