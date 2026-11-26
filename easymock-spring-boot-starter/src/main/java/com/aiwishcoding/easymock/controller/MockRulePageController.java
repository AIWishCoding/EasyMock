package com.aiwishcoding.easymock.controller;

import com.aiwishcoding.easymock.autoconfigure.EasyMockProperties;
import com.aiwishcoding.easymock.model.MockRule;
import com.aiwishcoding.easymock.scanner.EndpointScanner;
import com.aiwishcoding.easymock.service.MockRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("${easymock.page-path:/easymock}")
public class MockRulePageController {

    private final MockRuleService mockRuleService;
    private final EndpointScanner endpointScanner;
    private final EasyMockProperties properties;

    public MockRulePageController(MockRuleService mockRuleService,
                                  EndpointScanner endpointScanner,
                                  EasyMockProperties properties) {
        this.mockRuleService = mockRuleService;
        this.endpointScanner = endpointScanner;
        this.properties = properties;
    }

    @ModelAttribute("pagePath")
    public String pagePath() {
        return properties.getPagePath();
    }

    @ModelAttribute("apiPath")
    public String apiPath() {
        return properties.getApiPath();
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
