import Foundation
import AdSupport
import CommonCrypto

struct AdMobIdUtils {
    /// 获取当前设备的 AdMob 测试 ID (MD5 格式的 IDFA)
    static func getTestDeviceId() -> String {
        // 1. 获取 IDFA (针对 iOS)
        let idfa = ASIdentifierManager.shared().advertisingIdentifier.uuidString
        
        // 2. MD5 加密 (AdMob 测试 ID 的标准格式)
        return md5(idfa).uppercased()
    }
    
    /// 标准 MD5 算法
    private static func md5(_ string: String) -> String {
        let data = Data(string.utf8)
        var digest = [UInt8](repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
        _ = data.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) in
            CC_MD5(bytes.baseAddress, CC_LONG(data.count), &digest)
        }
        return digest.map { String(format: "%02hhx", $0) }.joined()
    }
}
