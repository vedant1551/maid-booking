package com.maidbooking.service.impl;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.exception.NotFoundException;
import com.maidbooking.model.Maid;
import com.maidbooking.repository.MaidRepository;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import com.maidbooking.service.MaidService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaidServiceImpl implements MaidService {
    private static final Comparator<Maid> BEST_MATCH_FIRST = Comparator
            .comparing(Maid::getRating, Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()))
            .thenComparingLong(Maid::getHourlyRatePaise)
            .thenComparing(Maid::getName);

    private final MaidRepository maidRepository;
    private final List<MaidFilter> filters;
    private final Clock clock;

    @Override
    public Maid onboard(Maid maid) {
        return maidRepository.save(maid);
    }

    @Override
    public Maid getMaid(UUID maidId) {
        return maidRepository.findById(maidId).orElseThrow(() -> new NotFoundException("Maid", maidId));
    }

    @Override
    public List<Maid> search(MaidSearchCriteria criteria) {
        MaidSearchCriteria searchNow = criteria.toBuilder().searchedAt(LocalDateTime.now(clock)).build();
        validate(searchNow);
        List<MaidFilter> applicableFilters = filters.stream().filter(filter -> filter.isApplicable(searchNow)).toList();
        return maidRepository.findAll().stream()
                .filter(maid -> applicableFilters.stream().allMatch(filter -> filter.matches(maid, searchNow)))
                .sorted(BEST_MATCH_FIRST)
                .toList();
    }

    private void validate(MaidSearchCriteria criteria) {
        boolean hasStart = criteria.getStartTime() != null;
        BadRequestException.check(hasStart == (criteria.getEndTime() != null), "Give both startTime and endTime");
        int timeFilters = (hasStart ? 1 : 0) + (criteria.getDate() != null ? 1 : 0) + (criteria.isAvailableNow() ? 1 : 0);
        BadRequestException.check(timeFilters <= 1, "Use only one of startTime/endTime, date or availableNow");
        BadRequestException.check(!hasStart || criteria.getDurationMinutes() == null,
                "durationMinutes is not used with startTime and endTime");
        BadRequestException.check(!hasStart || !criteria.getStartTime().isBefore(criteria.getSearchedAt()),
                "startTime is in the past");
        BadRequestException.check(criteria.getDate() == null
                || !criteria.getDate().isBefore(criteria.getSearchedAt().toLocalDate()), "date is in the past");
    }
}
