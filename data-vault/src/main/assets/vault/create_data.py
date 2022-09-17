import os
from Crypto.Cipher import AES
from Crypto.Cipher import _create_cipher

mb500 = os.urandom(10 * 1000000)


secret = os.urandom(16)
crypto = AES.new(os.urandom(32), AES.MODE_CBC)
encrypted = crypto.encrypt(mb500)

with open("10MB_CBC.txt", "wb") as f:
    f.write(encrypted)