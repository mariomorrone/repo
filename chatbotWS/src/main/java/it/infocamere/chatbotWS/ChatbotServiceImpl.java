package it.infocamere.chatbotWS;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ChatbotServiceImpl implements ChatbotService{
    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${prestazioni.select}")
    private String prestazioniSelect;

    @Value("${agenda.findDate.api}")
    private String agendaFindDateApi;

    @Value("${agenda.takeAppuntamento.api}")
    private String agendaTakeAppuntamentoApi;

    private final String instructionsOLD = "sei l'assistente virtuale della camera di commercio di Milano. Il tuo compito è chiedere informazioni all'utente per capire a quale categoria di appuntamento indirizzarlo. Le categorie disponibili sono: acquisto carta tachigrafica, acquisto firma digitale, informazioni su pratiche del registro imprese, vidimazione libri, attivazione firma remota, richieste sistema identità digitale, richieste nulla-osta, richieste visti e documenti per l'estero, punto impresa digitale. Puoi fare massimo 3 domande all'utente per capire a quale servizio indirizzarlo. Quando hai trovato il servizio adatto rispondi che il primo appuntamento disponibile è il 1 Luglio alle ore 18 e dagli questo link per prenotare: https://servizionline.milomb.camcom.it/front-agenda. Se sei sicuro già prima di aver fatto 3 domande, puoi rispondere come prima. Inizia con la domanda 'come ti posso aiutare? quale servizio cerchi?'. Devi lavorare solo sul territorio di Milano. Se dopo le 3 domande non hai trovato il servizio adatto saluta l'utente in modo seccato. Non proporre soluzioni esterne, resta sui servizi camerali. Mantieni un registro colloquiale. Non suggerire i servizi all'utente, limitati a chiedere informazioni generiche. se l'utente è maleducato o risponde con risposte non attinenti interrompi la conversazione.";
    private final String instructions = "devi scegliere qual'è il servizio più adatto per l'esigenza: \"${placeholder}\". Di seguito sono riportati i servizi disponibili con il loro codice numerico:\n" +
            "\n" +
            "AGND-157 - Certificato d'origine: Il certificato d'origine è un documento emesso dalle autorità competenti che attesta l'origine di un prodotto, fornendo informazioni dettagliate sulla sua provenienza geografica e/o sul processo di produzione utilizzato. Questo documento è spesso richiesto per motivi fiscali, doganali o commerciali, al fine di garantire la conformità e la tracciabilità del prodotto durante le operazioni di importazione ed esportazione.\n" +
            "AGND-157 -Carta tachigrafica: La carta tachigrafica è una scheda magnetica o smart card assegnata ai conducenti dei veicoli commerciali per registrare e monitorare il tempo di guida, le pause e le attività lavorative, assicurando il rispetto delle normative sulle ore di lavoro e di riposo nel trasporto su strada.\n" +
            "AGND-157 - Firma digitale: La firma digitale è una tecnologia crittografica che garantisce l'autenticità, l'integrità e la non ripudiabilità di un documento o un messaggio elettronico, utilizzando chiavi pubbliche e private per verificare l'identità del firmatario e proteggere i dati dalla manipolazione.\n" +
            "AGND-157 - Punto impresa digitale: Il punto impresa digitale è uno spazio fisico o virtuale dove le imprese possono accedere a servizi di consulenza, formazione e supporto per adottare e sfruttare le tecnologie digitali, migliorando la loro competitività e sviluppando nuove opportunità di business nell'era digitale.\n" +
            "AGND-157 - Carnet ATA: Il carnet ATA è un documento doganale internazionale utilizzato per semplificare temporaneamente le procedure doganali per l'importazione e l'esportazione di merci in determinati paesi, eliminando la necessità di depositare una cauzione o pagare dazi e tasse.\n" +
            "AGND-157 - Certificati registro imprese: Il certificato è un documento in bollo, ha valore legale, certifica l'iscrizione dell'impresa nel Registro delle Imprese e nel Repertorio Economico Amministrativo tenuti dalle Camere di commercio italiane. \n"  +
            "AGND-157 - Proprietà Intellettuale: Attraverso il servizio la camera di commercio assistere nelle registrazioni dei marchi, offrire consulenza sulla proprietà intellettuale, risolvere dispute come mediatori o arbitratori, organizzare eventi formativi, facilitare collaborazioni tra aziende per la protezione e sfruttamento della proprietà intellettuale. \n" +
            "Se trovi un servizio adatto tra quelli in elenco, rispondi con il nome completo e il codice numerico del servizio e una breve descrizione del servizio. " +
            "Non fornire servizi che non sono presenti nell'elenco" +
            "Se nessun servizio tra quelli disponibili si addice alla richiesta dell'utente rispondi con \"ERR323\"";

    private final String confirmInstructions = "riconosci se la frase \"${placeholder}\" è una conferma o una negazione. Se non riesci a riconoscere la frase dell'utente rispondi con 'ERR323', altrimenti rispondi con 'conferma' o 'negazione'.";

    private final String rewriteInstructions = "riscrivi la frase '${placeholder}' cambiando qualche parola.";

    private final String generalitaInstructions = "chiedi all'utente nell'ordine: 1- Nome, 2- Cognome, 3- Email. Quando hai raccolto tutte le informazioni ritorna un elenco ordinato: nome, cognome, mail";
    private final LinkedHashMap<String, LinkedHashMap<String, String>> userData = new LinkedHashMap<>();

    @Override
    public String sendMessage(String message, String user, boolean appuntamentoFound) {

//        int maxQuestions = 10;
        OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
        List<ChatMessage> listMessage = new ArrayList<>();
        ChatMessage sysContext = new ChatMessage();
        sysContext.setRole("system");
        if (!appuntamentoFound){
            sysContext.setContent(instructions.replace("${placeholder}", message));
        }
        else{
            sysContext.setContent(confirmInstructions.replace("${placeholder}", message));
        }
        listMessage.add(sysContext);
//        if (previousResponse.containsKey(user)){
//            listMessage.addAll(previousResponse.get(user));
//        }
        ChatMessage newQuestion = new ChatMessage();
        newQuestion.setContent(message);
        newQuestion.setRole("user");
        listMessage.add(newQuestion);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model("gpt-3.5-turbo").
                messages(listMessage).
                temperature(0.5)
                .maxTokens(800)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.6)
                .build();

        try{
            ChatMessage response = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();

//            if (previousResponse.containsKey(user)){
//                List<ChatMessage> userPreviousResponse = previousResponse.get(user);
//                previousResponse.get(user).add(newQuestion);
//                previousResponse.get(user).add(response);
//                while (userPreviousResponse.size()>maxQuestions){
//                    userPreviousResponse.remove(0);
//                }
//                previousResponse.put(user, userPreviousResponse);
//
//            }
//            else{
//                ArrayList<ChatMessage> newList = new ArrayList<>();
//                newList.add(response);
//                previousResponse.put(user, newList);
//            }

            if (!appuntamentoFound){
                if (response.getContent().contains("ERR323")){
                    return "Scusa, non ho capito";
                }
                return "${APPUNTAMENTO_SEARCH}"+response.getContent();
            }
            else{
                if (response.getContent().toLowerCase().contains("conferma") && !response.getContent().toLowerCase().contains("negazione")){
                    return "${CONFERMA}";
                }
                if (response.getContent().toLowerCase().contains("negazione") && !response.getContent().toLowerCase().contains("conferma")){
                    return "${NEGAZIONE}Va bene, posso aiutarti in altro modo?";
                }
                else{
                    return "${ERROR_RICONOSCIMENTO}Scusa, non ho capito";
                }
            }
        }
        catch(Exception e){
            return e.getMessage();
        }
    }

    public String confirmDenyAi(String message){
        OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
        List<ChatMessage> listMessage = new ArrayList<>();
        ChatMessage sysContext = new ChatMessage();
        sysContext.setRole("system");
        sysContext.setContent(confirmInstructions.replace("${placeholder}", message));
        listMessage.add(sysContext);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model("gpt-3.5-turbo").
                messages(listMessage).
                temperature(0.5)
                .maxTokens(800)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.6)
                .build();

        try{
            ChatMessage response = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
            return response.getContent();
        }
        catch(Exception e){
            return e.getMessage();
        }
    }

    public String rewriteAi(String message){
        OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
        List<ChatMessage> listMessage = new ArrayList<>();
        ChatMessage sysContext = new ChatMessage();
        sysContext.setRole("system");
        sysContext.setContent(rewriteInstructions.replace("${placeholder}", message));
        listMessage.add(sysContext);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model("gpt-3.5-turbo").
                messages(listMessage).
                temperature(0.5)
                .maxTokens(800)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.6)
                .build();

        try{
            ChatMessage response = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();

            return response.getContent();
        }
        catch(Exception e){
            return e.getMessage();
        }
    }

    @Override
    public String getGeneralita(String message, String user) throws MalformedURLException, InterruptedException {
        userData.computeIfAbsent(user, k -> new LinkedHashMap<>());
        TimeUnit.SECONDS.sleep(2);
        if (message.equals("${CONFERMA}")){
//            return rewriteAi("Inserisci il tuo nome.");
            return "Inserisci il tuo nome";
        }
        else{
            if (userData.get(user).get("nome")==null){
                userData.get(user).put("nome", message);
//                return rewriteAi("Inserisci il tuo cognome.");
                return "Inserisci il tuo cognome";
            }
            if (userData.get(user).get("cognome")==null){
                userData.get(user).put("cognome", message);
//                return rewriteAi("Inserisci la tua email.");
                return "Inserisci la tua email";
            }
            if (userData.get(user).get("email")==null){
                userData.get(user).put("email", message);
                return "Nome: " + userData.get(user).get("nome") + "\nCognome: " + userData.get(user).get("cognome") + "\nEmail: " + userData.get(user).get("email") + "\nConfermi l'appuntamento?";
            }
            if (userData.get(user).get("nome")!=null && userData.get(user).get("cognome")!=null && userData.get(user).get("email")!=null){
                String resultAi = confirmDenyAi(message);
                if (resultAi.toLowerCase().contains("conferma") && !resultAi.toLowerCase().contains("negazione")){
                    return getAppuntamento(user);
                }
                if (resultAi.toLowerCase().contains("negazione") && !resultAi.toLowerCase().contains("conferma")){
                    userData.get(user).remove("nome");
                    userData.get(user).remove("cognome");
                    userData.get(user).remove("email");
                    return getGeneralita("${CONFERMA}", user);
                }
                else{
                    return "${ERROR_RICONOSCIMENTO}Scusa, non ho capito";
                }
            }
        }
        return null;
    }

    public String getAppuntamento (String user) throws MalformedURLException {
        String codePrestazione = userData.get(user).get("prestazioneId");
        String date = userData.get(user).get("date");
        String slot = userData.get(user).get("slot");
        String nome = userData.get(user).get("nome");
        String cognome = userData.get(user).get("cognome");
        String email = userData.get(user).get("email");
        String urlString = agendaTakeAppuntamentoApi + "prestazione=" + codePrestazione + "&userEmail="+ email +
                "&dataAppuntamento="+date+"&nome="+nome+"&cognome="+cognome+"&interno=false&slot="+slot+"&servizioRichiesta=ROL_2551";
        URL url = new URL(urlString);
        try {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            if(responseCode == 200){
                BufferedReader inBR = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String serviceOutputResultPartial = "";
                String serviceOutputResult = "";

                while ((serviceOutputResultPartial = inBR.readLine()) != null)
                {
                    serviceOutputResult = serviceOutputResult + serviceOutputResultPartial;
                }
                JSONObject jsonResponse = new JSONObject(serviceOutputResult);
                if (jsonResponse.getString("status").equals("CONFERMATO")){
                    String response = "Il tuo appuntamento per " + userData.get(user).get("prestazioneString") + " è stato confermato il " + date + " alle " + slot + "\nGrazie per aver usato il chatbot dei Servizi Online";
                    userData.remove(user);
                    return response;
                }

            }
            else{
                return "Errore di connessione con agenda";

            }
        } catch ( Exception e ) {
            return "Errore di connessione con agenda";
        }
        return null;
    }

