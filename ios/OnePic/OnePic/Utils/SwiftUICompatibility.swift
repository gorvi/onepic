import SwiftUI

enum AppRuntimePolicy {
    // Restore full ad path across supported iOS versions.
    static var supportsNativeAds: Bool {
        true
    }
    
    // Realtime timeline/driver updates are disabled on older systems to reduce layout jitter.
    static var supportsRealtimeAnimationDriver: Bool {
        if #available(iOS 16.0, *) { return true }
        return false
    }
    
    static var supportsVisitorOverlay: Bool {
        supportsRealtimeAnimationDriver
    }
    
    static var supportsFloatingBuffWindow: Bool {
        true
    }
    
    static var useLazyHomeList: Bool {
        if #available(iOS 16.0, *) { return true }
        return false
    }
}

enum StabilityDiagnostics {
    static var enableJitterLogs = false
    
    static func jitter(_ scope: String, _ message: String) {
        guard enableJitterLogs else { return }
        print("[JITTER][\(scope)] \(message)")
    }
}

struct CompatNavigationContainer<Content: View>: View {
    @ViewBuilder var content: () -> Content
    
    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack { content() }
        } else {
            NavigationView { content() }
                .navigationViewStyle(StackNavigationViewStyle())
        }
    }
}

extension View {
    @ViewBuilder
    func onChangeCompat<Value: Equatable>(
        of value: Value,
        perform action: @escaping (Value) -> Void
    ) -> some View {
        if #available(iOS 17.0, *) {
            self.onChange(of: value) { _, newValue in
                action(newValue)
            }
        } else {
            self.onChange(of: value, perform: action)
        }
    }
    
    @ViewBuilder
    func hideNavigationBarCompat() -> some View {
        if #available(iOS 16.0, *) {
            self.toolbar(.hidden, for: .navigationBar)
        } else {
            self.navigationBarHidden(true)
        }
    }
    
    @ViewBuilder
    func hideTabBarCompat() -> some View {
        if #available(iOS 16.0, *) {
            self.toolbar(.hidden, for: .tabBar)
        } else {
            self
        }
    }
    
    @ViewBuilder
    func hideScrollContentBackgroundCompat() -> some View {
        if #available(iOS 16.0, *) {
            self.scrollContentBackground(.hidden)
        } else {
            self
        }
    }
    
    @ViewBuilder
    func navigationDestinationCompat<Destination: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder destination: @escaping () -> Destination
    ) -> some View {
        if #available(iOS 16.0, *) {
            self.navigationDestination(isPresented: isPresented, destination: destination)
        } else {
            self.background(
                NavigationLink(
                    destination: destination(),
                    isActive: isPresented,
                    label: { EmptyView() }
                )
                .hidden()
            )
        }
    }
}
