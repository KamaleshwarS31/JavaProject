package com.evoting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web/JSP Controller for server-side rendered views.
 * Demonstrates JSP + Spring MVC integration for the Advanced Java syllabus.
 * No business logic — view routing only.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "redirect:/elections";
    }

    @GetMapping("/elections")
    public String electionsList(Model model) {
        model.addAttribute("pageTitle", "Elections");
        return "election/list";
    }

    @GetMapping("/elections/{id}")
    public String electionDetail(@PathVariable String id, Model model) {
        model.addAttribute("electionId", id);
        model.addAttribute("pageTitle", "Election Details");
        return "election/detail";
    }

    @GetMapping("/elections/{id}/vote")
    public String votePage(@PathVariable String id, Model model) {
        model.addAttribute("electionId", id);
        model.addAttribute("pageTitle", "Cast Vote");
        return "ballot/vote";
    }

    @GetMapping("/ballot/receipt")
    public String receiptPage(Model model) {
        model.addAttribute("pageTitle", "Voting Receipt");
        return "ballot/receipt";
    }

    @GetMapping("/auth/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Login");
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "Register");
        return "auth/register";
    }

    @GetMapping("/audit-portal")
    public String auditPortal(Model model) {
        model.addAttribute("pageTitle", "Audit Portal");
        return "audit/portal";
    }
}
