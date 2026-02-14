import SwiftUI
import Combine
import GoogleMobileAds

struct SplashAdView: View {
    @State private var ad: NativeAd?
    @State private var timeRemaining = 5
    @Binding var isPresented: Bool
    
    // Timer to countdown
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            // 1. Background (App Launch Style)
            Color.black.ignoresSafeArea()
            
            // 2. Ad Content
            if let ad = ad {
                VStack(spacing: 24) {
                    Spacer()
                    
                    // Ad Container
                    ZStack(alignment: .topLeading) {
                        // Media View (Main Image/Video)
                        NativeAdGenericView(nativeAd: ad)
                            .frame(height: 300)
                            .cornerRadius(12)
                            .clipped()
                        
                        // Ad Label (Compliance)
                        Text("Ad")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(4)
                            .padding(8)
                    }
                    .padding(.horizontal, 24)
                        
                    // Headline & Body
                    HStack(alignment: .top, spacing: 12) {
                        if let icon = ad.icon?.image {
                            Image(uiImage: icon)
                                .resizable()
                                .frame(width: 48, height: 48)
                                .cornerRadius(8)
                        }
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(ad.headline ?? "")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                            
                            Text(ad.body ?? "")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                                .lineLimit(2)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 24)
                    
                    // Call to Action
                    if let cta = ad.callToAction {
                        Text(cta)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.blue)
                            .cornerRadius(25)
                            .padding(.horizontal, 24)
                    }
                    
                    Spacer()
                    
                    // App Logo / Bottom Branding
                    HStack {
                        Image("AppIcon") // If available in assets
                            .resizable()
                            .frame(width: 24, height: 24)
                        Text("OnePic")
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundColor(.white.opacity(0.6))
                    }
                    .padding(.bottom, 40)
                }
            } else {
                // Fallback / Loading State (Should be short)
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
            }
            
            // 3. Skip Button (Top Right)
            VStack {
                HStack {
                    Spacer()
                    Button(action: {
                        dismissAd()
                    }) {
                        HStack(spacing: 4) {
                            Text("Skip")
                            Text("\(timeRemaining)s")
                                .monospacedDigit()
                        }
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.4))
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.white.opacity(0.2), lineWidth: 0.5)
                        )
                    }
                    .padding(.top, 60) // Safe Area
                    .padding(.trailing, 20)
                }
                Spacer()
            }
        }
        .onAppear {
            // Get the preloaded ad
            self.ad = AdManager.shared.nativeAdOpen
            if self.ad == nil {
               // If ad not ready, dismiss immediately to avoid blocking user
               print("⚠️ SplashAdView: No ad ready, dismissing...")
               dismissAd()
            }
        }
        .onReceive(timer) { _ in
            if timeRemaining > 0 {
                timeRemaining -= 1
            } else {
                dismissAd()
            }
        }
    }
    
    private func dismissAd() {
        // Stop ad audio/video if needed?
        isPresented = false
    }
}

// Wrapper for GADNativeAdView
struct NativeAdGenericView: UIViewRepresentable {
    let nativeAd: NativeAd
    
    func makeUIView(context: Context) -> NativeAdView {
        let adView = NativeAdView()
        
        // Media View
        let mediaView = MediaView()
        mediaView.translatesAutoresizingMaskIntoConstraints = false
        adView.addSubview(mediaView)
        adView.mediaView = mediaView
        
        NSLayoutConstraint.activate([
            mediaView.topAnchor.constraint(equalTo: adView.topAnchor),
            mediaView.leadingAnchor.constraint(equalTo: adView.leadingAnchor),
            mediaView.trailingAnchor.constraint(equalTo: adView.trailingAnchor),
            mediaView.bottomAnchor.constraint(equalTo: adView.bottomAnchor)
        ])
        
        // Populate
        adView.nativeAd = nativeAd
        
        return adView
    }
    
    func updateUIView(_ uiView: NativeAdView, context: Context) {
        // No updates needed usually
    }
}
