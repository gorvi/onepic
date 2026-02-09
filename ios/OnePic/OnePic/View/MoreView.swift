import SwiftUI
import PhotosUI

struct MoreView: View {
    @ObservedObject var visitorManager: CelestialVisitorManager
    @ObservedObject var levelManager = LevelProgressManager.shared
    @State private var nickname = LevelProgressManager.shared.getNickname()
    @State private var avatarImage: Image? = nil
    @State private var showPhotosPicker = false
    @State private var selectedItem: PhotosPickerItem? = nil
    
    @State private var showEditNicknameAlert = false
    @State private var nicknameInput = ""
    
    @State private var soundEnabled = true // Connect to SoundManager in real implementation
    
    @State private var showAboutSheet = false
    @State private var showLanguageSheet = false
    
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        ZStack {
            // 1. Background
            SharedGalaxyBackground(atmosphereTheme: "profile", visitorManager: visitorManager)
                .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header
                    headerSection
                    
                    // Profile Card
                    profileCard
                    
                    // Function Menu
                    menuContainer {
                        // Inline Language Selector
                        // Language Selector
                        Button(action: { showLanguageSheet = true }) {
                            MenuLinkItem(
                                icon: "globe",
                                title: TRANS.get("language", "Language"),
                                color: Color(hex: 0x00E676),
                                value: getCurrentLanguageLabel()
                            )
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                        
                        Divider().background(Color.white.opacity(0.1)).padding(.horizontal, 20)
                        
                        // Settings Items
                        MenuSwitchItem(
                            icon: "speaker.wave.2.fill",
                            title: TRANS.get("sound_effects", "Sound Effects"),
                            isOn: $soundEnabled,
                            color: Color(hex: 0x2979FF)
                        )
                        
                        Divider().background(Color.white.opacity(0.1)).padding(.horizontal, 20)
                        
                        // About Item
                        Button(action: { showAboutSheet = true }) {
                            MenuLinkItem(icon: "info.circle.fill", title: TRANS.get("about", "About"), color: Color(hex: 0x2979FF))
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    
                    // Version Info
                    versionInfo
                    
                    Spacer(minLength: 80)
                }
                .padding(20)
            }
        }
        .navigationBarHidden(true)
        .alert(TRANS.get("edit_nickname", "Edit Nickname"), isPresented: $showEditNicknameAlert) {
            TextField(TRANS.get("enter_nickname", "Enter nickname"), text: $nicknameInput)
            Button(TRANS.get("cancel", "Cancel"), role: .cancel) { }
            Button(TRANS.get("save", "Save")) {
                let trimmed = nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty {
                    LevelProgressManager.shared.saveNickname(trimmed)
                    nickname = trimmed
                }
            }
        }
        .sheet(isPresented: $showAboutSheet) {
            AboutView()
        }
        .sheet(isPresented: $showLanguageSheet) {
            LanguageSelectionSheet()
        }
        .photosPicker(isPresented: $showPhotosPicker, selection: $selectedItem, matching: .images)
        .onChange(of: selectedItem) { newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self),
                   let uiImage = UIImage(data: data) {
                    avatarImage = Image(uiImage: uiImage)
                    // TODO: In real app, save data to a local file and store path in LevelProgressManager
                }
            }
        }
    }
    
    private var headerSection: some View {
        HStack {
            Text(TRANS.get("personal_center", "Personal Center"))
                .font(.system(size: 28, weight: .black))
                .foregroundColor(.white)
            Spacer()
        }
        .padding(.top, 10)
    }
    
    private var profileCard: some View {
        HStack(spacing: 20) {
            // Avatar
            Button(action: { showPhotosPicker = true }) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(colors: [Color(hex: 0x00E676), Color(hex: 0x2979FF)], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 76, height: 76)
                    
                    if let avatarImage = avatarImage {
                        avatarImage
                            .resizable()
                            .scaledToFill()
                            .frame(width: 72, height: 72)
                            .clipShape(Circle())
                    } else {
                        Image(systemName: "person.fill")
                            .font(.system(size: 32))
                            .foregroundColor(.white)
                    }
                }
                .overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 2))
            }
            
            // Info
            VStack(alignment: .leading, spacing: 6) {
                Button(action: {
                    nicknameInput = nickname
                    showEditNicknameAlert = true
                }) {
                    HStack(spacing: 8) {
                        Text(nickname)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(.white)
                        
                        Image(systemName: "pencil")
                            .font(.system(size: 14))
                            .foregroundColor(.white.opacity(0.4))
                    }
                }
                
                
                Text(TRANS.get("explorer_id", "Explorer ID") + ": \(LevelProgressManager.shared.getExplorerId())")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.4))
            }
            
            Spacer()
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 28)
                .fill(.ultraThinMaterial)
                .overlay(RoundedRectangle(cornerRadius: 28).stroke(Color.white.opacity(0.1), lineWidth: 1))
        )
    }
    
    private func menuContainer<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .background(Color.white.opacity(0.05))
        .cornerRadius(24)
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.05), lineWidth: 0.5))
    }
    
    private func getCurrentLanguageLabel() -> String {
        let currentCode = LevelProgressManager.shared.getSelectedLanguage()
        let languages = [
            ("en", "English"), ("zh", "简体中文"), ("zh-HK", "繁體中文 (香港)"), ("zh-TW", "繁體中文 (台灣)"),
            ("ru", "Русский"),
            ("es", "Español"), ("pt", "Português"), ("fr", "Français"), ("de", "Deutsch"),
            ("vi", "Tiếng Việt"), ("tr", "Türkçe"), ("ko", "한국어"), ("it", "Italiano"),
            ("ar", "العربية"), ("th", "ไทย"), ("nl", "Nederlands"), ("pl", "Polski"),
            ("sv", "Svenska"), ("hi", "हिन्दी"),
            ("ja", "日本語")
        ]
        return languages.first(where: { $0.0 == currentCode })?.1 ?? "English"
    }
    
    private var versionInfo: some View {
        VStack(spacing: 4) {
            Text(TRANS.get("app_name", "OnePic") + " v1.0.1")
                .font(.system(size: 12, weight: .medium))
            Text(TRANS.get("mod_status_online", "ONLINE").uppercased())
                .font(.system(size: 10, weight: .black))
                .kerning(2)
        }
        .foregroundColor(.white.opacity(0.2))
        .padding(.vertical, 32)
    }
}

