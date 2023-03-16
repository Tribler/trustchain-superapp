FROM python:3.10 AS production

WORKDIR /

RUN mkdir -p /keys/
RUN git clone https://github.com/Tribler/py-ipv8.git pyipv8
RUN pip install -r pyipv8/requirements.txt
RUN touch __init__.py

RUN apt update
RUN apt install -y libsodium-dev

COPY requirements.txt /requirements.txt
RUN pip install -r /requirements.txt

ENTRYPOINT [ "python", "-u", "communitysim.py" ]
