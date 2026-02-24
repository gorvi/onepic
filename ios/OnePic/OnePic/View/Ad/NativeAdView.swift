import SwiftUI
import GoogleMobileAds

/**
 * AdMobNativeAdView: 将 SDK 的 NativeAdView 封装为 SwiftUI 组件
 * 采用赛博全息风格的背景与边框
 * 适配 SDK V11+
 */
struct AdMobNativeAdView: View {
    let scene: AdManager.NativeScene
    private let adSlotHeight: CGFloat = 110
    @State private var nativeAd: NativeAd?
    @ObservedObject private var adManager = AdManager.shared
    
    @State private var isBreathing = false

    var body: some View {
        ZStack {
            if let ad = nativeAd {
                ZStack {
                    // 全息背景
                    RoundedRectangle(cornerRadius: 16)
                        .fill(.ultraThinMaterial.opacity(0.9)) // 增加不透明度以提升对比度
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(
                                    LinearGradient(
                                        colors: [Color.cyan.opacity(0.8), Color.purple.opacity(0.8)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 1.5
                                )
                                .opacity(isBreathing ? 1.0 : 0.4) // 呼吸效果
                                .shadow(color: Color.cyan.opacity(isBreathing ? 0.5 : 0.1), radius: isBreathing ? 8 : 2)
                        )

                    NativeAdRepresentable(nativeAd: ad)
                        .padding(12)
                }
                .onAppear {
                    withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                        isBreathing = true
                    }
                }
            } else {
                // 固定占位：避免广告加载完成后触发列表整体重排（iOS 15 抖动）
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.white.opacity(0.05))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                    )
                    .allowsHitTesting(false)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: adSlotHeight) // 对齐 Android 的紧凑型布局高度
        .transaction { tx in
            // 禁止广告内容切换导致父级布局动画，防止首页元素上下跳动
            tx.animation = nil
        }
        .onAppear {
            attemptToFetchAd()
        }
        .onReceive(adManager.nativeHomeReadyPublisher) { isReady in
            if scene == .home && isReady {
                attemptToFetchAd()
            }
        }
        .onReceive(adManager.nativeGalaxyReadyPublisher) { isReady in
            if scene == .galaxy && isReady {
                attemptToFetchAd()
            }
        }
        .onReceive(adManager.nativeHomeTokenPublisher) { _ in
            if scene == .home {
                attemptToFetchAd()
            }
        }
        .onReceive(adManager.nativeGalaxyTokenPublisher) { _ in
            if scene == .galaxy {
                attemptToFetchAd()
            }
        }
    }
    
    private func attemptToFetchAd() {
        guard nativeAd == nil else { return }
        
        // 尝试从 Manager 获取（获取即消耗，Manager 会自动开始加载下一个）
        if let ad = adManager.getNativeAd(for: scene) {
            // 这里不做动画，避免 iOS 15 将视图替换扩散为布局动画
            self.nativeAd = ad
        }
    }
}

/**
 * 内部封装 UIKit 的原生广告视图
 */
private struct NativeAdRepresentable: UIViewRepresentable {
    let nativeAd: NativeAd
    
    func makeUIView(context: Context) -> NativeAdView {
        let nativeView = NativeAdView()
        nativeView.backgroundColor = .clear
        return nativeView
    }
    
    func updateUIView(_ uiView: NativeAdView, context: Context) {
        if uiView.bounds.width > 0, uiView.bounds.height > 0 {
            configureIfNeeded(uiView, coordinator: context.coordinator)
        } else {
            // Some iOS 16/17 layout passes call update before size is resolved.
            // Re-try on next main-loop tick so ad content can still be attached.
            DispatchQueue.main.async { [nativeAd] in
                guard uiView.bounds.width > 0, uiView.bounds.height > 0 else { return }
                if context.coordinator.lastAdObjectID == ObjectIdentifier(nativeAd) { return }
                context.coordinator.lastAdObjectID = ObjectIdentifier(nativeAd)
                uiView.subviews.forEach { $0.removeFromSuperview() }
                self.buildSubviews(in: uiView)
                self.populate(uiView, with: nativeAd)
            }
        }
    }
    
    private func configureIfNeeded(_ uiView: NativeAdView, coordinator: Coordinator) {
        if coordinator.lastAdObjectID == ObjectIdentifier(nativeAd) { return }
        coordinator.lastAdObjectID = ObjectIdentifier(nativeAd)
        
        uiView.subviews.forEach { $0.removeFromSuperview() }
        buildSubviews(in: uiView)
        populate(uiView, with: nativeAd)
    }
    
