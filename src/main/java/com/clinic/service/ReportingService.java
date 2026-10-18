package com.clinic.service;

import com.clinic.domain.*;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.QueueEntryRepository;
import com.clinic.repository.VisitRecordRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manager analytics: pure Streams / Collectors / Comparators over the persisted data. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    public record WaitStats(long samples, double mean, double median) {}
    public record ThroughputRow(String clinician, long completed) {}
    public record Forecast(LocalDate date, DayOfWeek day, double expectedAppointments) {}
    public record Summary(String label, long appointments, long cancelled, long checkIns, long completedVisits,
                          double averageWaitMinutes) {}

    private static final int FORECAST_HISTORY_WEEKS = 8;

    private final QueueEntryRepository queue;
    private final VisitRecordRepository visits;
    private final AppointmentRepository appointments;
    private final Clock clock;

    /** Demand by hour of day (check-ins), sorted by hour. */
    public SortedMap<Integer, Long> peakHours(LocalDate from, LocalDate to) {
        return queue.findByCheckInTimeBetween(start(from), end(to)).stream()
                .collect(Collectors.groupingBy(q -> q.getCheckInTime().getHour(), TreeMap::new, Collectors.counting()));
    }

    /** Mean & median minutes waited per priority. Patients still waiting count up to "now" (real time). */
    public SortedMap<Priority, WaitStats> waitTimeByPriority(LocalDate from, LocalDate to) {
        return waitMinutes(from, to).stream()
                .collect(Collectors.groupingBy(WaitSample::priority, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), list -> stats(
                                list.stream().mapToDouble(WaitSample::minutes).sorted().toArray()))));
    }

    /** Completed consultations per clinician, busiest first. */
    public List<ThroughputRow> throughput(LocalDate from, LocalDate to) {
        return visits.findByEndTimeBetween(start(from), end(to)).stream()
                .collect(Collectors.groupingBy(v -> v.getClinician().displayName(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ThroughputRow(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ThroughputRow::completed).reversed()
                        .thenComparing(ThroughputRow::clinician))
                .toList();
    }

    public Summary summary(String label, LocalDate from, LocalDate to) {
        List<Appointment> appts = appointments.findByAppointmentTimestampBetween(start(from), end(to));
        double avgWait = waitMinutes(from, to).stream().mapToDouble(WaitSample::minutes).average().orElse(0);
        return new Summary(label,
                appts.size(),
                appts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count(),
                queue.findByCheckInTimeBetween(start(from), end(to)).size(),
                visits.findByEndTimeBetween(start(from), end(to)).size(),
                avgWait);
    }

    /** Moving-average forecast: expected bookings per upcoming day = mean of the same weekday over the last 8 weeks. */
    public List<Forecast> demandForecast(int days) {
        LocalDate today = LocalDate.now(clock);
        Map<DayOfWeek, Long> perWeekday = appointments
                .findByAppointmentTimestampBetween(today.minusWeeks(FORECAST_HISTORY_WEEKS).atStartOfDay(), today.atStartOfDay())
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(a -> a.getAppointmentTimestamp().getDayOfWeek(), Collectors.counting()));
        return IntStream.rangeClosed(1, days)
                .mapToObj(i -> today.plusDays(i))
                .map(d -> new Forecast(d, d.getDayOfWeek(),
                        perWeekday.getOrDefault(d.getDayOfWeek(), 0L) / (double) FORECAST_HISTORY_WEEKS))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private record WaitSample(Priority priority, double minutes) {}

    private List<WaitSample> waitMinutes(LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, LocalDateTime> startedAt = visits.findByQueueEntryCheckInTimeBetween(start(from), end(to)).stream()
                .collect(Collectors.toMap(v -> v.getQueueEntry().getId(), VisitRecord::getStartTime));
        return queue.findByCheckInTimeBetween(start(from), end(to)).stream()
                .map(q -> new WaitSample(q.getPriorityLevel(),
                        Duration.between(q.getCheckInTime(), startedAt.getOrDefault(q.getId(), now)).toSeconds() / 60.0))
                .toList();
    }

    private static WaitStats stats(double[] sorted) {
        int n = sorted.length;
        if (n == 0) return new WaitStats(0, 0, 0);
        double mean = Arrays.stream(sorted).average().orElse(0);
        double median = n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        return new WaitStats(n, mean, median);
    }

    private static LocalDateTime start(LocalDate d) { return d.atStartOfDay(); }

    private static LocalDateTime end(LocalDate d) { return d.plusDays(1).atStartOfDay(); }
}
