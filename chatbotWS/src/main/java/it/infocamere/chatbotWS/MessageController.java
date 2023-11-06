package it.infocamere.chatbotWS;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;

@RestController
@RequestMapping(value = "/message")
public class MessageController {

    @Autowired
    ChatbotService chatbotService;


    @PostMapping(value = "/sendMessage", produces = "application/json")
    public String sendMessage(@RequestParam String message, @RequestParam String user, @RequestParam boolean appuntamentoFound){
        String response = chatbotService.sendMessage(message, user, appuntamentoFound);
        return response;
    }

    @PostMapping(value = "/getGeneralita", produces = "application/json")
    public String getGeneralita(@RequestParam String message, @RequestParam String user) throws MalformedURLException, InterruptedException {
       return chatbotService.getGeneralita(message, user);
//        return response.getContent();
    }

    @PostMapping(value = "/findAppuntamento", produces = "application/json")
    public String findAppuntamento(@RequestParam String prestazioneText, @RequestParam String user) throws MalformedURLException {
        return chatbotService.findAppuntamento(prestazioneText, user);
    }

    @PostMapping(value = "/cleanHistory", produces = "application/json")
    public String cleanHistory(@RequestParam String user){
        chatbotService.cleanHistory(user);
        return "History deleted for user: " + user;
    }


}
