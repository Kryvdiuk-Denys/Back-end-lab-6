package com.coworking.booking.controller;

import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.async.BookingPreparationService;
import com.coworking.booking.dto.BookingPreparationRequest;
import com.coworking.booking.dto.BookingPreparationResult;
import com.coworking.booking.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingRestController {

    private final BookingService bookingService;
    private final BookingPreparationService bookingPreparationService;

    public BookingRestController(BookingService bookingService, BookingPreparationService bookingPreparationService) {
        this.bookingService = bookingService;
        this.bookingPreparationService = bookingPreparationService;
    }

    @GetMapping
    public List<BookingDto> findAll() {
        return bookingService.findAll();
    }

    @PostMapping("/preflight")
    public BookingPreparationResult prepare(@Valid @RequestBody BookingPreparationRequest request) {
        return bookingPreparationService.prepare(request);
    }

    @GetMapping("/{id}")
    public BookingDto findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody BookingDto dto) {
        return bookingService.create(dto);
    }

    @PutMapping("/{id}")
    public BookingDto update(@PathVariable Long id, @Valid @RequestBody BookingDto dto) {
        return bookingService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }
}