// MARK: - About View

struct AboutView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 24) {
                Spacer()
                
                // Icon Mock
                RoundedRectangle(cornerRadius: 24)
                    .fill(LinearGradient(colors: [Color(hex: 0x2979FF), Color(hex: 0x00E676)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 80, height: 80)
                    .overlay(
                        Image(systemName: "circle.dotted.circle.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                    )
                
                VStack(spacing: 8) {
                    Text(TRANS.get("app_name", "OnePic"))
                        .font(.system(size: 24, weight: .black))
                        .foregroundColor(.white)
                    
                    Text("v1.0.1 " + TRANS.get("stable_build", "(Stable Build)"))
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.4))
                }
                
                Text(TRANS.get("about_description", "Explore the infinite cosmos, one piece at a time. OnePic is a contemplative space journey where every merge reveals the hidden beauty of the galaxy. Created by AIOK with passion for minimalism and exploration."))
                    .font(.system(size: 16))
                    .foregroundColor(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                    .lineSpacing(4)
                
                Spacer()
                
                Button(action: { dismiss() }) {
                    Text(TRANS.get("close", "Close"))
                        .font(.system(size: 16, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.1))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Components

struct LanguageSelectionSheet: View {
    @Environment(\.dismiss) var dismiss
    let languages = [
        ("en", "English"), ("zh", "简体中文"), ("zh-HK", "繁體中文 (香港)"), ("zh-TW", "繁體中文 (台灣)"),
        ("ru", "Русский"),
        ("es", "Español"), ("pt", "Português"), ("fr", "Français"), ("de", "Deutsch"),
        ("vi", "Tiếng Việt"), ("tr", "Türkçe"), ("ko", "한국어"), ("it", "Italiano"),
        ("ar", "العربية"), ("th", "ไทย"), ("nl", "Nederlands"), ("pl", "Polski"),
        ("sv", "Svenska"), ("hi", "हिन्दी"),
        ("ja", "日本語")
    ]
    @State private var currentLang = LevelProgressManager.shared.getSelectedLanguage()

    var body: some View {
        ZStack {
            // Deep Space Gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(hex: 0x0F2027),
                    Color(hex: 0x203A43),
                    Color(hex: 0x2C5364)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text(TRANS.get("select_language", "Select Language"))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white.opacity(0.3))
                    }
                }
                .padding(20)
                
                // List
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(languages, id: \.0) { code, label in
                            let isSelected = currentLang == code
                            Button(action: {
                                LevelProgressManager.shared.saveLanguage(code)
                                currentLang = code
                                // Slight delay to let user see selection
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                    dismiss()
                                }
                            }) {
                                HStack {
                                    Text(label)
                                        .font(.system(size: 16, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(isSelected ? .white : .white.opacity(0.7))
                                    Spacer()
                                    if isSelected {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(Color(hex: 0x00E676))
                                    }
                                }
                                .padding(.horizontal, 20)
                                .padding(.vertical, 16)
                                .background(isSelected ? Color.white.opacity(0.1) : Color.clear)
                                .cornerRadius(12)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 40)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

struct MenuSwitchItem: View {
    let icon: String
    let title: String
    @Binding var isOn: Bool
    let color: Color
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .frame(width: 24)
                .foregroundColor(color)
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white.opacity(0.9))
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(color)
        }
        .padding(20)
    }
}

struct MenuLinkItem: View {
    let icon: String
    let title: String
    let color: Color
    var value: String? = nil
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .frame(width: 24)
                .foregroundColor(color)
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white.opacity(0.9))
            Spacer()
            if let value = value {
                Text(value)
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.5))
            }
            Image(systemName: "chevron.right")
                .font(.system(size: 14))
                .foregroundColor(.white.opacity(0.3))
        }
        .padding(20)
    }
}

#Preview {
    MoreView(visitorManager: CelestialVisitorManager())
}
