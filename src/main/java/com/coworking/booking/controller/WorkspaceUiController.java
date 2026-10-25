package com.coworking.booking.controller;

import com.coworking.booking.dto.WorkspaceDto;
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

@Controller
@RequestMapping("/workspaces")
public class WorkspaceUiController {

    private final WorkspaceService workspaceService;

    public WorkspaceUiController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("workspaces", workspaceService.findAll());
        return "workspaces/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        WorkspaceDto workspace = new WorkspaceDto();
        workspace.setActive(true);
        model.addAttribute("workspace", workspace);
        model.addAttribute("action", "/workspaces");
        model.addAttribute("title", "New workspace");
        return "workspaces/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("workspace") WorkspaceDto workspace,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/workspaces", "New workspace");
            return "workspaces/form";
        }
        workspaceService.create(workspace);
        return "redirect:/workspaces";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("workspace", workspaceService.findById(id));
        model.addAttribute("action", "/workspaces/" + id);
        model.addAttribute("title", "Edit workspace");
        return "workspaces/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("workspace") WorkspaceDto workspace,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/workspaces/" + id, "Edit workspace");
            return "workspaces/form";
        }
        workspaceService.update(id, workspace);
        return "redirect:/workspaces";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return "redirect:/workspaces";
    }

    private void prepareForm(Model model, String action, String title) {
        model.addAttribute("action", action);
        model.addAttribute("title", title);
    }
}
