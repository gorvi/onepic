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
    @ObservedObject private var adManager = AdManager.shared
    
    @State private var isBreathing = false

    var body: some View {
        Group {
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
                .frame(maxWidth: .infinity)
                .frame(height: 110) // 对齐 Android 的紧凑型布局高度
                .onAppear {
                    withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                        isBreathing = true
                    }
                }
            } else {
                Color.clear.frame(height: 1)
            }
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
    }
    
    private func attemptToFetchAd() {
        guard nativeAd == nil else { return }
        
        // 尝试从 Manager 获取（获取即消耗，Manager 会自动开始加载下一个）
        if let ad = adManager.getNativeAd(for: scene) {
            withAnimation {
                self.nativeAd = ad
            }
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
        icon.layer.cornerRadius = 8 // 更圆润
        icon.clipsToBounds = true
        nativeView.iconView = icon
        nativeView.addSubview(icon)
        
        // 4. 操作按钮 (Call to Action) - 渐变胶囊风格
        let cta = GradientButton() // 使用自定义渐变按钮
        cta.titleLabel?.font = .systemFont(ofSize: 14, weight: .bold)
        cta.setTitleColor(.white, for: .normal)
        cta.layer.cornerRadius = 16
        cta.clipsToBounds = true
        
        nativeView.callToActionView = cta
        nativeView.addSubview(cta)
        
        // 5. AdChoices View (必选)
        let adChoices = AdChoicesView()
        nativeView.adChoicesView = adChoices
        nativeView.addSubview(adChoices)
        
        // 6. 广告标识 (Ad Label)
        let adLabel = UILabel()
        adLabel.text = "Ad"
        adLabel.font = .systemFont(ofSize: 10, weight: .bold)
        adLabel.textColor = UIColor.white
        adLabel.backgroundColor = UIColor(hex: 0x00E676).withAlphaComponent(0.2) // 对齐 Android: #22000000 但带绿色调
        adLabel.layer.borderColor = UIColor(hex: 0x00E676).withAlphaComponent(0.5).cgColor
        adLabel.layer.borderWidth = 0.5
        adLabel.layer.cornerRadius = 2
        adLabel.clipsToBounds = true
        adLabel.textAlignment = .center
        nativeView.addSubview(adLabel)

        // 7. 星级评分 (Star Rating)
        let starStack = UIStackView()
        starStack.axis = .horizontal
        starStack.spacing = 2
        starStack.alignment = .center
        nativeView.starRatingView = starStack
        nativeView.addSubview(starStack)

        // 布局
        headline.translatesAutoresizingMaskIntoConstraints = false
        body.translatesAutoresizingMaskIntoConstraints = false
        icon.translatesAutoresizingMaskIntoConstraints = false
        cta.translatesAutoresizingMaskIntoConstraints = false
        adChoices.translatesAutoresizingMaskIntoConstraints = false
        adLabel.translatesAutoresizingMaskIntoConstraints = false
        starStack.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: nativeView.leadingAnchor),
            icon.topAnchor.constraint(equalTo: nativeView.topAnchor),
            icon.widthAnchor.constraint(equalToConstant: 48),
            icon.heightAnchor.constraint(equalToConstant: 48),
            
            // Ad Label 放在 Icon 右侧顶部
            adLabel.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 12),
            adLabel.topAnchor.constraint(equalTo: nativeView.topAnchor, constant: 4),
            adLabel.widthAnchor.constraint(equalToConstant: 28),
            adLabel.heightAnchor.constraint(equalToConstant: 16),

            // Headline 紧跟 Ad Label
            headline.leadingAnchor.constraint(equalTo: adLabel.trailingAnchor, constant: 6),
            headline.centerYAnchor.constraint(equalTo: adLabel.centerYAnchor),
            headline.trailingAnchor.constraint(lessThanOrEqualTo: adChoices.leadingAnchor, constant: -8),
            
            // AdChoices 放置在右上角
            adChoices.topAnchor.constraint(equalTo: nativeView.topAnchor),
            adChoices.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            
            // Star Rating (放在 Headline 下方)
            starStack.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 10),
            starStack.topAnchor.constraint(equalTo: adLabel.bottomAnchor, constant: 4),
            starStack.heightAnchor.constraint(equalToConstant: 14),

            // Body
            body.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 10),
            body.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            // 如果有星星，body 放在星星下面；否则放在 adLabel 下面 (将在 populate 中动态调整 constraint，这里给个默认)
            body.topAnchor.constraint(equalTo: starStack.bottomAnchor, constant: 4),
            
            // CTA Button
            cta.trailingAnchor.constraint(equalTo: nativeView.trailingAnchor),
            cta.bottomAnchor.constraint(equalTo: nativeView.bottomAnchor),
            cta.heightAnchor.constraint(equalToConstant: 32),
            cta.widthAnchor.constraint(greaterThanOrEqualToConstant: 80),
            cta.topAnchor.constraint(greaterThanOrEqualTo: body.bottomAnchor, constant: 8)
        ])
        
        // 绑定数据
        populate(nativeView, with: nativeAd)
        
        return nativeView
    }
    
    func updateUIView(_ uiView: NativeAdView, context: Context) {}
    
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
