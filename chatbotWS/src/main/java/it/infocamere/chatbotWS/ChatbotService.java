package it.infocamere.chatbotWS;

import com.theokanning.openai.completion.chat.ChatMessage;

import java.net.MalformedURLException;

public interface ChatbotService {

    String sendMessage (String message, String user, boolean appuntamentoFound);

    String getGeneralita (String message, String user) throws MalformedURLException, InterruptedException;

    String findAppuntamento (String prestazioneText, String user) throws MalformedURLException;

    String startChat ();

    void cleanHistory(String user);


}
