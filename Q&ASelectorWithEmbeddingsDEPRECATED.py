from langchain.chains import LLMChain
from langchain.llms import OpenAI
from langchain.chat_models import ChatOpenAI
import certifi
import chardet


from langchain.document_loaders import WebBaseLoader
from langchain.document_loaders import TextLoader
from langchain.vectorstores import FAISS

from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.embeddings.openai import OpenAIEmbeddings
from langchain.chains import RetrievalQA


from langchain.prompts import PromptTemplate
from langchain.chains.combine_documents.stuff import StuffDocumentsChain

openai_api_key=""

url="https://www.milomb.camcom.it/faq-urp"
llm = ChatOpenAI(model='gpt-3.5-turbo-16k', temperature=0, openai_api_key=openai_api_key)


def summarize_text():
    loader = TextLoader('urpFaq.html', encoding="utf-8")
#    loader = WebBaseLoader(url)
    docs = loader.load()
    
    promptTemplate="""Estrai le domande e le risposte separate per argomento.
    
    {document}
    """
    prompt = PromptTemplate.from_template(promptTemplate)
    llm_chain = LLMChain(llm=llm, prompt=prompt)

    stuff_chain = StuffDocumentsChain(llm_chain=llm_chain, document_variable_name="document")
    
    print(stuff_chain.run(docs))


def findAnswer():

    loader = TextLoader('urpFaq_utf8.txt', encoding='utf8')
    documents = loader.load()
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)

    texts = text_splitter.split_documents(documents)

    embeddings = OpenAIEmbeddings(openai_api_key=openai_api_key)
    docsearch = FAISS.from_documents(documents, embeddings)


    qa = RetrievalQA.from_chain_type(llm=llm, chain_type="stuff", retriever=docsearch.as_retriever())
    
    while(True):
        query = input("Scrivi la tua domanda\n")
        response = qa.run(query)+"\n"
        print(response)
        
    
    
#summarize_text()
findAnswer()



#[Document(page_content='Request unsuccessful. Incapsula incident ID: 416000730004227221-8328383995843147', metadata={'source': 'https://www.milomb.camcom.it/faq-urp', 'language': 'No language found.'})]
