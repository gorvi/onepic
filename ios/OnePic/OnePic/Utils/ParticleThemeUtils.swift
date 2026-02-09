import SwiftUI

// MARK: - Enums
enum ParticleStyle {
    case float // Gentle drift (Default)
    case rise  // Upwards (Fire, Magic)
    case fall  // Downwards (Nature, Ice)
    case orbit // Slight rotation
}

enum ParticleShape {
    case circle
    case ring
    case square
    case triangle
    case diamond
    case hexagon
    case star
}

// MARK: - Config Data
struct ParticleThemeConfig {
    let colors: [Color]
    let speedMultiplier: Double
    let sizeBase: CGFloat
    let count: Int
    let style: ParticleStyle
    let shape: ParticleShape
    
    init(
        colors: [Color],
        speedMultiplier: Double = 1.0,
        sizeBase: CGFloat = 100.0,
        count: Int = 10,
        style: ParticleStyle = .float,
        shape: ParticleShape = .circle
    ) {
        self.colors = colors
        self.speedMultiplier = speedMultiplier
        self.sizeBase = sizeBase
        self.count = count
        self.style = style
        self.shape = shape
    }
}

// MARK: - Theme Factory
class ParticleThemeUtils {
    static func getParticleTheme(theme: String) -> ParticleThemeConfig {
        switch theme {
        case "light": // 光芒 - 性能优化
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFFFF176), Color(hex: 0xFFFFFFFF)],
                speedMultiplier: 0.12,
                count: 6, // Reduced from 10
                shape: .circle
            )
        case "nature": // 林间 - 性能优化
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFC8E6C9), Color(hex: 0xFFF1F8E9)],
                speedMultiplier: 0.4,
                count: 5, // Reduced from 8
                style: .fall,
                shape: .circle
            )
        case "abyss": // 深渊
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFF0288D1), Color(hex: 0xFF00ACC1)],
                speedMultiplier: 0.3,
                sizeBase: 80,
                count: 4, // Reduced from 5
                shape: .circle
            )
        case "fire": // 烈焰
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFFF7043), Color(hex: 0xFFFFAB91)],
                speedMultiplier: 0.6,
                sizeBase: 60,
                count: 4, // Reduced from 5
                style: .rise,
                shape: .circle
            )
        case "magic": // 魔法
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFBA68C8), Color(hex: 0xFFE1BEE7)],
                speedMultiplier: 0.35,
                count: 4, // Reduced from 6
                shape: .circle
            )
        case "ruins": // 废墟
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFBCAAA4), Color(hex: 0xFFD7CCC8)],
                speedMultiplier: 0.2,
                sizeBase: 100.0,
                count: 3,
                style: .float,
                shape: .circle
            )
        case "ice": // 冻土
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFB3E5FC), Color(hex: 0xFFFFFFFF)],
                speedMultiplier: 0.3,
                sizeBase: 100.0,
                count: 3, // Reduced from 4
                style: .fall,
                shape: .circle
            )
        case "mechanical": // 机械
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFF4DD0E1), Color(hex: 0xFFB2EBF2)],
                speedMultiplier: 0.4,
                sizeBase: 90,
                count: 3, // Reduced from 4
                shape: .circle
            )
        case "time": // 时间
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFFFD54F), Color(hex: 0xFFFFF9C4)],
                speedMultiplier: 0.25,
                count: 3, // Reduced from 4
                shape: .circle
            )
        case "cosmos": // 星辰 - 马卡龙焕新
            return ParticleThemeConfig(
                colors: [
                    Color(hex: 0xFFB2EBF2), // 马卡龙蓝
                    Color(hex: 0xFFFFC1CC), // 樱花粉
                    Color(hex: 0xFFB2FBDA), // 薄荷绿
                    Color(hex: 0xFFFFF176)  // 柠檬黄
                ],
                speedMultiplier: 0.25,
                count: 8,
                shape: .circle
            )

        default: // 默认亮丽配色
            return ParticleThemeConfig(
                colors: [Color(hex: 0xFFB2EBF2), Color(hex: 0xFFFFC1CC)],
                speedMultiplier: 0.3,
                count: 6,
                shape: .circle
            )
        }
    }
}