//    public String chiediGeneralita(int step){
//        switch (String.valueOf(step)){
//            case "1":
//                return "${GENERALITA-1}Ti chiedo per favore di inserire il tuo nome";
//        }
//        return null;
//    }

//    @Override
//    public String takeAppuntamento (){
//        String tmpUrl = agendaTakeAppuntamentoApi+ "prestazione="+params.idPrestazioneBackoffice+"&userEmail="+richiesta?.mittente?.email+"&dataAppuntamento="+params.dataPrenotazione+"&nome="+nome+"&cognome="+cognome+"&interno=false&slot="+params.get("select-slot")+"&servizioRichiesta=ROL_"+richiesta.id;
//
//
//    }

    @Override
    public String findAppuntamento(String prestazioneText, String user) throws MalformedURLException {
        String[] prestazioniList = prestazioniSelect.split("-");

        String prompt = "Il primo appuntamento disponibile per ${prestazione} è il ${dataAppuntamento} alle ${oraAppuntamento}. Vuoi prenotarlo?" ;
        String codePrestazione = null;
        for (String prestazioneCode: prestazioniList){
            String[] prestazioneCode2 = prestazioneCode.split(";");
            if (StringUtils.containsIgnoreCase(prestazioneText, prestazioneCode2[0]) && StringUtils.containsIgnoreCase(prestazioneText, "AGND-" + prestazioneCode2[1])){
                prompt = prompt.replace("${prestazione}", prestazioneCode2[0]);
                codePrestazione = prestazioneCode2[1];
                userData.computeIfAbsent(user, k -> new LinkedHashMap<>());
                userData.get(user).put("prestazioneId", codePrestazione);
                userData.get(user).put("prestazioneString", prestazioneCode2[0]);
                break;
            }
        }
        if (prompt.contains("${prestazione}")){
            return "${ERRORE_RICONOSCIMENTO_PRESTAZIONE}";
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        String today = format.format(new Date());
        String urlString = agendaFindDateApi + "prestazioneId=" + codePrestazione + "&dataSelezionata=" + today;
        URL url = new URL(urlString);
        try {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            if(responseCode == 200){
                BufferedReader inBR = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String serviceOutputResultPartial = "";
                String serviceOutputResult = "";

                while ((serviceOutputResultPartial = inBR.readLine()) != null)
                {
                    serviceOutputResult = serviceOutputResult + serviceOutputResultPartial;
                }
                String date = "";
                String slot = "";
                JSONObject jsonResponse = new JSONObject(serviceOutputResult);
                for (Object element : jsonResponse.getJSONArray("slotsMaps")){
                    JSONObject jsonElement = (JSONObject) element;
                    if (!((JSONObject) jsonElement.get("slots")).isEmpty()){
                        date = jsonElement.getString("calculatedDate");
                        JSONObject slotsJson = (JSONObject) jsonElement.get("slots");
                        Iterator<String> slotsIterator = slotsJson.keys();
                        ArrayList<String> listSlots = new ArrayList<>();
                        while (slotsIterator.hasNext()) {
                            listSlots.add(slotsIterator.next());
                        }
                        Collections.sort(listSlots);
                        for (String key: listSlots){
                            if (slotsJson.getBoolean(key)){
                                slot = key;
                                break;
                            }
                        }
                    }
                }
                prompt = prompt.replace("${dataAppuntamento}",date);
                prompt = prompt.replace("${oraAppuntamento}", slot);
                userData.get(user).put("date", date);
                userData.get(user).put("slot", slot);
                return prompt;

            }
            else{
                return "Errore di connessione con agenda";

            }

        } catch ( Exception e ) {
            return null;
        }
    }

    @Override
    public String startChat() {
        OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));


        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("text-davinci-003")
                .temperature(1.0)
                .maxTokens(50)
                .prompt("riscrivi la frase 'Buongiorno, come posso aiutarti?' cambiando qualche parola.")
                .build();

        CompletionResult result= service.createCompletion(completionRequest);

        return result.getChoices().get(0).getText();
    }

    @Override
    public void cleanHistory(String user) {
        userData.remove(user);
    }
}
