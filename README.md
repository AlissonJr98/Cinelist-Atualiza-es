<div align="center">

# 🎬 CineList

**O ecossistema definitivo e futurista para gerenciar, descobrir e maratonar Filmes, Séries e Animes.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

<br/>

[✨ Funcionalidades](#-funcionalidades-principais) •
[📱 Demonstração](#-design--experiência-do-usuário) •
[🛠️ Tecnologias](#-arquitetura-e-stack-tecnológica) •
[🚀 Instalação](#-como-instalar-e-executar) •
[🔄 Atualizações OTA](#-atualizações-over-the-air-ota)

---

</div>

## 🌟 Visão Geral

O **CineList** redefine a experiência de organizar e compartilhar filmes, séries e animes. Desenvolvido inteiramente com tecnologia moderna em **Jetpack Compose** e **Kotlin**, o aplicativo combina uma estética *Cyberpunk Neon Dark* com ferramentas sociais avançadas, controle detalhado de maratonas e sincronização instantânea em nuvem via **Firebase**.

---

## ✨ Funcionalidades Principais

### 📺 Gerenciamento Inteligente de Mídias
- **Listas Personalizadas**: Separe seus títulos em *Quero Assistir*, *Assistindo*, *Concluídos*, *Abandonados* e *Favoritos*.
- **Controle de Episódios e Temporadas**: Acompanhe o progresso de cada série ou anime com um toque. Ao terminar a última temporada ou episódio, o app detecta e conclui automaticamente seu título.
- **Detecção de Episódios Filler**: Alertas e identificação clara de fillers em animes clássicos e modernos, garantindo que você nunca perca tempo com episódios desnecessários.
- **Calculadora de Maratona e Horário Real**: Saiba com precisão quantas horas restam para concluir uma série e veja o horário exato de término baseado no seu ritmo atual.

### 🎬 Detalhes Estendidos & Dados Oficiais do Cinema
- **Classificação Indicativa Oficial**: Exibição dos selos oficiais por faixa etária (Livre, 10, 12, 14, 16 e 18 anos) com a padronização oficial de cores e descritores detalhados de conteúdo (violência, conteúdo sensível, drogas).
- **Bilheteria Mundial & Orçamento**: Painel estatístico com custo de produção (budget), arrecadação global nas bilheterias (revenue) e cálculo inteligente de rentabilidade (*🏆 Blockbuster*, *✨ Sucesso Comercial*, *📊 Lucrativo*).
- **Equipe Criativa & Direção**: Conheça os Diretores, Roteiristas, Criadores da Série e Compositores da trilha sonora através de chips interativos.
- **Onde Assistir Completo**: Saiba onde assistir em streaming por assinatura (Netflix, Max, Disney+, Prime Video, Apple TV+), plataformas 100% grátis (Pluto TV, NetMovies, Runtime) e catálogo de **Aluguel e Compra Digital**.
- **Vídeos Extras, Teasers e Bastidores**: Assista a trailers, teasers promocionais, featurettes e vídeos de bastidores diretamente no app.
- **Cinemas: Em Cartaz & Em Breve**: Acompanhe os filmes em exibição nas salas de cinema e confira as próximas estreias agendadas.

### 🎙️ Áudio & Dublagem Brasileira (PT-BR)
- **Identificação Visual Instantânea & Resolução em Segundo Plano**: Saiba imediatamente se a obra possui dublagem com selos dinâmicos (`🎙️ PT-BR`, `🇧🇷 Áudio Original`, `💬 Legendado` ou `❓ A Confirmar`). Pré-carregamento em lote via Firestore e verificação assíncrona automática em segundo plano para todos os cards da lista (verticais e horizontais) sem precisar entrar nos detalhes.
- **Ficha de Dublagem Brasileira Completa**: Consulte os estúdios oficiais (*Delart, TV Group, Wan Macher, Dublavídeo, Universal Cinergia, Álamo, Unidub*), a direção de dublagem e o elenco completo com os dubladores brasileiros de cada personagem, integrado à API da **Dublapédia (MediaWiki)** e **CineAI (Gemini)**.
- **Perfil Interativo de Vozes Brasileiras**: Toque em qualquer dublador para abrir sua ficha profissional completa com avatar, papel no título atual, personagens mais icônicos em animações/filmes e botão de busca instantânea no catálogo.
- **Validação Colaborativa em Nuvem**: Contribuição comunitária com 1 toque (`✓ Dublado` / `💬 Legendado`) sincronizada em tempo real via **Firestore**.
- **Filtro na Roleta Cibernética & CineMatch**: Sorteie ou vote exclusivamente em títulos que possuem dublagem em português.

### 🎲 Decisão Rápida: Roleta & CineMatch
- **Roleta Cibernética**: Não sabe o que assistir? Filtre por tempo disponível (ex: `< 90 min`), provedores de streaming (Netflix, Disney+, Max, Prime Video), status ou **apenas títulos dublados**, e deixe o CineList sortear para você.
- **CineMatch (Modo Casal & Amigos)**: Conecte-se com um amigo ou parceiro(a), filtre por dublagem, filmes ou séries, deslize pelos títulos sugeridos e descubra o que ambos têm em comum para assistir juntos hoje.
- **Álbum de Matches & Torneio 1v1**: Reviva todos os títulos decididos em conjunto ou dispute mata-mata direto entre duas obras favoritas da sala.
- **CineParty Ao Vivo & Ligação de Voz WebRTC**: Assista conteúdos sincronizados e converse por áudio em tempo real com amigos conectados.
  - **Chamada Instantânea com Notificação de Sistema**: Chame amigos ausentes diretamente da sala CineParty; o convidado recebe uma notificação prioritária no aparelho (com toque, vibração e botões de ação rápida "Entrar 🍿" e "Recusar ❌") que abre a sessão instantaneamente.
  - **Presença Global e Status Online em Tempo Real**: Heartbeat ativo de segundo plano a cada 25 segundos mantendo o status "Online agora" sempre fiel e sincronizado entre dispositivos conectados.
  - **Ordenação Inteligente de Amigos**: Abas de amigos e participantes destacam no topo quem está realmente online no momento.

### 📊 Estatísticas Avançadas & DNA Cinematográfico
- **DNA Cinematográfico (Gêneros Dominantes)**: Gráfico interativo no perfil com a distribuição percentual dos seus gêneros favoritos e barras de progresso neon personalizadas.
- **Economia de Tempo em Fillers**: Contador exclusivo de tempo economizado pulando episódios fillers em animes clássicos e sazonais.
- **Importação e Exportação Flexível**: Suporte a backups em JSON e importação/exportação via **CSV compatível com Letterboxd, IMDb, TV Time e CineList**.

### 📱 Experiência Visual & Temas Futuristas
- **Modo AMOLED Void (#000000)**: Preto puro absoluto que desliga os pixels de telas OLED para máxima economia de bateria e imersão total.
- **Paletas Cinematográficas**: Temas personalizados com tons de Deep Space Blue, Titanium Slate, Cyberpunk Neon e Light Clean.

### 🌐 Feed Social & Comunidade
- **Feed Comunitário**: Compartilhe suas análises, avaliações (1 a 5 estrelas), teorias e recomendações.
- **Vitrine de Favoritos & Séries do Amigo**: Acesse o perfil público de qualquer amigo conectado e explore suas séries, filmes e animes favoritos com filtros rápidos. Cada título exibe o status na sua lista e um botão de adição rápida com 1 toque (`+ Adicionar à Minha Lista`) com feedback háptico.
- **Afinidade Cinematográfica (% Match de Gosto)**: Medidor visual inteligente que calcula a compatibilidade de gosto entre você e seu amigo baseado em obras e gêneros em comum.
- **CineParty Live com Voz & Permissões Unificadas**: Chamadas de áudio ao vivo com verificação proativa de microfone, anel holográfico pulsante para indicar participantes falando e solicitação unificada de permissões essenciais ao iniciar o app.
- **Busca Global de Amigos**: Encontre amigos e cinéfilos instantaneamente por nome, e-mail ou código de usuário.
- **Perfis Conectados com Google**: Sincronização automática da foto do perfil da conta Google e atualizações de nome em tempo real.
- **CineCode (QR Code)**: Compartilhe seu perfil ou mídias favoritas instantaneamente gerando e escaneando QR Codes personalizados.

### 🌍 Internacionalização (8 Idiomas)
O CineList detecta e se adapta dinamicamente ao idioma do dispositivo, oferecendo suporte nativo para:
- 🇧🇷 Português (Brasil)
- 🇺🇸 Inglês (English)
- 🇪🇸 Espanhol (Español)
- 🇫🇷 Francês (Français)
- 🇩🇪 Alemão (Deutsch)
- 🇮🇹 Italiano (Italiano)
- 🇯🇵 Japonês (日本語)
- 🇰🇷 Coreano (한국어)

---

## 🎨 Design & Experiência do Usuário

- **Interface Cyberpunk Imersiva**: Tons de azul escuro espacial (`#0B0E17`), contrastando com destaques em ciano elétrico (`#00E5FF`), roxo cósmico (`#8B5CF6`) e amarelo cinema (`#FACC15`).
- **Animações Fluidas (Spring & Shimmer)**: Transições suaves orientadas por física de mola e efeitos táteis a cada interação.
- **Design Totalmente Responsivo**: Layout dinâmico que se adapta perfeitamente a qualquer resolução, densidade de tela e proporção (Xiaomi, Samsung, Motorola, Pixel e outros).

---

## 🛠️ Arquitetura e Stack Tecnológica

O projeto adota as melhores práticas recomendadas pela Google para desenvolvimento moderno no ecossistema Android:

```
app/
 ├── data/               # Modelos locais e remotos, DAOs e entidades Room
 ├── repository/         # Camada de abstração de dados (Room + Retrofit + Firebase)
 ├── viewmodel/          # Estados reativos com StateFlow / SharedFlow e Coroutines
 ├── ui/
 │    ├── components/    # Componentes reutilizáveis (Banners, Cards, Dialogs)
 │    ├── screens/       # Telas Compose (Home, Detalhes, Perfil, Social, Roleta)
 │    └── theme/         # Paleta de cores, tipografia e formas dinâmicas
 └── utils/              # Tradução (I18n), calculadoras e utilitários OTA
```

### Tecnologias Utilizadas
- **Linguagem**: [Kotlin 2.0](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3
- **Banco de Dados Local**: [Room Database](https://developer.android.com/training/data-storage/room) com cache offline completo
- **Backend & Cloud**: [Firebase Authentication](https://firebase.google.com/products/auth) & [Cloud Firestore](https://firebase.google.com/products/firestore)
- **APIs de Metadados**: [The Movie Database (TMDB) API](https://www.themoviedb.org/documentation/api)
- **Rede & Imagens**: [OkHttp3](https://square.github.io/okhttp/) + [Retrofit](https://square.github.io/retrofit/) + [Coil Compose](https://coil-kt.github.io/coil/)
- **Injeção de Dependências & Concorrência**: Kotlin Coroutines & Kotlin Flows

---

## 🔄 Atualizações Over-The-Air (OTA)

O CineList possui um sistema integrado de atualizações automáticas via **GitHub Releases**:
1. Ao iniciar, o app verifica silenciosamente a existência de versões mais recentes via `versao.json` e API do GitHub.
2. Uma janela cósmica holográfica apresenta as novidades e melhorias reais da nova versão.
3. O download do APK é realizado diretamente no app com barra de progresso em tempo real e abertura assistida do instalador de pacotes do Android.

---

## 🚀 Como Instalar e Executar

### Pré-requisitos
- **Android Studio** Ladybug | 2024.2+ ou superior
- **JDK 17** configurado
- Dispositivo ou emulador Android rodando **Android 7.0 (API 24)** ou superior

### Passos de Instalação
1. Clone o repositório:
   ```bash
   git clone https://github.com/AlissonJr98/Cinelist-Atualiza-es.git
   cd Cinelist-Atualiza-es
   ```

2. Configure sua chave da API do TMDB:
   Crie ou edite o arquivo `local.properties` na raiz do projeto:
   ```properties
   tmdb.token=SEU_TOKEN_TMDB_AQUI
   ```

3. Configure o Firebase:
   Certifique-se de que o arquivo `google-services.json` está posicionado na pasta `app/`.

4. Compile e execute o projeto no Android Studio ou via terminal:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 Licença

Este projeto é desenvolvido para amantes do cinema e da cultura pop. Distribuído sob a licença MIT. Consulte o arquivo `LICENSE` para mais informações.

<div align="center">
  <sub>Criado com 🍿 e 💜 por <b>Alisson Junior</b> e comunidade cinéfila.</sub>
</div>
