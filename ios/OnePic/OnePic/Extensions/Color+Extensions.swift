import SwiftUI

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
    
    // Helper to accept Int literals commonly used (e.g. 0xFFFFFF without UInt cast)
    init(hex: Int, alpha: Double = 1) {
        self.init(hex: UInt(hex), alpha: alpha)
    }
    
    func lighter(by amount: CGFloat = 0.3) -> Color {
        let uiColor = UIColor(self)
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if uiColor.getHue(&h, saturation: &s, brightness: &b, alpha: &a) {
            return Color(UIColor(hue: h, saturation: s, brightness: min(b * (1 + amount), 1.0), alpha: a))
        }
        return self
    }
    
    func toMacaron() -> Color {
        let uiColor = UIColor(self)
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if uiColor.getHue(&h, saturation: &s, brightness: &b, alpha: &a) {
            // Macaron style: Low saturation (pastels) and High brightness
            // Keep Hue consistent with theme
            return Color(UIColor(hue: h, saturation: 0.55, brightness: 0.98, alpha: a))
        }
        return self
    }
}
