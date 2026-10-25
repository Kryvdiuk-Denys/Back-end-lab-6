package com.coworking.booking.controller;

import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.entity.BookingStatus;
import com.coworking.booking.BookingService;
import com.coworking.booking.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/bookings")
public class BookingUiController {

    private final BookingService bookingService;
    private final WorkspaceService workspaceService;

    public BookingUiController(BookingService bookingService, WorkspaceService workspaceService) {
        this.bookingService = bookingService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        BookingDto booking = new BookingDto();
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        booking.setStartsAt(startsAt);
        booking.setEndsAt(startsAt.plusHours(2));
        booking.setStatus(BookingStatus.CONFIRMED);
        workspaceService.findAll().stream()
                .findFirst()
                .ifPresent(workspace -> booking.setWorkspaceId(workspace.getId()));

        model.addAttribute("booking", booking);
        prepareForm(model, "/bookings", "New booking");
        return "bookings/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("booking") BookingDto booking,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/bookings", "New booking");
            return "bookings/form";
        }
        try {
            bookingService.create(booking);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("booking.invalid", ex.getMessage());
            prepareForm(model, "/bookings", "New booking");
            return "bookings/form";
        }
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("booking", bookingService.findById(id));
        prepareForm(model, "/bookings/" + id, "Edit booking");
        return "bookings/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("booking") BookingDto booking,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/bookings/" + id, "Edit booking");
            return "bookings/form";
        }
        try {
            bookingService.update(id, booking);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("booking.invalid", ex.getMessage());
            prepareForm(model, "/bookings/" + id, "Edit booking");
            return "bookings/form";
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookingService.delete(id);
        return "redirect:/bookings";
    }

    private void prepareForm(Model model, String action, String title) {
        model.addAttribute("workspaces", workspaceService.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("action", action);
        model.addAttribute("title", title);
    }
}
