import Foundation
import SwiftUI

enum ImageSource {
    case resource(name: String)
    case asset(path: String)
    case url(url: URL)
    case generated
}

struct LevelConfig {
    let levelId: String
    let title: String
    let difficulty: String
    let imageSource: ImageSource
    let rows: Int
    let cols: Int
    var storyText: String? = nil
    var animationTheme: String? = nil
    var isAscended: Bool = false
    // Project Exodus Fields
    var moduleName: String? = nil
    var integrityStatus: String? = nil
    var motivation: String? = nil
}
