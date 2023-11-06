from langchain.chains import LLMChain
from langchain.llms import OpenAI

from langchain.chat_models import ChatOpenAI
from langchain.prompts import PromptTemplate
from langchain.memory import ConversationBufferMemory

import os
import requests
from datetime import date
from langchain.document_loaders import UnstructuredURLLoader
from langchain.docstore.document import Document
from unstructured.cleaners.core import remove_punctuation,clean,clean_extra_whitespace
from langchain.chains.summarize import load_summarize_chain
from langchain.document_loaders import WebBaseLoader
from langchain.tools import RequestsPostTool
from langchain.utilities import TextRequestsWrapper
from langchain.agents import load_tools
from langchain.agents import initialize_agent
from langchain.agents import Tool
from langchain.utilities import GoogleSearchAPIWrapper
import datetime as dt
from langchain.agents import AgentType
from langchain.agents.types import AGENT_TO_CLASS



import json

from langchain.chains.combine_documents.stuff import StuffDocumentsChain
from langchain.chains import APIChain

GOOGLE_CSE_ID = os.getenv('GOOGLE_CSE_ID', '')
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY', '')

llm = ChatOpenAI(temperature=0, openai_api_key="")

llm2 = OpenAI( temperature=0.7, openai_api_key="")

url="https://servizionline.milomb.camcom.it/front-agenda/"

request_wrapper = TextRequestsWrapper()
request = RequestsPostTool(requests_wrapper=request_wrapper)
search = GoogleSearchAPIWrapper(google_api_key=GOOGLE_API_KEY, google_cse_id=GOOGLE_CSE_ID)



descPost="""
Da usare per recuperare gli slot appuntamento disponibile per una data prestazione.
L'input deve essere un json composto da due chiavi: "url" e "data". 
"url" è l'url base in formato string, "data" è un dizionario chiave-valore dei parametri da passare alla chiamata POST.
Assicurati che l'input sia in formato json
L'output è il json ricevuto

http://vlsimi008.mi.cciaa.net:8080/core-agenda/
    
    ENDPOINTS: 
    POST /appuntamento/loadEnabledDaysDaily?prestazioneId=prestazioneId&dataSelezionata=dataSelezionata
    Ritorna una lista di slot appuntamento disponibili per data. 
    prestazioneId e dataSelezionata sono parametri da impostare
    Il parametro dataSelezionata è una data nel formato dd/MM/yyyy
    prestazioneId indica l'id della prestazione trovata
"""

def processThought(thought):
    return thought
   
                

# Tool(
        # name = "Search",
        # func=search.run,
        # description="Use this only for questions about dates"
    # )


toolkit = [  
    Tool(
        name = 'requests_post',
        func = request.run,
        description=descPost
        )
    # Tool(
        # name= "Thought Processing",
        # description= "Se pensi di poter ricevere un ParsingException, usa questo tool",
        # func=processThought,
    # )
]

def summarize_text():
    loader = WebBaseLoader(url)
    docs = loader.load()
    promptTemplate="""Elenca la lista dei servizi di prenotazione disponibili con una breve descrizione.
    
    {document}
    """
    prompt = PromptTemplate.from_template(promptTemplate)
    llm_chain = LLMChain(llm=llm, prompt=prompt)

    stuff_chain = StuffDocumentsChain(llm_chain=llm_chain, document_variable_name="document")
    print(stuff_chain.run(docs))

