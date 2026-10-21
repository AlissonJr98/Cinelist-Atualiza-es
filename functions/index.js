const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Utilitário para coletar tokens FCM dos membros de uma sala (excluindo autor)
 */
async function obterTokensDosMembros(grupoId, autorUid) {
  try {
    const membrosSnap = await admin.firestore().collection("grupos").doc(grupoId).collection("membros").get();
    const uids = [];
    membrosSnap.forEach(doc => {
      if (doc.id !== autorUid) {
        uids.push(doc.id);
      }
    });
    if (uids.length === 0) return [];

    const tokens = [];
    for (let i = 0; i < uids.length; i += 10) {
      const slice = uids.slice(i, i + 10);
      const refs = slice.map(uid => admin.firestore().collection("usuarios_publicos").doc(uid));
      const docs = await admin.firestore().getAll(...refs);
      docs.forEach(d => {
        if (d.exists && d.data() && d.data().fcmToken) {
          tokens.push(d.data().fcmToken);
        }
      });
    }
    return [...new Set(tokens)];
  } catch (err) {
    console.error("Erro ao obter tokens dos membros:", err);
    return [];
  }
}

/**
 * Utilitário para buscar o token FCM de um usuário específico
 */
async function obterTokenUsuario(uid) {
  try {
    const doc = await admin.firestore().collection("usuarios_publicos").doc(uid).get();
    if (doc.exists && doc.data() && doc.data().fcmToken) {
      return doc.data().fcmToken;
    }
  } catch (err) {
    console.error("Erro ao obter token do usuario:", err);
  }
  return null;
}

/**
 * Envia notificação com prioridade máxima para Android (acorda o dispositivo no modo Doze / app fechado)
 */
async function enviarNotificacaoAltaPrioridade({ token, tokens, topic, title, body, data, channelId }) {
  const canal = channelId || "canal_salas_compartilhadas";

  const androidConfig = {
    priority: "high",
    ttl: 86400 * 1000, // 24 horas
    notification: {
      channelId: canal,
      priority: "max",
      defaultSound: true,
      defaultVibrateTimings: true,
      visibility: "public",
    },
  };

  const payloadNotification = {
    title: title,
    body: body,
  };

  const sanitizedData = {};
  if (data) {
    for (const [key, value] of Object.entries(data)) {
      sanitizedData[key] = value != null ? String(value) : "";
    }
  }

  // 1. Envio Direto Multicast (dispositivos dos membros)
  if (tokens && tokens.length > 0) {
    try {
      const resp = await admin.messaging().sendEachForMulticast({
        tokens: tokens,
        notification: payloadNotification,
        data: sanitizedData,
        android: androidConfig,
      });
      console.log(`Multicast FCM enviado para ${tokens.length} tokens. Sucesso: ${resp.successCount}, Falhas: ${resp.failureCount}`);
      return resp;
    } catch (e) {
      console.error("Erro no envio multicast FCM:", e);
    }
  }

  // 2. Envio Direto para Token Único
  if (token) {
    try {
      const resp = await admin.messaging().send({
        token: token,
        notification: payloadNotification,
        data: sanitizedData,
        android: androidConfig,
      });
      console.log("Notificação FCM de alta prioridade enviada para token direto.");
      return resp;
    } catch (e) {
      console.error("Erro no envio direto para token FCM:", e);
    }
  }

  // 3. Fallback para Tópico
  if (topic) {
    try {
      const resp = await admin.messaging().send({
        topic: topic,
        notification: payloadNotification,
        data: sanitizedData,
        android: androidConfig,
      });
      console.log(`Notificação FCM enviada para tópico ${topic}.`);
      return resp;
    } catch (e) {
      console.error(`Erro no envio para tópico FCM ${topic}:`, e);
    }
  }

  return null;
}

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
    const textoMensagem = mensagem.tipoMensagem !== "TEXTO" ? "📷 Enviou um anexo/figurinha" : String(mensagem.texto || "");

    const tokens = await obterTokensDosMembros(grupoId, autorUid);

    return await enviarNotificacaoAltaPrioridade({
      tokens: tokens.length > 0 ? tokens : null,
      topic: tokens.length === 0 ? `grupo_${grupoId}` : null,
      title: `💬 Nova mensagem em ${nomeGrupo}`,
      body: `${autor}: "${textoMensagem}"`,
      channelId: "canal_salas_compartilhadas",
      data: {
        grupoId: grupoId,
        autorUid: autorUid,
        autorNome: autor,
        tipoAcao: "ABRIR_CHAT",
        alvoId: grupoId,
        tipo: "CHAT_GRUPO",
      },
    });
  } catch (error) {
    console.error("Erro no chat de grupo:", error);
    return null;
  }
});

