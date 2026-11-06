package com.maidbooking.controller;

import com.maidbooking.dto.request.CreateMaidRequest;
import com.maidbooking.dto.request.MaidSearchRequest;
import com.maidbooking.dto.response.MaidAvailabilityResponse;
import com.maidbooking.dto.response.MaidResponse;
import com.maidbooking.mapper.MaidMapper;
import com.maidbooking.service.MaidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Maids")
@RestController
@RequestMapping("/maids")
@RequiredArgsConstructor
public class MaidController {
    private final MaidService maidService;

    @Operation(summary = "Add a maid")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaidResponse onboard(@Valid @RequestBody CreateMaidRequest request) {
        return MaidMapper.toResponse(maidService.onboard(MaidMapper.toMaid(request)));
    }

    @Operation(summary = "Get a maid")
    @GetMapping("/{maidId}")
    public MaidResponse getMaid(@PathVariable UUID maidId) {
        return MaidMapper.toResponse(maidService.getMaid(maidId));
    }

    @Operation(summary = "Working hours, booked time and free time of a maid on a date")
    @GetMapping("/{maidId}/availability")
    public MaidAvailabilityResponse getAvailability(@PathVariable UUID maidId, @RequestParam LocalDate date) {
        return MaidMapper.toAvailability(maidService.getMaid(maidId), date);
    }

    @Operation(summary = "Search maids by profile and availability")
    @GetMapping
    public List<MaidResponse> search(@Valid @ParameterObject @ModelAttribute MaidSearchRequest request) {
        return maidService.search(MaidMapper.toCriteria(request)).stream()
                .map(MaidMapper::toResponse)
                .toList();
    }
}