    private func buildSubviews(in uiView: NativeAdView) {
        
        // 1. 标题 (Headline)
        let headline = UILabel()
        headline.font = .systemFont(ofSize: 16, weight: .bold)
        headline.textColor = .white
        uiView.headlineView = headline
        uiView.addSubview(headline)
        
        // 2. 描述 (Body)
        let body = UILabel()
        body.font = .systemFont(ofSize: 12)
        body.textColor = .white.withAlphaComponent(0.7)
        body.numberOfLines = 2
        uiView.bodyView = body
        uiView.addSubview(body)
        
        // 3. 图标 (Icon)
        let icon = UIImageView()
        icon.layer.cornerRadius = 8
        icon.clipsToBounds = true
        uiView.iconView = icon
        uiView.addSubview(icon)
        
        // 4. CTA
        let cta = GradientButton()
        cta.titleLabel?.font = .systemFont(ofSize: 14, weight: .bold)
        cta.setTitleColor(.white, for: .normal)
        cta.layer.cornerRadius = 16
        cta.clipsToBounds = true
        uiView.callToActionView = cta
        uiView.addSubview(cta)
        
        // 5. AdChoices
        let adChoices = AdChoicesView()
        uiView.adChoicesView = adChoices
        uiView.addSubview(adChoices)
        
        // 6. Ad label
        let adLabel = UILabel()
        adLabel.text = "Ad"
        adLabel.font = .systemFont(ofSize: 10, weight: .bold)
        adLabel.textColor = UIColor.white
        adLabel.backgroundColor = UIColor(hex: 0x00E676).withAlphaComponent(0.2)
        adLabel.layer.borderColor = UIColor(hex: 0x00E676).withAlphaComponent(0.5).cgColor
        adLabel.layer.borderWidth = 0.5
        adLabel.layer.cornerRadius = 2
        adLabel.clipsToBounds = true
        adLabel.textAlignment = .center
        uiView.addSubview(adLabel)
        
        // 7. 星级
        let starStack = UIStackView()
        starStack.axis = .horizontal
        starStack.spacing = 2
        starStack.alignment = .center
        uiView.starRatingView = starStack
        uiView.addSubview(starStack)
        
        headline.translatesAutoresizingMaskIntoConstraints = false
        body.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false
        cta.translatesAutoresizingMaskIntoConstraints = false
        adChoices.translatesAutoresizingMaskIntoConstraints = false
        adLabel.translatesAutoresizingMaskIntoConstraints = false
        starStack.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: uiView.leadingAnchor),
            icon.topAnchor.constraint(equalTo: uiView.topAnchor),
            icon.widthAnchor.constraint(equalToConstant: 36),
            icon.heightAnchor.constraint(equalToConstant: 36),
            
            adLabel.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 8),
            adLabel.topAnchor.constraint(equalTo: uiView.topAnchor, constant: 2),
            adLabel.widthAnchor.constraint(equalToConstant: 24),
            adLabel.heightAnchor.constraint(equalToConstant: 14),
            
            headline.leadingAnchor.constraint(equalTo: adLabel.trailingAnchor, constant: 6),
            headline.centerYAnchor.constraint(equalTo: adLabel.centerYAnchor),
            headline.trailingAnchor.constraint(lessThanOrEqualTo: adChoices.leadingAnchor, constant: -6),
            
            adChoices.topAnchor.constraint(equalTo: uiView.topAnchor),
            adChoices.trailingAnchor.constraint(equalTo: uiView.trailingAnchor),
            
            starStack.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 8),
            starStack.topAnchor.constraint(equalTo: adLabel.bottomAnchor, constant: 2),
            starStack.heightAnchor.constraint(equalToConstant: 12),
            
            body.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 8),
            body.trailingAnchor.constraint(equalTo: uiView.trailingAnchor),
            body.topAnchor.constraint(equalTo: starStack.bottomAnchor, constant: 2),
            
            cta.trailingAnchor.constraint(equalTo: uiView.trailingAnchor),
            cta.bottomAnchor.constraint(equalTo: uiView.bottomAnchor),
            cta.heightAnchor.constraint(equalToConstant: 28),
            cta.widthAnchor.constraint(greaterThanOrEqualToConstant: 72),
            cta.topAnchor.constraint(greaterThanOrEqualTo: body.bottomAnchor, constant: 6)
        ])
    }
    
    func makeCoordinator() -> Coordinator { Coordinator() }
    
    final class Coordinator {
        var lastAdObjectID: ObjectIdentifier?
    }
    
    private func populate(_ adView: NativeAdView, with nativeAd: NativeAd) {
        (adView.headlineView as? UILabel)?.text = nativeAd.headline
        (adView.bodyView as? UILabel)?.text = nativeAd.body
        (adView.iconView as? UIImageView)?.image = nativeAd.icon?.image
        (adView.callToActionView as? UIButton)?.setTitle(nativeAd.callToAction, for: .normal)
        
        // 处理星级
        if let starStack = adView.starRatingView as? UIStackView {
            starStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            if let rating = nativeAd.starRating {
                let ratingValue = rating.doubleValue
                // 简单的星星生成逻辑：满5星
                let fullStars = Int(ratingValue)
                let hasHalfStar = ratingValue - Double(fullStars) >= 0.5
                
                for _ in 0..<fullStars {
                    let iv = UIImageView(image: UIImage(systemName: "star.fill"))
                    iv.tintColor = .systemYellow
                    iv.contentMode = .scaleAspectFit
                    starStack.addArrangedSubview(iv)
                }
                if hasHalfStar {
                    let iv = UIImageView(image: UIImage(systemName: "star.leadinghalf.filled"))
                    iv.tintColor = .systemYellow
                    iv.contentMode = .scaleAspectFit
                    starStack.addArrangedSubview(iv)
                }
            }
            starStack.isHidden = (nativeAd.starRating == nil)
        }
        
        adView.nativeAd = nativeAd
    }
}

// 简单的渐变按钮类
class GradientButton: UIButton {
    override func layoutSubviews() {
        super.layoutSubviews()
        // 确保只有一个 gradient layer
        if layer.sublayers?.first is CAGradientLayer { return }
        
        let gradient = CAGradientLayer()
        gradient.colors = [
            UIColor(hex: 0x00E676).withAlphaComponent(0.8).cgColor,
            UIColor(hex: 0x00C853).withAlphaComponent(1.0).cgColor
        ]
        gradient.startPoint = CGPoint(x: 0, y: 0.5)
        gradient.endPoint = CGPoint(x: 1, y: 0.5)
        gradient.frame = bounds
        gradient.cornerRadius = 8 // 对齐 Android 的小圆角
        
        // 插入到最底层
        layer.insertSublayer(gradient, at: 0)
    }
}
