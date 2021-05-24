#ifndef SELF_DNS_DES_DES_H
#define SELF_DNS_DES_DES_H

namespace self_dns {

#define DES_ENCRYPTION_MODE 0
#define DES_DECRYPTION_MODE 1

typedef struct {
  unsigned char k[8];
  unsigned char c[4];
  unsigned char d[4];
} key_set;

void generate_key(const unsigned char *key);
void generate_sub_keys(unsigned char *main_key, key_set *key_sets);
void process_message(unsigned char *message_piece,
                     unsigned char *processed_piece,
                     key_set *key_sets,
                     int mode);

void des_crypt(const unsigned char *src, const unsigned int size,
    const unsigned char *key, int cryptMode,
    unsigned char **outDst, unsigned int *outSize);

}  // namespace self_dns

#endif  // SELF_DNS_DES_DES_H
