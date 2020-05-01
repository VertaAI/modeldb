package codec

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"io"
)

var key = []byte(`
5e a9 7d 82 44 07 d5 b9 40 a7 0a d5 ca 59 e5 fb
7c b6 b0 24 29 23 91 28 29 e6 03 83 07 e1 42 83
41 bf ca 07 08 05 dc 4d 4c f0 05 2a e7 7c c2 a6
c8 c1 e7 13 e0 2d 3f 99 5e 60 4b e1 3a d5 df 4a
`)
var fixedKey = sha256.Sum256(key)

func Decode(token string, obj interface{}) error {
	nextBytes, err := base64.RawStdEncoding.DecodeString(token)
	if err != nil {
		return err
	}

	block, err := aes.NewCipher(fixedKey[:])
	if err != nil {
		return err
	}
	if len(token) < aes.BlockSize {
		return errors.New("token is too short")
	}

	iv := nextBytes[:aes.BlockSize]
	nextBytes = nextBytes[aes.BlockSize:]
	cfb := cipher.NewCFBDecrypter(block, iv)
	cfb.XORKeyStream(nextBytes, nextBytes)

	if err := json.Unmarshal(nextBytes, obj); err != nil {
		return err
	}

	return nil
}

func Encode(obj interface{}) string {
	dataBytes, err := json.Marshal(obj)
	if err != nil {
		panic(err)
	}

	block, err := aes.NewCipher(fixedKey[:])
	if err != nil {
		panic(err)
	}

	ciphertext := make([]byte, aes.BlockSize+len(dataBytes))
	iv := ciphertext[:aes.BlockSize]
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		panic(err)
	}
	cfb := cipher.NewCFBEncrypter(block, iv)
	cfb.XORKeyStream(ciphertext[aes.BlockSize:], []byte(dataBytes))

	s := base64.RawStdEncoding.EncodeToString(ciphertext)
	return s
}
