import SwiftUI
#if os(iOS)
import UIKit
#endif

struct TransparentBackgroundModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color.clear)
            .hideScrollContentBackgroundCompat()
            #if os(iOS)
            .modifier(CompatToolbarBackgroundModifier())
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

#if os(iOS)
private struct CompatToolbarBackgroundModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarBackground(.hidden, for: .tabBar)
        } else {
            content
        }
    }
}
#endif

extension View {
    func makeTransparentBackground() -> some View {
        self.modifier(TransparentBackgroundModifier())
    }
}
