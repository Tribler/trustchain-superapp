import sys
import time
import libtorrent as lt
import os
ses = lt.session()
magnet = ""
ses.listen_on(6881, 6891)
h = ses.add_torrent(lt.parse_magnet_uri(magnet)) 
h.resume()

while True:
    

	s = h.status()
	state_str = ['queued', 'checking', 'downloading metadata', \
                  'downloading', 'finished', 'seeding', 'allocating', 'checking fastresume']

	print('\r%.2f%% complete (down: %.1f kb/s up: %.1f kB/s peers: %d) %s' % \
                  (s.progress * 100, s.download_rate / 1000, s.upload_rate / 1000, s.num_peers, state_str[s.state]))
	sys.stdout.flush()

	time.sleep(0.5)

    