// 2. MÍDIA ADICIONADA AO GRUPO (CORRIGIDO: midias/{midiaId})
exports.notificarMidiaAdicionada = onDocumentCreated("grupos/{grupoId}/midias/{midiaId}", async (event) => {
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

    const tokens = await obterTokensDosMembros(grupoId, autorUid);

    return await enviarNotificacaoAltaPrioridade({
      tokens: tokens.length > 0 ? tokens : null,
      topic: tokens.length === 0 ? `grupo_${grupoId}` : null,
      title: `🍿 Nova sugestão em ${nomeGrupo}`,
      body: `${adicionadoPorNome} sugeriu "${tituloMidia}". Toque para ver e aceitar!`,
      channelId: "canal_salas_compartilhadas",
      data: {
        grupoId: grupoId,
        autorUid: autorUid,
        autorNome: adicionadoPorNome,
        tipoAcao: "ABRIR_MINHA_LISTA",
        alvoId: grupoId,
        tipo: "MIDIA_ADICIONADA",
        titulo: tituloMidia,
      },
    });
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

    const textoPush = mensagem.tipoMensagem !== "TEXTO" ? "📷 Enviou um anexo/figurinha" : String(mensagem.texto || "");

    const tokenDestinatario = await obterTokenUsuario(destinatarioUid);

    return await enviarNotificacaoAltaPrioridade({
      token: tokenDestinatario,
      topic: !tokenDestinatario ? `user_${destinatarioUid}` : null,
      title: `💬 Mensagem de ${remetenteNome}`,
      body: textoPush,
      channelId: "canal_salas_compartilhadas",
      data: {
        autorUid: remetenteUid,
        autorNome: remetenteNome,
        tipoAcao: "ABRIR_CHAT_PRIVADO",
        alvoId: remetenteUid,
        tipo: "CHAT_PRIVADO",
      },
    });
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
  const remetenteUid = event.params.remetenteUid;

  try {
    const tokenDestinatario = await obterTokenUsuario(destinatarioUid);

    return await enviarNotificacaoAltaPrioridade({
      token: tokenDestinatario,
      topic: !tokenDestinatario ? `user_${destinatarioUid}` : null,
      title: "👤 Novo Pedido de Conexão",
      body: `${remetenteNome} quer adicionar você como amigo.`,
      channelId: "canal_solicitacoes_amizade",
      data: {
        autorUid: remetenteUid,
        autorNome: remetenteNome,
        tipoAcao: "ABRIR_PERFIL",
        alvoId: destinatarioUid,
        tipo: "SOLICITACAO_AMIZADE",
      },
    });
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

  const snapBefore = event.data.before;
  if (snapBefore.exists && snapBefore.data().status === "aceito") return null;

  const destinatarioUid = event.params.uid;
  const amigoUid = event.params.amigoUid;

  try {
    const amigoDoc = await admin.firestore().collection("usuarios_publicos").doc(amigoUid).get();
    const amigoNome = amigoDoc.exists && amigoDoc.data().nome ? amigoDoc.data().nome : "Alguém";

    const tokenDestinatario = await obterTokenUsuario(destinatarioUid);

    return await enviarNotificacaoAltaPrioridade({
      token: tokenDestinatario,
      topic: !tokenDestinatario ? `user_${destinatarioUid}` : null,
      title: "✅ Amizade Aceita!",
      body: `Você e ${amigoNome} agora estão conectados no CineList.`,
      channelId: "canal_solicitacoes_amizade",
      data: {
        autorUid: amigoUid,
        autorNome: amigoNome,
        tipoAcao: "ABRIR_PERFIL",
        alvoId: destinatarioUid,
        tipo: "AMIZADE_ACEITA",
      },
    });
  } catch (error) {
    console.error("Erro ao notificar amizade aceita:", error);
    return null;
  }
});

// 6. NOTIFICAR NOVA ENQUETE NA SALA
exports.notificarEnqueteCriada = onDocumentCreated("grupos/{grupoId}/enquetes/{enqueteId}", async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const enquete = snap.data();
  const grupoId = event.params.grupoId;
  if (!enquete) return null;

  try {
    const grupoSnap = await admin.firestore().collection("grupos").doc(grupoId).get();
    let nomeGrupo = "Sala Compartilhada";
    if (grupoSnap.exists && grupoSnap.data().nomeGrupo) {
      nomeGrupo = grupoSnap.data().nomeGrupo;
    }

    const criadorNome = enquete.criadorNome || "Alguém";
    const criadorUid = enquete.criadorUid || "";
    const tituloEnquete = enquete.titulo || "O que vamos assistir hoje?";

    const tokens = await obterTokensDosMembros(grupoId, criadorUid);

    return await enviarNotificacaoAltaPrioridade({
      tokens: tokens.length > 0 ? tokens : null,
      topic: tokens.length === 0 ? `grupo_${grupoId}` : null,
      title: `📊 Nova Votação em ${nomeGrupo}`,
      body: `${criadorNome} abriu uma enquete: "${tituloEnquete}". Dê seu voto!`,
      channelId: "canal_salas_compartilhadas",
      data: {
        grupoId: grupoId,
        autorUid: criadorUid,
        autorNome: criadorNome,
        tipoAcao: "ABRIR_MINHA_LISTA",
        alvoId: grupoId,
        tipo: "ENQUETE_CRIADA",
      },
    });
  } catch (error) {
    console.error("Erro ao notificar enquete:", error);
    return null;
  }
});
