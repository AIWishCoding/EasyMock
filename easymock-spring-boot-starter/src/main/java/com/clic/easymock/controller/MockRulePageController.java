package com.clic.easymock.controller;

import com.clic.easymock.model.MockRule;
import com.clic.easymock.scanner.EndpointScanner;
import com.clic.easymock.service.MockRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("${easymock.page-path:/easymock}")
public class MockRulePageController {

    private final MockRuleService mockRuleService;
    private final EndpointScanner endpointScanner;

    public MockRulePageController(MockRuleService mockRuleService,
                                  EndpointScanner endpointScanner) {
        this.mockRuleService = mockRuleService;
        this.endpointScanner = endpointScanner;
    }

    @GetMapping
    public String index(Model model) {
        List<MockRule> rules = mockRuleService.listAll();
        List<String> endpoints = endpointScanner.scanEndpoints();
        model.addAttribute("rules", rules);
        model.addAttribute("endpoints", endpoints);
        return "easymock/index";
    }

    @GetMapping("/rule/add")
    public String addForm(Model model) {
        model.addAttribute("rule", new MockRule());
        model.addAttribute("endpoints", endpointScanner.scanEndpoints());
        model.addAttribute("isEdit", false);
        return "easymock/rule-form";
    }

    @GetMapping("/rule/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        MockRule rule = mockRuleService.getById(id);
        model.addAttribute("rule", rule);
        model.addAttribute("endpoints", endpointScanner.scanEndpoints());
        model.addAttribute("isEdit", true);
        return "easymock/rule-form";
    }
}
