package it.infocamere.chatbotWS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chatbot")
public class ChatbotController {

    @Autowired
    ChatbotService chatbotService;


    @GetMapping(value = "/startChat", produces = "application/json")
    public String startChat(@RequestParam String user, Model model) {
        String response = chatbotService.startChat();
//        String startMessage = "Benvenuto, come posso aiutarti?";
        model.addAttribute("message", response);
        model.addAttribute("user", user);
        return "startChat";
    }

}
