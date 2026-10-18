package com.clinic.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/")
    String home(Authentication auth, Model model, RedirectAttributes ra) {
        // keep a banner alive across this extra redirect hop
        for (String key : new String[] {"error", "success"}) {
            Object v = model.asMap().get(key);
            if (v != null) ra.addFlashAttribute(key, v);
        }
        var roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        if (roles.contains("ROLE_RECEPTIONIST")) return "redirect:/reception/appointments";
        if (roles.contains("ROLE_CLINICIAN")) return "redirect:/clinician/queue";
        if (roles.contains("ROLE_MANAGER")) return "redirect:/manager/dashboard";
        return "redirect:/login?logout";
    }
}
