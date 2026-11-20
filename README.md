# LV Viagem

App Android (Kotlin + Jetpack Compose) com Filmes, Músicas e Jogos.

Lê de `/storage/emulated/0/LVviagem/`:

```
LVviagem/
  filmes/
    Meu Filme/   (qualquer nome .mp4 + qualquer nome .jpg)
  musicas/
    Minha Musica/  (qualquer nome .mp3 + qualquer nome .jpg)
  jogos/
    Meu Jogo/      (index.html + arquivos do jogo HTML5)
```

## Gerar o APK
1. Suba todo o conteúdo desta pasta (incluindo `.github`) para um repositório no GitHub.
2. Aba **Actions** → **Build APK** (roda sozinho no push, ou clique em *Run workflow*).
3. Ao terminar, baixe o artefato **LVViagem-apk** (contém `app-debug.apk`).

O app é 100% offline (sem permissão de internet). As pastas são criadas automaticamente se não existirem.

Jogos embutidos (offline): Snake, Jogo da Velha, 2048, Memória, Voo, Quebra-Blocos e Corrida. Jogos HTML5 extras vão em `LVviagem/jogos`.
