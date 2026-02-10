import SwiftUI
import GoogleMobileAds

/**
 * AdMobNativeAdView: 将 SDK 的 NativeAdView 封装为 SwiftUI 组件
 * 采用赛博全息风格的背景与边框
 * 适配 SDK V11+
 */
struct AdMobNativeAdView: View {
    let scene: AdManager.NativeScene
    @State private var nativeAd: NativeAd?
    
    var body: some View {
        Group {
            if let ad = nativeAd {
                ZStack {
                    // 全息背景
                    RoundedRectangle(cornerRadius: 16)
                        .fill(.ultraThinMaterial)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(
                                    LinearGradient(
                                        colors: [Color.cyan.opacity(0.6), Color.purple.opacity(0.6)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 1
                                )
                        )
                    
                    NativeAdRepresentable(nativeAd: ad)
                        .padding(12)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 120) // 适配横幅风格
            } else {
                EmptyView()
            }
        }
        .onAppear {
            self.nativeAd = AdManager.shared.getNativeAd(for: scene)
        }
    }
}

/**
 * 内部封装 UIKit 的原生广告视图
 */
private struct NativeAdRepresentable: UIViewRepresentable {
    let nativeAd: NativeAd
    
    func makeUIView(context: Context) -> GoogleMobileAds.NativeAdView {
        let nativeView = GoogleMobileAds.NativeAdView()
        
        // 1. 标题 (Headline)
        let headline = UILabel()
        headline.font = .systemFont(ofSize: 16, weight: .bold)
        headline.textColor = .white
        nativeView.headlineView = headline
        nativeView.addSubview(headline)
        
        // 2. 描述 (Body)
        let body = UILabel()
        body.font = .systemFont(ofSize: 12)
        body.textColor = .white.withAlphaComponent(0.7)
        body.numberOfLines = 2
        nativeView.bodyView = body
        nativeView.addSubview(body)
        
        // 3. 图标 (Icon)
        let icon = UIImageView()
        icon.layer.cornerRadius = 4
        icon.clipsToBounds = true
        nativeView.iconView = icon
        nativeView.addSubview(icon)
        
        // 4. 操作按钮 (Call to Action)
        let cta = UIButton(type: .system)
        cta.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        cta.setTitleColor(.cyan, for: .normal)
        cta.layer.borderWidth = 1
        cta.layer.borderColor = UIColor.cyan.withAlphaComponent(0.5).cgColor
        cta.layer.cornerRadius = 14
        
        // 适配 iOS 15+ 的 UIButton.Configuration，修复 contentEdgeInsets 弃用警告
        var config = UIButton.Configuration.plain()
        config.contentInsets = NSDirectionalEdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12)
        cta.configuration = config
        
        nativeView.callToActionView = cta
        nativeView.addSubview(cta)
        
        // 布局 (简单 Autolayout)
        headline.translatesAutoresizingMaskIntoConstraints = false
        body.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false
        cta.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: nativeView.leadingAnchor),
            icon.topAnchor.constraint(equalTo: nativeView.topAnchor),
            icon.widthAnchor.constraint(equalToConstant: 44),
            icon.heightAnchor.constraint(equalToConstant: 44),
            
            headline.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 12),
            headline.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            headline.topAnchor.constraint(equalTo: nativeView.topAnchor),
            
            body.leadingAnchor.constraint(equalTo: headline.leadingAnchor),
            body.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            body.topAnchor.constraint(equalTo: headline.bottomAnchor, constant: 4),
            
            cta.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            cta.bottomAnchor.constraint(equalTo: nativeView.bottomAnchor),
            cta.topAnchor.constraint(greaterThanOrEqualTo: body.bottomAnchor, constant: 8)
        ])
        
        // 5. 绑定数据
        populate(nativeView, with: nativeAd)
        
        return nativeView
    }
    
    func updateUIView(_ uiView: GoogleMobileAds.NativeAdView, context: Context) {}
    
    private func populate(_ adView: GoogleMobileAds.NativeAdView, with nativeAd: NativeAd) {
        (adView.headlineView as? UILabel)?.text = nativeAd.headline
        (adView.bodyView as? UILabel)?.text = nativeAd.body
        (adView.iconView as? UIImageView)?.image = nativeAd.icon?.image
        (adView.callToActionView as? UIButton)?.setTitle(nativeAd.callToAction, for: .normal)
        
        adView.nativeAd = nativeAd
    }
}
