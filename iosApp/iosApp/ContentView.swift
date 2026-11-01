import SwiftUI
import SharedKit

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(red: 10/255, green: 15/255, blue: 28/255)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                Image(systemName: "film.stack.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 80, height: 80)
                    .foregroundColor(Color(red: 255/255, green: 215/255, blue: 0/255))
                
                Text("CineList")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.white)
                
                Text(CineListSharedConfig.shared.descricao)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color(red: 160/255, green: 174/255, blue: 192/255))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                Text("Versão Shared: " + CineListSharedConfig.shared.VERSAO_SHARED)
                    .font(.system(size: 14))
                    .foregroundColor(Color.gray)
            }
        }
    }
}