listPrestazioni = """
131 - Acquisto Firma Digitale/Carta Nazionale Servizi: Prenota un appuntamento per l'acquisto della firma digitale o della carta nazionale dei servizi presso una delle sedi disponibili.

160 - Firma Digitale Intermediari (Monza, Lodi, Legnano): Servizio dedicato agli intermediari incaricati alla registrazione per l'acquisto della firma digitale. È necessario prenotare un appuntamento per ogni firma digitale desiderata.

85 - Firma Digitale Intermediari - 1 Firma - Milano: Servizio dedicato agli intermediari incaricati alla registrazione per l'acquisto di un solo dispositivo di riconoscimento. Disponibile solo presso la sede di Milano.

129 - Firma Digitale Intermediari - Fino a 5 Firme - Milano: Servizio dedicato agli intermediari incaricati alla registrazione per l'acquisto di un solo dispositivo di riconoscimento. È possibile richiedere fino a 5 firme con un solo appuntamento. Disponibile solo presso la sede di Milano.

130 - Firma Digitale Intermediari - Fino a 10 Firme - Milano: Servizio dedicato agli intermediari incaricati alla registrazione per l'acquisto di un solo dispositivo di riconoscimento. È possibile richiedere fino a 10 firme con un solo appuntamento. Disponibile solo presso la sede di Milano.

14 - Informazioni per I.R. (Incaricati alla Registrazione) - Milano: Servizio dedicato agli intermediari incaricati alla registrazione per fornire informazioni e supporto. Disponibile presso la sede di Milano.

591 - Punto Impresa Digitale - Zoom 4.0: Prenota un appuntamento con i Digital Promoters del Punto Impresa Digitale per ricevere assistenza sui bandi in corso, informazioni sugli eventi di formazione e analisi della maturità digitale dell'impresa.

161 - Carnet ATA: Prenota un appuntamento per la consegna di un Carnet ATA presso una delle sedi disponibili.

227 - Visti e Documenti per l'Estero: Prenota un appuntamento per la richiesta di visti su documenti per l'estero.

157 - Carte tachigrafiche: Prenota un appuntamento per l'acquisto della Carta Tachigrafica.

165 - Certificati Registro Imprese: Prenota un appuntamento per la richiesta di certificati del Registro Imprese.

181 - Nulla-osta e parametri economici: Prenota un appuntamento per la richiesta di nulla osta per cittadini extracomunitari che vogliano svolgere un'attività economica.

144 - Proprietà Intellettuale: Prenota un appuntamento per il deposito di documentazione per marchi e brevetti.

147 - Protesti e ruoli abilitanti: Prenota un appuntamento per l'accettazione di pratiche per protesti e ruoli abilitanti.

18 - SPID: Prenota un appuntamento per l'attivazione di un'utenza SPID presso la Camera di Commercio.

202 - Ufficio Relazioni con il Pubblico: Prenota un appuntamento con l'Ufficio Relazioni con il pubblico.

168 - Vendita carta filigranata e bollini: Prenota un appuntamento per l'acquisto di carta filigranata e bollini.

152 - Vidimazioni libri e formulari trasporto rifiuti: Prenota un appuntamento per la vidimazione di libri sociali e formulari trasporto rifiuti.

152 - Vidimazioni registri carico/scarico rifiuti: Prenota un appuntamento per la vidimazione dei registri carico/scarico rifiuti.

107 - Punto Impresa Digitale - Online check-up: Prenota un appuntamento per l'Online check-up, un servizio di primo orientamento sull'efficienza digitale dell'impresa.

107 - Desk orientamento digitale: Prenota un appuntamento per ricevere supporto sull'accesso e il funzionamento delle principali piattaforme digitali.

76 - Attivazione Firma Remota - Milano: Prenota un appuntamento per l'attivazione della firma remota presso la sede di Milano.

314 - InfoPoint Transizione Energetica: Prenota un appuntamento per ricevere supporto sulle tematiche dell'efficienza energetica.

24) Albo Gestori Ambientali - Iscrizione categorie 2 e 3 bis - LODI: Servizio di supporto all'iscrizione alle categorie 2 e 3 bis dell'Albo telematico gestori ambientali presso la sede di Lodi.
"""

