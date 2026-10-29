package com.infotact.venuevista.service;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/** Template helper: ${@fmt.inr(12345)} -> ₹12,345 (Indian digit grouping). */
@Component("fmt")
public class Fmt {
    public String inr(Number n) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"));
        return "₹" + nf.format(n == null ? 0 : n.longValue());
    }
}
