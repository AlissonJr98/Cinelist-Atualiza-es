const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

// 1. CHAT DE GRUPO
exports.notificarMensagemGrupo = onDocumentCreated("grupos/{grupoId}/chat_mensagens/{mensagemId}", async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const mensagem = snap.data();
  const grupoId = event.params.grupoId;
  if (!mensagem) return null;

  try {
    const grupoSnap = await admin.firestore().collection("grupos").doc(grupoId).get();
    let nomeGrupo = "Sala Compartilhada";
    if (grupoSnap.exists && grupoSnap.data().nomeGrupo) {
      nomeGrupo = grupoSnap.data().nomeGrupo;
    }

    const autor = mensagem.autorNome || "Alguém";
    const autorUid = mensagem.autorUid || "";
    // Suporte a Imagens/Figurinhas
    const textoMensagem = mensagem.tipoMensagem !== "TEXTO" ? "📷 Enviou um anexo/figurinha" : String(mensagem.texto);

    const message = {
      notification: {
        title: `💬 Nova mensagem em ${nomeGrupo}`,
        body: `${autor}: "${textoMensagem}"`,
      },
      data: {
        grupoId: grupoId,
        autorUid: autorUid,
        tipoAcao: "ABRIR_CHAT",
        alvoId: grupoId,
      },
      topic: `grupo_${grupoId}`,
    };

    return await admin.messaging().send(message);
  } catch (error) {
    console.error("Erro no chat de grupo:", error);
    return null;
  }
});

// 2. MÍDIA ADICIONADA AO GRUPO
exports.notificarMidiaAdicionada = onDocumentCreated("grupos/{grupoId}/midias_grupo/{midiaId}", async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const midia = snap.data();
  const grupoId = event.params.grupoId;
  if (!midia) return null;

  try {
    const grupoSnap = await admin.firestore().collection("grupos").doc(grupoId).get();
    let nomeGrupo = "Sala Compartilhada";
    if (grupoSnap.exists && grupoSnap.data().nomeGrupo) {
      nomeGrupo = grupoSnap.data().nomeGrupo;
    }

    const tituloMidia = midia.titulo || "Novo título";
    const adicionadoPorNome = midia.adicionadoPorNome || "Alguém";
    const autorUid = midia.adicionadoPorUid || "";

    const message = {
      notification: {
        title: `🍿 Nova sugestão em ${nomeGrupo}`,
        body: `${adicionadoPorNome} sugeriu "${tituloMidia}". Toque para ver e aceitar!`,
      },
      data: {
        grupoId: grupoId,
        autorUid: autorUid,
        tipoAcao: "ABRIR_MINHA_LISTA",
        alvoId: grupoId,
      },
      topic: `grupo_${grupoId}`,
    };

    return await admin.messaging().send(message);
  } catch (error) {
    console.error("Erro na mídia do grupo:", error);
    return null;
  }
});

// 3. CHAT PRIVADO / DIRECT COM AMIGO
exports.notificarChatPrivado = onDocumentCreated("chats/{chatId}/mensagens/{mensagemId}", async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const mensagem = snap.data();
  const chatId = event.params.chatId;
  if (!mensagem) return null;

  try {
    const remetenteUid = mensagem.remetenteUid || "";
    const uids = chatId.split("_");
    const destinatarioUid = uids.find(uid => uid !== remetenteUid);

    if (!destinatarioUid) return null;

    const remetenteDoc = await admin.firestore().collection("usuarios_publicos").doc(remetenteUid).get();
    const remetenteNome = remetenteDoc.exists && remetenteDoc.data().nome ? remetenteDoc.data().nome : "Amigo";

    // Suporte a Imagens/Figurinhas
    const textoPush = mensagem.tipoMensagem !== "TEXTO" ? "📷 Enviou um anexo/figurinha" : mensagem.texto;

    const message = {
      notification: {
        title: `💬 Mensagem de ${remetenteNome}`,
        body: textoPush,
      },
      data: {
        autorUid: remetenteUid,
        tipoAcao: "ABRIR_CHAT_PRIVADO",
        alvoId: remetenteUid,
      },
      topic: `user_${destinatarioUid}`,
    };

    return await admin.messaging().send(message);
  } catch (error) {
    console.error("Erro no chat privado:", error);
    return null;
  }
});

// 4. NOTIFICAR PEDIDO DE AMIZADE
exports.notificarPedidoAmizade = onDocumentCreated("usuarios/{uid}/solicitacoes/{remetenteUid}", async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const dados = snap.data();
  const destinatarioUid = event.params.uid;
  const remetenteNome = dados.remetenteNome || "Alguém";

  try {
    const message = {
      notification: {
        title: "👤 Novo Pedido de Conexão",
        body: `${remetenteNome} quer adicionar você como amigo.`,
      },
      data: {
        tipoAcao: "ABRIR_PERFIL",
        alvoId: destinatarioUid,
      },
      topic: `user_${destinatarioUid}`,
    };
    return await admin.messaging().send(message);
  } catch (error) {
    console.error("Erro no pedido de amizade:", error);
    return null;
  }
});

// 5. NOTIFICAR AMIZADE ACEITA
exports.notificarAmizadeAceita = onDocumentWritten("usuarios/{uid}/amigos/{amigoUid}", async (event) => {
  const snapAfter = event.data.after;
  if (!snapAfter.exists) return null;
  const dados = snapAfter.data();

  if (dados.status !== "aceito") return null;

  // Só notifica se antes não existia ou não era aceito
  const snapBefore = event.data.before;
  if (snapBefore.exists && snapBefore.data().status === "aceito") return null;

  const destinatarioUid = event.params.uid; // Aquele cuja lista de amigos recebeu um 'aceito'
  const amigoUid = event.params.amigoUid;   // Quem acabou de ser validado

  try {
    const amigoDoc = await admin.firestore().collection("usuarios_publicos").doc(amigoUid).get();
    const amigoNome = amigoDoc.exists && amigoDoc.data().nome ? amigoDoc.data().nome : "Alguém";

    const message = {
      notification: {
        title: "✅ Amizade Aceita!",
        body: `Você e ${amigoNome} agora estão conectados no CineList.`,
      },
      topic: `user_${destinatarioUid}`,
    };
    return await admin.messaging().send(message);
  } catch (error) {
    console.error("Erro ao notificar amizade aceita:", error);
    return null;
  }
});