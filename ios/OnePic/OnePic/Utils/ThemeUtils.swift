import SwiftUI

struct ThemeUtils {
    static func getChapterColors(stageIndex: Int) -> [Color] {
        switch stageIndex {
        case 1: return [Color(hex: 0x1B5E20), Color(hex: 0x2E7D32), Color(hex: 0x43A047), Color(hex: 0x000000)] // Nature
        case 2: return [Color(hex: 0x000051), Color(hex: 0x1A237E), Color(hex: 0x0D47A1), Color(hex: 0x000000)] // Abyss
        case 3: return [Color(hex: 0xBF360C), Color(hex: 0xD84315), Color(hex: 0xFF5722), Color(hex: 0x3E2723)] // Fire
        case 4: return [Color(hex: 0x4A148C), Color(hex: 0x6A1B9A), Color(hex: 0x8E24AA), Color(hex: 0x311B92)] // Magic
        case 5: return [Color(hex: 0x3E2723), Color(hex: 0x4E342E), Color(hex: 0x5D4037), Color(hex: 0x000000)] // Ruins
        case 6: return [Color(hex: 0x01579B), Color(hex: 0x0277BD), Color(hex: 0x0288D1), Color(hex: 0xE1F5FE)] // Ice
        case 7: return [Color(hex: 0x263238), Color(hex: 0x37474F), Color(hex: 0x455A64), Color(hex: 0x000000)] // Mechanical
        case 8: return [Color(hex: 0x3E2723), Color(hex: 0x5D4037), Color(hex: 0x8D6E63), Color(hex: 0xFFFFD700)] // Time
        case 9: return [Color(hex: 0x000000), Color(hex: 0x12005E), Color(hex: 0x311B92), Color(hex: 0x000000)] // Cosmos
        case 10: return [Color(hex: 0x212121), Color(hex: 0x000000), Color(hex: 0x311B92), Color(hex: 0x000000)] // Void
        case 11: return [Color(hex: 0xF57F17), Color(hex: 0xFFB300), Color(hex: 0xFFCA28), Color(hex: 0xFFF8E1)] // Light
        case 12: return [Color(hex: 0x1A237E), Color(hex: 0x006064), Color(hex: 0x004D40), Color(hex: 0x880E4F)] // Unity
        default: return [Color(hex: 0x1A237E), Color(hex: 0x311B92), Color(hex: 0x0D47A1), Color(hex: 0x000000)] // Default/Intro
        }
    }
    
    static func getStageColors(stageIndex: Int) -> [Color] {
        let base = getChapterColors(stageIndex: stageIndex)
        if base.count >= 2 {
            return [base[0], base[1]]
        }
        return [Color(hex: 0x81C784), Color(hex: 0x66BB6A)]
    }
    
    static func getConnectorSymbol(stageIndex: Int) -> String {
        switch stageIndex % 6 {
        case 0: return "•"
        case 1: return "◦"
        case 2: return "•"
        case 3: return "✧"
        case 4: return "•"
        case 5: return "❄"
        default: return "•"
        }
    }
}
