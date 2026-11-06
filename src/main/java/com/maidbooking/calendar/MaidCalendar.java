package com.maidbooking.calendar;

import com.maidbooking.exception.ConflictException;
import com.maidbooking.model.TimeSlot;
import com.maidbooking.model.Visit;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/** One lock per day: a visit never crosses midnight, so overlapping bookings always wait for each other. */
public class MaidCalendar {
    private final Map<LocalDate, List<Visit>> visitsByDay = new ConcurrentHashMap<>();
    private final Map<LocalDate, ReentrantLock> dayLocks = new ConcurrentHashMap<>();

    public boolean isFree(TimeSlot slot) {
        return visitsOn(slot.getDate()).stream().noneMatch(visit -> visit.getSlot().overlaps(slot));
    }

    public void reserve(List<Visit> visits) {
        List<ReentrantLock> locks = visits.stream()
                .map(visit -> visit.getSlot().getDate())
                .distinct()
                .sorted()
                .map(day -> dayLocks.computeIfAbsent(day, key -> new ReentrantLock()))
                .toList();
        locks.forEach(ReentrantLock::lock);
        try {
            for (Visit visit : visits) {
                if (!isFree(visit.getSlot())) {
                    throw new ConflictException("Maid is already booked between "
                            + visit.getSlot().getStart() + " and " + visit.getSlot().getEnd().toLocalTime());
                }
            }
            visits.forEach(visit -> visitsByDay
                    .computeIfAbsent(visit.getSlot().getDate(), day -> new CopyOnWriteArrayList<>())
                    .add(visit));
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public void release(Visit visit) {
        List<Visit> visits = visitsByDay.get(visit.getSlot().getDate());
        if (visits != null) {
            visits.remove(visit);
        }
    }

    public List<TimeSlot> bookedSlotsOn(LocalDate date) {
        return visitsOn(date).stream()
                .map(Visit::getSlot)
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .toList();
    }

    private List<Visit> visitsOn(LocalDate date) {
        return visitsByDay.getOrDefault(date, List.of());
    }
}
