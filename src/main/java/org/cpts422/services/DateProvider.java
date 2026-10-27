package org.cpts422.services;

import java.time.LocalDate;

// Wraps "today" so that tests can control the date
public interface DateProvider {
    LocalDate today();
}
