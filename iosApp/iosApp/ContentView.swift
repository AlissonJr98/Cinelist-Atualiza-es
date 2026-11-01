import SwiftUI
import SharedKit

struct ContentView: View {
    @State private var idiomaSelecionado = "pt"
    let idiomas = ["pt", "en", "es", "fr", "de", "it", "ja", "ko"]
    
    var body: some View {
        ZStack {
            Color(red: 10/255, green: 15/255, blue: 28/255)
                .ignoresSafeArea()
            
            VStack(spacing: 24) {
                Spacer()
                
                Image(systemName: "film.stack.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 80, height: 80)
                    .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                
                Text("CineList")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.white)
                
                Text(I18nCore.shared.traduzir(textoPt: "Minha Lista Pessoal", idiomaTag: idiomaSelecionado))
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                
                Text(I18nCore.shared.traduzir(textoPt: "Descubra filmes, séries e animes com amigos", idiomaTag: idiomaSelecionado))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(Color(red: 160/255, green: 174/255, blue: 192/255))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                // Seletor de Idioma para testar a reatividade multiplataforma
                Picker("Idioma", selection: $idiomaSelecionado) {
                    ForEach(idiomas, id: \.self) { lang in
                        Text(lang.uppercased()).tag(lang)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 24)
                
                Spacer()
                
                VStack(spacing: 6) {
                    Text(CineListSharedConfig.shared.descricao)
                        .font(.system(size: 13))
                        .foregroundColor(Color.gray)
                    
                    Text("Versão Shared: " + CineListSharedConfig.shared.VERSAO_SHARED)
                        .font(.system(size: 12))
                        .foregroundColor(Color(red: 100/255, green: 116/255, blue: 139/255))
                }
                .padding(.bottom, 20)
            }
        }
    }
}
