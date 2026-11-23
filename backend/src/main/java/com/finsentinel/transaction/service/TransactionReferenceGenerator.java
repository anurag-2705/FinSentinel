package com.finsentinel.transaction.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TransactionReferenceGenerator {

    private static final String PREFIX = "FSN";

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateReference() {

        String date = LocalDate.now()
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        StringBuilder randomPart = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            randomPart.append(CHARACTERS.charAt(index));
        }

        return PREFIX + "-" + date + "-" + randomPart;
    }
}