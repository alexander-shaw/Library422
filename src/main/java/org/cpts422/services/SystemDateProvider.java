package org.cpts422.services;

import java.time.LocalDate;

public class SystemDateProvider implements DateProvider {
    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
