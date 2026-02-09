import SwiftUI

struct ContentView: View {
    @State private var isIntroFinished = false
    
    var body: some View {
        if isIntroFinished {
            MainTabView()
                .transition(.opacity)
        } else {
            IntroView(onStartJourney: {
                withAnimation {
                    isIntroFinished = true
                }
            })
        }
    }
}
