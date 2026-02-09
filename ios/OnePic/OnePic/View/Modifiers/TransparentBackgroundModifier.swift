import SwiftUI
#if os(iOS)
import UIKit
#endif

struct TransparentBackgroundModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color.clear)
            #if os(iOS)
            .scrollContentBackground(.hidden)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbarBackground(.hidden, for: .tabBar)
            #endif
            .onAppear {
                #if os(iOS)
                // 强制去除 UIKit 组件的默认背景
                let appearance = UINavigationBarAppearance()
                appearance.configureWithTransparentBackground()
                appearance.backgroundColor = .clear
                UINavigationBar.appearance().standardAppearance = appearance
                UINavigationBar.appearance().scrollEdgeAppearance = appearance
                UINavigationBar.appearance().compactAppearance = appearance
                
                let tabRunning = UITabBarAppearance()
                tabRunning.configureWithTransparentBackground()
                tabRunning.backgroundColor = .clear
                UITabBar.appearance().standardAppearance = tabRunning
                if #available(iOS 15.0, *) {
                    UITabBar.appearance().scrollEdgeAppearance = tabRunning
                }
                #endif
            }
    }
}

extension View {
    func makeTransparentBackground() -> some View {
        self.modifier(TransparentBackgroundModifier())
    }
}
