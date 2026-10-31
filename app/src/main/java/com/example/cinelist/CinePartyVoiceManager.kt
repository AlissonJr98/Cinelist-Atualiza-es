package com.example.cinelist

import android.content.Context
import android.media.AudioManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.nio.ByteBuffer

/**
 * Gerenciador de chamada de voz online em tempo real para salas de CineParty.
 * Utiliza WebRTC ponto a ponto (P2P com servidores Google STUN) com cancelamento
 * de eco acústico, supressão de ruído e sinalização via Firebase Firestore.
 */
class CinePartyVoiceManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var listenerOffer: ListenerRegistration? = null
    private var listenerAnswer: ListenerRegistration? = null
    private var listenerCandidates: ListenerRegistration? = null

    private val _conectado = MutableStateFlow(false)
    val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    private val _microfoneMutado = MutableStateFlow(false)
    val microfoneMutado: StateFlow<Boolean> = _microfoneMutado.asStateFlow()

    private val _autoFalanteLigado = MutableStateFlow(true)
    val autoFalanteLigado: StateFlow<Boolean> = _autoFalanteLigado.asStateFlow()

    private val _falandoRemoto = MutableStateFlow(false)
    val falandoRemoto: StateFlow<Boolean> = _falandoRemoto.asStateFlow()

    private var salaIdAtual: String = ""
    private var souCriadorOferta: Boolean = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private var inicializadoGlobal = false

        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
    }

    init {
        inicializarWebRtc()
    }

    private fun inicializarWebRtc() {
        try {
            if (!inicializadoGlobal) {
                val initOptions = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(initOptions)
                inicializadoGlobal = true
            }

            val options = PeerConnectionFactory.Options()
            factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Entra na sala de voz da CineParty e conecta com o outro participante.
     */
    fun conectarVoz(salaId: String, ehAnfitriao: Boolean) {
        if (salaId.isBlank()) return
        desconectarVoz()

        salaIdAtual = salaId
        souCriadorOferta = ehAnfitriao

        configurarAudioModoComunicacao()

        try {
            val f = factory ?: return
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            }
            audioSource = f.createAudioSource(audioConstraints)
            localAudioTrack = f.createAudioTrack("cine_audio_track_${System.currentTimeMillis()}", audioSource)
            localAudioTrack?.setEnabled(!_microfoneMutado.value)

            val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            peerConnection = f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            _conectado.value = true
                            _falandoRemoto.value = true
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        PeerConnection.IceConnectionState.FAILED,
                        PeerConnection.IceConnectionState.CLOSED -> {
                            _conectado.value = false
                            _falandoRemoto.value = false
                        }
                        else -> {}
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { enviarIceCandidate(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {}

                override fun onRemoveStream(stream: MediaStream?) {}

                override fun onDataChannel(channel: DataChannel?) {}

                override fun onRenegotiationNeeded() {}

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    _falandoRemoto.value = true
                }
            })

            localAudioTrack?.let {
                peerConnection?.addTrack(it, listOf("cine_stream_$salaId"))
            }

            iniciarSinalizacaoFirestore()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configurarAudioModoComunicacao() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = _autoFalanteLigado.value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun iniciarSinalizacaoFirestore() {
        val meuUid = auth.currentUser?.uid ?: "anom"
        val docSinalizacao = firestore.collection("cineparty_salas")
            .document(salaIdAtual)
            .collection("chamada_voz")

        if (souCriadorOferta) {
            // Cria a Oferta SDP
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            peerConnection?.createOffer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { sdp ->
                        peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                        val dadosOffer = mapOf(
                            "tipo" to sdp.type.canonicalForm(),
                            "sdp" to sdp.description,
                            "remetenteUid" to meuUid,
                            "timestamp" to System.currentTimeMillis()
                        )
                        docSinalizacao.document("offer").set(dadosOffer)
                    }
                }
            }, constraints)

            // Ouve a resposta (Answer)
            listenerAnswer = docSinalizacao.document("answer").addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    val sdpStr = snap.getString("sdp")
                    val tipoStr = snap.getString("tipo")
                    if (!sdpStr.isNullOrBlank() && tipoStr.equals("answer", ignoreCase = true)) {
                        val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, sdpStr)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), answerDesc)
                    }
                }
            }
        } else {
            // Ouve a Oferta (Offer) e responde com Answer
            listenerOffer = docSinalizacao.document("offer").addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    val sdpStr = snap.getString("sdp")
                    val remetente = snap.getString("remetenteUid")
                    if (!sdpStr.isNullOrBlank() && remetente != meuUid) {
                        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, sdpStr)
                        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                val constraints = MediaConstraints().apply {
                                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                                }
                                peerConnection?.createAnswer(object : SdpObserverAdapter() {
                                    override fun onCreateSuccess(desc: SessionDescription?) {
                                        desc?.let { answer ->
                                            peerConnection?.setLocalDescription(SdpObserverAdapter(), answer)
                                            val dadosAnswer = mapOf(
                                                "tipo" to answer.type.canonicalForm(),
                                                "sdp" to answer.description,
                                                "remetenteUid" to meuUid,
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            docSinalizacao.document("answer").set(dadosAnswer)
                                        }
                                    }
                                }, constraints)
                            }
                        }, offerDesc)
                    }
                }
            }
        }

        // Ouve candidatos ICE do outro peer
        listenerCandidates = docSinalizacao.document("candidatos").collection("lista")
            .addSnapshotListener { snaps, _ ->
                snaps?.documentChanges?.forEach { change ->
                    val data = change.document.data
                    val remetente = data["remetenteUid"] as? String
                    if (remetente != null && remetente != meuUid) {
                        val sdpMid = data["sdpMid"] as? String ?: ""
                        val sdpMLineIndex = (data["sdpMLineIndex"] as? Long)?.toInt() ?: 0
                        val sdp = data["sdp"] as? String ?: ""
                        if (sdp.isNotBlank()) {
                            val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                            peerConnection?.addIceCandidate(candidate)
                        }
                    }
                }
            }
    }

    private fun enviarIceCandidate(candidate: IceCandidate) {
        if (salaIdAtual.isBlank()) return
        val meuUid = auth.currentUser?.uid ?: "anom"
        val dados = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdp" to candidate.sdp,
            "remetenteUid" to meuUid,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("cineparty_salas")
            .document(salaIdAtual)
            .collection("chamada_voz")
            .document("candidatos")
            .collection("lista")
            .add(dados)
    }

    fun alternarMudo(): Boolean {
        val novoEstado = !_microfoneMutado.value
        _microfoneMutado.value = novoEstado
        localAudioTrack?.setEnabled(!novoEstado)
        return novoEstado
    }

    fun alternarAltoFalante(): Boolean {
        val novo = !_autoFalanteLigado.value
        _autoFalanteLigado.value = novo
        try {
            audioManager.isSpeakerphoneOn = novo
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return novo
    }

    fun desconectarVoz() {
        _conectado.value = false
        _falandoRemoto.value = false

        listenerOffer?.remove()
        listenerOffer = null
        listenerAnswer?.remove()
        listenerAnswer = null
        listenerCandidates?.remove()
        listenerCandidates = null

        try {
            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
