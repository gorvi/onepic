import AVFoundation
import Combine
import SwiftUI

class SoundManager: ObservableObject {
    static let shared = SoundManager()
    private init() {}
    
    private var players: [String: AVAudioPlayer] = [:]
    private var bgmPlayer: AVAudioPlayer?
    
    @Published var isMuted: Bool = UserDefaults.standard.bool(forKey: "is_muted") {
        didSet {
            UserDefaults.standard.set(isMuted, forKey: "is_muted")
            if isMuted {
                bgmPlayer?.pause()
            } else {
                bgmPlayer?.play()
            }
        }
    }
    
    func playSound(_ name: String, type: String = "mp3") {
        let finalName = (name == "snap_grid") ? "snap" : name
        guard !isMuted else { return }
        
        if let player = players[finalName] {
            player.play()
            return
        }
        
        guard let url = Bundle.main.url(forResource: finalName, withExtension: type) else {
            print("Sound file \(finalName).\(type) not found")
            return
        }
        
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            player.play()
            players[finalName] = player
        } catch {
            print("Error playing sound \(finalName): \(error)")
        }
    }
    
    func playBGM(_ name: String, type: String = "mp3") {
        guard let url = Bundle.main.url(forResource: name, withExtension: type) else { return }
        
        do {
            bgmPlayer = try AVAudioPlayer(contentsOf: url)
            bgmPlayer?.numberOfLoops = -1 // Loop indefinitely
            
            if !isMuted {
                bgmPlayer?.play()
            }
        } catch {
            print("Error playing BGM: \(error)")
        }
    }
    
    func stopBGM() {
        bgmPlayer?.stop()
    }
    
    // Predefined sounds mirroring Android SoundType
    func playSnap() { playSound("snap") }
    func playSnapGrid() { playSound("snap") }
    func playWin() { playSound("complete") }
    func playClick() { playSound("snap") }
    func playRevert() { playSound("revert") }
    /// 金币收集/碰撞音效（首页飞入收集完成后）
    func playCoinCollect() { playSound("snap") }
}
