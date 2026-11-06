package com.maidbooking.mapper;

import com.maidbooking.dto.money.Rupees;
import com.maidbooking.dto.request.CreateMaidRequest;
import com.maidbooking.dto.request.MaidSearchRequest;
import com.maidbooking.dto.response.MaidAvailabilityResponse;
import com.maidbooking.dto.response.MaidResponse;
import com.maidbooking.dto.response.TimeRangeResponse;
import com.maidbooking.dto.response.WorkingHoursResponse;
import com.maidbooking.model.Maid;
import com.maidbooking.model.TimeSlot;
import com.maidbooking.model.WorkingHours;
import com.maidbooking.search.MaidSearchCriteria;
import java.time.LocalDate;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MaidMapper {

    public Maid toMaid(CreateMaidRequest request) {
        return Maid.builder()
                .name(request.getName())
                .locality(request.getLocality())
                .skills(request.getSkills())
                .gender(request.getGender())
                .hourlyRatePaise(request.hourlyRateInPaise())
                .workingHours(request.getWorkingHours().stream()
                        .map(hours -> new WorkingHours(hours.getDay(), hours.getFrom(), hours.getTo()))
                        .toList())
                .build();
    }

    public MaidSearchCriteria toCriteria(MaidSearchRequest request) {
        return MaidSearchCriteria.builder()
                .locality(request.getLocality())
                .service(request.getService())
                .skills(request.getSkills())
                .gender(request.getGender())
                .minRating(request.getMinRating())
                .maxHourlyRatePaise(request.maxHourlyRateInPaise())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .date(request.getDate())
                .availableNow(Boolean.TRUE.equals(request.getAvailableNow()))
                .durationMinutes(request.getDurationMinutes())
                .build();
    }

    public MaidAvailabilityResponse toAvailability(Maid maid, LocalDate date) {
        return MaidAvailabilityResponse.builder()
                .maidId(maid.getId())
                .date(date)
                .workingHours(toRanges(maid.shiftsOn(date)))
                .booked(toRanges(maid.bookedTimeOn(date)))
                .free(toRanges(maid.freeTimeOn(date)))
                .build();
    }

    private List<TimeRangeResponse> toRanges(List<TimeSlot> slots) {
        return slots.stream()
                .map(slot -> TimeRangeResponse.builder().startTime(slot.getStart()).endTime(slot.getEnd()).build())
                .toList();
    }

    public MaidResponse toResponse(Maid maid) {
        return MaidResponse.builder()
                .id(maid.getId())
                .name(maid.getName())
                .locality(maid.getLocality())
                .skills(maid.getSkills())
                .gender(maid.getGender())
                .rating(maid.getRating())
                .ratingCount(maid.getRatingCount())
                .hourlyRate(Rupees.fromPaise(maid.getHourlyRatePaise()))
                .workingHours(maid.getWorkingHours().stream()
                        .map(hours -> WorkingHoursResponse.builder()
                                .day(hours.getDay()).from(hours.getFrom()).to(hours.getTo()).build())
                        .toList())
                .build();
    }
}
