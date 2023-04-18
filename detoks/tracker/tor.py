import sys
import time
import libtorrent as lt
import os
ses = lt.session()

ses.listen_on(6881, 6891)

import socket
sesss = []
HOST = "127.0.0.1"  # Standard loopback interface address (localhost)
PORT = 8082  # Port to listen on (non-privileged ports are > 1023)
import threading
def thread_function():
  with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
      s.bind((HOST, PORT))
      s.listen()
      while True:
        conn, addr = s.accept()
        with conn:
            print(f"Connected by {addr}")
            data = conn.recv(1024)
            print(data.decode('latin-1'))
            if data:
                  h = ses.add_torrent(lt.parse_magnet_uri("magnet:?xt=urn:btih:"+data.decode('latin-1')+"&tr=http%3a%2f%2flocalhost%3a8080%2fannounce&tr=http%3a%2f%2fopen.acgnxtracker.com%3a80%2fannounce")) 
                  h.resume()
                  sesss.append(h)
            
                # conn.sendall(data)

x = threading.Thread(target=thread_function)

x.start()
while True:
              print(sesss)
              for s in sesss:

                s = s.status()
                state_str = ['queued', 'checking', 'downloading metadata', \
                  'downloading', 'finished', 'seeding', 'allocating', 'checking fastresume']

                print('\r%.2f%% complete (down: %.1f kb/s up: %.1f kB/s peers: %d) %s' % \
                  (s.progress * 100, s.download_rate / 1000, s.upload_rate / 1000, s.num_peers, state_str[s.state]))
                sys.stdout.flush()

              time.sleep(0.5)

    
