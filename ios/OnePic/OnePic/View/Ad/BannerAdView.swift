import SwiftUI
import GoogleMobileAds

/**
 * BannerAdView: 将 UIKit 的 BannerView 封装到 SwiftUI
 * 适配 SDK V11+
 */
struct BannerAdView: UIViewRepresentable {
    let adUnitID: String
    
    func makeUIView(context: Context) -> BannerView {
        // 使用 AdSizeBanner (SDK 13.0.0 Swift 直接导入)
        let bannerView = BannerView(adSize: AdSizeBanner)
        bannerView.adUnitID = adUnitID
        
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        if let root = windowScene?.windows.first?.rootViewController {
            bannerView.rootViewController = root
        }
        
        bannerView.load(Request())
        
        return bannerView
    }
    
    func updateUIView(_ uiView: BannerView, context: Context) {}
}

// 预览辅助
struct BannerAdView_Previews: PreviewProvider {
    static var previews: some View {
        BannerAdView(adUnitID: AdConfig.bannerHomeId)
            .frame(width: 320, height: 50)
            .previewLayout(.sizeThatFits)
    }
}
