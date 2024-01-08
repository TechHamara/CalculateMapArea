from itertools import product
from string import ascii_lowercase
import os

def generate_proguard_dictionary(filename, word_length, num_words):
    """
    Gera um arquivo de dicionário para uso com ProGuard.

    :param filename: Nome do arquivo a ser gerado.
    :param word_length: Comprimento das palavras a serem geradas.
    :param num_words: Número de palavras a serem geradas.
    """

    # Gerar palavras usando combinações de letras
    words = [''.join(p) for p in product(ascii_lowercase, repeat=word_length)]

    # Limitar o número de palavras, se necessário
    if num_words < len(words):
        words = words[:num_words]

    # Escrever as palavras no arquivo
    with open(filename, 'w') as file:
        file.write('\n'.join(words))


# Configurações para os arquivos de dicionário
configurations = [
    ("proguard-class-dict.txt", 3, 1000),  # Classe: palavras de 3 letras, 1000 palavras
    ("proguard-dict.txt", 2, 500),         # Geral: palavras de 2 letras, 500 palavras
    ("proguard-package-dict.txt", 4, 1500) # Pacote: palavras de 4 letras, 1500 palavras
]

# Gerar os arquivos de dicionário
for config in configurations:
    generate_proguard_dictionary(*config)

# Verificando a existência dos arquivos gerados
os.listdir('.')  # Listar arquivos no diretório atual