def trovaSlotPrestazione():    
    today = date.today()
    dateFormat = today.strftime("%d/%m/%Y")    
    
    promptTemplate="""
    LISTA PRESTAZIONI:
    {prestazioni}.
    
    Trova il primo slot appuntamento disponibile per la prestazione più adatta alla richiesta dell'utente e ritorna le informazioni in un json composto da:
    nomePrestazione, dataAppuntamento, slotOraAppuntamento, idPrestazione
    La data odierna è {data}.

    UTENTE:
    {utente}
    """
    
    template2="""
        RISPOSTA UTENTE: {rispostaUtente}
        
        Valuta se la risposta dell'utente è una risposta affermativa o negativa.
        Se è positiva ritorna "AFFIRMATIVE" altrimenti "NEGATIVE"   
    """
    
    analysis=""
    while (analysis!="AFFIRMATIVE"):
        utenteString = input("Di cosa hai bisogno?\n")
        promptTemplate = promptTemplate.replace("{prestazioni}", listPrestazioni)
        promptTemplate = promptTemplate.replace("{utente}",utenteString)
        promptTemplate = promptTemplate.replace("{data}",dateFormat)
        agent = initialize_agent(toolkit, llm2, agent="zero-shot-react-description", verbose=True, return_intermediate_steps=True)
        response = agent({"input": promptTemplate})
        agentResponse = (response["output"])
        print(agentResponse)
        jsonResponse = json.loads(agentResponse)
        print("Il primo appuntamento disponibile per il servizio " + jsonResponse["nomePrestazione"] + " è il " + jsonResponse["dataAppuntamento"] + " alle ore " + jsonResponse["slotOraAppuntamento"])
        userResponse = input("Vuoi continuare con la prenotazione?\n")
        
        # prompt = PromptTemplate(
            # input_variables=["rispostaUtente"],
            # template=template2,
        # )
        # llm_chain = LLMChain(llm=llm, prompt=prompt)
        # analysis = llm_chain.predict(rispostaUtente=userResponse)
        # print(analysis)
    return agentResponse
    



def prendiAppuntamento(response):

    template = """
        INFORMAZIONI_APPUNTAMENTO:
        dataAppuntamento={dataAppuntamento},
        slotOraAppuntamento={slot},
        idPrestazione={idPrestazione}.
        
        Sei un chatbot che deve chiedere delle informazioni ad un utente.
        Inizia tu salutando l'utente.
        Non dire di essere un chatbot.
        Le informazioni da recuperare sono:
        Nome ,
        Cognome,
        Email.
        Chiedi un informazione alla volta.
        Una volta finito il recupero, completa l'url http://vlsimi008.mi.cciaa.net:8080/core-agenda/appuntamentoApi/takeAppuntamentoFromDate
        impostando i parametri dalle informazioni che possiedi:
        prestazione è l'id della prestazione,
        userEmail,
        dataAppuntamento,
        nome,
        cognome,
        interno è sempre = false,
        slot è lo slot ora appuntamento,
        servizioRichiesta è sempre = ROL_2551.
        finisci con BREAK: url
        
        {chat_history}
        Utente: {human_input}
        Chatbot:"""
    
    jsonResponse = json.loads(response)
    template = template.replace("{dataAppuntamento}", jsonResponse["dataAppuntamento"])
    template = template.replace("{slot}", jsonResponse["slotOraAppuntamento"])
    template = template.replace("{idPrestazione}", jsonResponse["idPrestazione"])
    prompt = PromptTemplate(
        input_variables=["chat_history", "human_input",], 
        template=template
    )
   
    memory = ConversationBufferMemory(memory_key="chat_history")
    
    llm_chain = LLMChain(
    llm=llm, 
    prompt=prompt, 
    verbose=False, 
    memory=memory)
    
    userString="saluta l'utente e comincia a chiedere le informazioni"
    chatbot=""
    while("BREAK" not in chatbot):
        chatbot=(llm_chain.predict(human_input=userString))
        print(chatbot)
        userString = input()
    


response = trovaSlotPrestazione()
prendiAppuntamento(response)

