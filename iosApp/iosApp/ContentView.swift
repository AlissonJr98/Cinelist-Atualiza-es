import SwiftUI
import SharedKit

struct ContentView: View {
    @State private var idiomaSelecionado = "pt"
    @State private var titulosPopulares: [String] = []
    @State private var carregando = false
    @State private var erroMsg: String? = nil
    
    let idiomas = ["pt", "en", "es", "fr", "de", "it", "ja", "ko"]
    private let tmdbClient = TmdbMultiplatformClient(client: PlatformHttpClient_iosKt.createPlatformHttpClient(), apiToken: TmdbMultiplatformClient.Companion.shared.DEFAULT_TMDB_TOKEN)
    
    var body: some View {
        ZStack {
            Color(red: 10/255, green: 15/255, blue: 28/255)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                Spacer().frame(height: 10)
                
                Image(systemName: "film.stack.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 60, height: 60)
                    .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                
                Text("CineList")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                
                Text(I18nCore.shared.traduzir(textoPt: "Minha Lista Pessoal", idiomaTag: idiomaSelecionado))
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                
                // Seletor de Idioma
                Picker("Idioma", selection: $idiomaSelecionado) {
                    ForEach(idiomas, id: \.self) { lang in
                        Text(lang.uppercased()).tag(lang)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 24)
                
                // Botão de Busca de Tendências via Ktor Multiplatform
                Button(action: buscarPopulares) {
                    HStack {
                        if carregando {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .black))
                        } else {
                            Image(systemName: "sparkles")
                            Text(I18nCore.shared.traduzir(textoPt: "Carregar Tendências (TMDB)", idiomaTag: idiomaSelecionado))
                        }
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 20)
                    .background(Color(red: 255/255, green: 215/255, blue: 0/255))
                    .cornerRadius(12)
                }
                .disabled(carregando)
                
                if let erro = erroMsg {
                    Text(erro)
                        .font(.system(size: 13))
                        .foregroundColor(.red)
                        .padding(.horizontal)
                }
                
                // Lista de Itens Carregados
                if !titulosPopulares.isEmpty {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(titulosPopulares, id: \.self) { titulo in
                                HStack {
                                    Image(systemName: "play.circle.fill")
                                        .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                                    Text(titulo)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(.white)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(Color(red: 20/255, green: 27/255, blue: 45/255))
                                .cornerRadius(8)
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .frame(maxHeight: 220)
                }
                
                Spacer()
                
                VStack(spacing: 4) {
                    Text(CineListSharedConfig.shared.descricao)
                        .font(.system(size: 12))
                        .foregroundColor(Color.gray)
                    
                    Text("Multiplataforma: Ktor 3 + Kotlin 2.2")
                        .font(.system(size: 11))
                        .foregroundColor(Color(red: 100/255, green: 116/255, blue: 139/255))
                }
                .padding(.bottom, 16)
            }
        }
    }
    
    private func buscarPopulares() {
        carregando = true
        erroMsg = nil
        Task {
            do {
                let resp = try await tmdbClient.getTrending(timeWindow: "week", language: idiomaSelecionado, page: 1)
                await MainActor.run {
                    self.titulosPopulares = resp.results.compactMap { $0.displayTitle }
                    self.carregando = false
                }
            } catch {
                await MainActor.run {
                    self.erroMsg = error.localizedDescription
                    self.carregando = false
                }
            }
        }
    }
}
