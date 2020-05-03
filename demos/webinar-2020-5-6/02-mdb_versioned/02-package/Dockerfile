FROM python:3.7

COPY requirements_local.txt requirements.txt
RUN pip install -r requirements.txt
RUN python -m spacy download en_core_web_sm
RUN python -m spacy download xx_ent_wiki_sm

COPY model.pkl .
COPY predictor.py .

CMD ["python", "predictor.py"]
