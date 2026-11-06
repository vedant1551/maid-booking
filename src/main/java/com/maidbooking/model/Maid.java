package com.maidbooking.model;

import com.maidbooking.calendar.MaidCalendar;
import com.maidbooking.exception.BadRequestException;
import com.maidbooking.exception.ConflictException;
import com.maidbooking.util.TextUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Maid {
    private static final long MIN_HOURLY_RATE_PAISE = 100;
    private static final long MAX_HOURLY_RATE_PAISE = 10_000_000;

    private final UUID id;
    private final String name;
    private final String locality;
    private final Set<ServiceType> skills;
    private final Gender gender;
    private final long hourlyRatePaise;
    private final List<WorkingHours> workingHours;
    @Getter(AccessLevel.NONE)
    private final MaidCalendar calendar = new MaidCalendar();
    @Getter(AccessLevel.NONE)
    private int ratingTotal;
    private int ratingCount;

    @Builder
    private Maid(String name, String locality, Set<ServiceType> skills, Gender gender,
                 long hourlyRatePaise, List<WorkingHours> workingHours) {
        this.id = UUID.randomUUID();
        this.name = name.strip();
        this.locality = TextUtils.normalize(locality);
        this.skills = Set.copyOf(skills);
        this.gender = gender;
        BadRequestException.check(hourlyRatePaise >= MIN_HOURLY_RATE_PAISE && hourlyRatePaise <= MAX_HOURLY_RATE_PAISE,
                "hourlyRate must be between 1 and 100000 rupees");
        this.hourlyRatePaise = hourlyRatePaise;
        this.workingHours = workingHours.stream()
                .sorted(Comparator.comparing(WorkingHours::getDay).thenComparing(WorkingHours::getFrom))
                .toList();
        checkNoOverlappingHours(this.workingHours);
    }

    private static void checkNoOverlappingHours(List<WorkingHours> sortedHours) {
        for (int i = 1; i < sortedHours.size(); i++) {
            WorkingHours previous = sortedHours.get(i - 1);
            WorkingHours current = sortedHours.get(i);
            boolean overlaps = previous.getDay() == current.getDay() && current.getFrom().isBefore(previous.getTo());
            BadRequestException.check(!overlaps, "Working hours overlap on " + current.getDay());
        }
    }

    public synchronized void addRating(int stars) {
        ratingTotal += stars;
        ratingCount++;
    }

    /** Average of customer ratings, or null until the first rating. */
    public synchronized BigDecimal getRating() {
        if (ratingCount == 0) {
            return null;
        }
        return BigDecimal.valueOf(ratingTotal).divide(BigDecimal.valueOf(ratingCount), 1, RoundingMode.HALF_UP);
    }

    public synchronized int getRatingCount() {
        return ratingCount;
    }

    public boolean offers(ServiceType service) {
        return skills.contains(service);
    }

    public boolean isAvailable(TimeSlot slot) {
        return isWorkingDuring(slot) && calendar.isFree(slot);
    }

    public boolean hasFreeTimeOn(LocalDate date, Duration duration, LocalDateTime now) {
        return freeTimeOn(date).stream().anyMatch(free -> {
            LocalDateTime start = free.getStart().isBefore(now) ? now : free.getStart();
            return !start.plus(duration).isAfter(free.getEnd());
        });
    }

    /** Working hours on that date, with back-to-back hours joined into one shift. */
    public List<TimeSlot> shiftsOn(LocalDate date) {
        List<TimeSlot> shifts = new ArrayList<>();
        for (WorkingHours hours : hoursOn(date)) {
            TimeSlot next = new TimeSlot(date.atTime(hours.getFrom()), date.atTime(hours.getTo()));
            TimeSlot last = shifts.isEmpty() ? null : shifts.get(shifts.size() - 1);
            if (last != null && last.getEnd().equals(next.getStart())) {
                shifts.set(shifts.size() - 1, new TimeSlot(last.getStart(), next.getEnd()));
            } else {
                shifts.add(next);
            }
        }
        return shifts;
    }

    public List<TimeSlot> bookedTimeOn(LocalDate date) {
        return calendar.bookedSlotsOn(date);
    }

    public List<TimeSlot> freeTimeOn(LocalDate date) {
        List<TimeSlot> booked = bookedTimeOn(date);
        List<TimeSlot> free = new ArrayList<>();
        for (TimeSlot shift : shiftsOn(date)) {
            LocalDateTime freeFrom = shift.getStart();
            for (TimeSlot visit : booked) {
                if (visit.getEnd().isAfter(freeFrom) && visit.getStart().isBefore(shift.getEnd())) {
                    if (visit.getStart().isAfter(freeFrom)) {
                        free.add(new TimeSlot(freeFrom, visit.getStart()));
                    }
                    freeFrom = visit.getEnd();
                }
            }
            if (freeFrom.isBefore(shift.getEnd())) {
                free.add(new TimeSlot(freeFrom, shift.getEnd()));
            }
        }
        return free;
    }

    public void reserve(List<Visit> visits) {
        for (Visit visit : visits) {
            if (!isWorkingDuring(visit.getSlot())) {
                throw new ConflictException("Maid is not working between "
                        + visit.getSlot().getStart() + " and " + visit.getSlot().getEnd().toLocalTime());
            }
        }
        calendar.reserve(visits);
    }

    public void release(Visit visit) {
        calendar.release(visit);
    }

    private boolean isWorkingDuring(TimeSlot slot) {
        return slot.isWithinOneDay() && shiftsOn(slot.getDate()).stream().anyMatch(shift ->
                !slot.getStart().isBefore(shift.getStart()) && !slot.getEnd().isAfter(shift.getEnd()));
    }

    private List<WorkingHours> hoursOn(LocalDate date) {
        return workingHours.stream().filter(hours -> hours.getDay() == date.getDayOfWeek()).toList();
    }
}
