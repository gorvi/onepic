import SwiftUI
import UIKit

struct ImageUtils {
    // 性能优化：添加图片缓存，避免重复加载
    private static let imageCache = NSCache<NSString, UIImage>()
    private static let pathCache = NSCache<NSString, NSURL>()
    
    /// Loads an image from various sources, handling raw Bundle files correctly.
    /// 性能优化：使用缓存的 UIImage 构建 Image，避免重复加载
    static func loadImage(source: ImageSource) -> Image {
        // 性能优化：优先使用缓存的 UIImage
        if let uiImage = loadUIImage(source: source) {
            return Image(uiImage: uiImage)
        }
        
        // Fallback
        switch source {
        case .resource(let name):
            return Image(name)
        case .asset(_):
            return Image(systemName: "photo")
        case .url(_):
            return Image(systemName: "globe")
        case .generated:
            return Image(systemName: "photo")
        }
    }
    
    static func loadUIImage(source: ImageSource) -> UIImage? {
        switch source {
        case .resource(let name):
            // 性能优化：缓存 UIImage
            let cacheKey = "res:\(name)" as NSString
            if let cached = imageCache.object(forKey: cacheKey) {
                return cached
            }
            if let img = UIImage(named: name) {
                imageCache.setObject(img, forKey: cacheKey)
                return img
            }
            return nil
            
        case .asset(let path):
            // 性能优化：缓存 UIImage
            let cacheKey = "asset:\(path)" as NSString
            if let cached = imageCache.object(forKey: cacheKey) {
                return cached
            }
            
            if let img = UIImage(named: path) {
                imageCache.setObject(img, forKey: cacheKey)
                return img
            }
            
            let filename = (path as NSString).lastPathComponent
            let nameWithoutExt = (filename as NSString).deletingPathExtension
            // Try jpg/png/webp（性能优化：缓存文件路径）
            if let url = findResourceURLCached(name: nameWithoutExt, ext: "jpg") ?? 
                         findResourceURLCached(name: nameWithoutExt, ext: "png") ?? 
                         findResourceURLCached(name: nameWithoutExt, ext: "webp") {
                if let img = UIImage(contentsOfFile: url.path) {
                    imageCache.setObject(img, forKey: cacheKey)
                    return img
                }
            }
            return nil
        default: return nil
        }
    }
    
    // 性能优化：缓存文件路径查找结果
    private static func findResourceURLCached(name: String, ext: String) -> URL? {
        let cacheKey = "\(name).\(ext)" as NSString
        if let cached = pathCache.object(forKey: cacheKey) as URL? {
            return cached
        }
        if let url = findResourceURL(name: name, ext: ext) {
            pathCache.setObject(url as NSURL, forKey: cacheKey)
            return url
        }
        return nil
    }
    
    // Recursive Finder (Copy of LevelRepository logic for global use)
    private static func findResourceURL(name: String, ext: String) -> URL? {
        if let url = Bundle.main.url(forResource: name, withExtension: ext) { return url }
        if let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "gallery_levels") { return url }
        if let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "Images") { return url }

        let fileManager = FileManager.default
        if let resourcePath = Bundle.main.resourcePath {
            let enumerator = fileManager.enumerator(atPath: resourcePath)
            while let file = enumerator?.nextObject() as? String {
                if file.hasSuffix("\(name).\(ext)") {
                    return Bundle.main.bundleURL.appendingPathComponent(file)
                }
            }
        }
        return nil
    }
}
