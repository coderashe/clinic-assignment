package com.clinic.web;

import com.clinic.service.ReportingService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final ReportingService reports;
    private final Clock clock;

    @GetMapping("/dashboard")
    String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                     Model m) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        YearMonth ym = month != null ? month : YearMonth.from(day);
        LocalDate monthStart = ym.atDay(1), monthEnd = ym.atEndOfMonth();

        var peak = reports.peakHours(monthStart, monthEnd);
        m.addAttribute("date", day);
        m.addAttribute("month", ym);
        m.addAttribute("peak", peak);
        m.addAttribute("peakMax", peak.values().stream().max(Long::compare).orElse(1L));
        m.addAttribute("waits", reports.waitTimeByPriority(monthStart, monthEnd));
        m.addAttribute("dailyByClinician", reports.throughput(day, day).stream()
                .collect(Collectors.toMap(ReportingService.ThroughputRow::clinician, ReportingService.ThroughputRow::completed)));
        m.addAttribute("monthlyThroughput", reports.throughput(monthStart, monthEnd));
        m.addAttribute("summaries", List.of(
                reports.summary("Daily summary - " + day, day, day),
                reports.summary("Monthly summary - " + ym, monthStart, monthEnd)));
        m.addAttribute("forecast", reports.demandForecast(7));
        return "manager/dashboard";
    }
}
