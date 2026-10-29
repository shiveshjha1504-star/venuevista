package com.infotact.venuevista.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;

@ControllerAdvice
public class GlobalAdvice {
    @ModelAttribute("year")
    public int year() { return Year.now().getValue(); }
}
