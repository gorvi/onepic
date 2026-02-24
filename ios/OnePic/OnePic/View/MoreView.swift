import SwiftUI
import PhotosUI

struct MoreView: View {
    @ObservedObject var visitorManager: CelestialVisitorManager
    @ObservedObject var levelManager = LevelProgressManager.shared
    @State private var nickname = LevelProgressManager.shared.getNickname()
    @State private var avatarImage: Image? = nil
    @State private var showPhotosPicker = false
    
    @State private var showEditNicknameAlert = false
    @State private var nicknameInput = ""
    @FocusState private var isNicknameInputFocused: Bool
    
    @State private var soundEnabled = true // Connect to SoundManager in real implementation
    
    @State private var showAboutSheet = false
    @State private var showLanguageSheet = false
    
    @Environment(\.presentationMode) var presentationMode
    
    private let avatarFileName = "user_avatar.jpg"

    var body: some View {
        ZStack {
            // 1. Background
            SharedGalaxyBackground(atmosphereTheme: "profile", visitorManager: visitorManager)
                .ignoresSafeArea()
            
            // 2. Nickname Edit Overlay
            if showEditNicknameAlert {
                nicknameEditOverlay
                    .zIndex(2000)
            }

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
            .allowsHitTesting(!showEditNicknameAlert)
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showAboutSheet) {
            AboutView()
        }
        .sheet(isPresented: $showLanguageSheet) {
            LanguageSelectionSheet()
        }
        .sheet(isPresented: $showPhotosPicker) {
            LegacyPhotoPicker { image in
                avatarImage = Image(uiImage: image)
                persistAvatarImage(image)
            }
        }
        .onAppear {
            loadPersistedAvatar()
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
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        isNicknameInputFocused = true
                    }
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
            Text(TRANS.get("app_name", "Traveler Puzzle") + " v1.0.1")
                .font(.system(size: 12, weight: .medium))
            Text(TRANS.get("mod_status_online", "ONLINE").uppercased())
                .font(.system(size: 10, weight: .black))
                .kerning(2)
        }
        .foregroundColor(.white.opacity(0.2))
        .padding(.vertical, 32)
    }
    
    // MARK: - Custom Views
    
    private var nicknameEditOverlay: some View {
        ZStack {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture {
                    isNicknameInputFocused = false
                    withAnimation { showEditNicknameAlert = false }
                }
            
            VStack(spacing: 24) {
                Text(TRANS.get("edit_nickname", "Edit Nickname"))
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                
                VStack(alignment: .trailing, spacing: 4) {
                    TextField(TRANS.get("enter_nickname", "Enter nickname"), text: $nicknameInput)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(12)
                        .foregroundColor(.white)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.2), lineWidth: 1))
                        .autocorrectionDisabled(true)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.default)
                        .focused($isNicknameInputFocused)
                        .submitLabel(.done)
                        .onSubmit {
                            saveNicknameAndDismiss()
                        }
                        .onTapGesture {
                            isNicknameInputFocused = true
                        }
                        .onChangeCompat(of: nicknameInput) { newValue in
                            if newValue.count > 12 {
                                nicknameInput = String(newValue.prefix(12))
                            }
                        }
                    
                    Text("\(nicknameInput.count)/12")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(nicknameInput.count >= 12 ? .red : .white.opacity(0.4))
                        .padding(.trailing, 4)
                }
                
                HStack(spacing: 16) {
                    Button(action: {
                        isNicknameInputFocused = false
                        withAnimation { showEditNicknameAlert = false }
                    }) {
                        Text(TRANS.get("cancel", "Cancel"))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(16)
                    }
                    
                    Button(action: {
                        saveNicknameAndDismiss()
                    }) {
                        Text(TRANS.get("save", "Save"))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(LinearGradient(colors: [Color(hex: 0x2979FF), Color(hex: 0x00E676)], startPoint: .leading, endPoint: .trailing).opacity(0.6))
                            .cornerRadius(16)
                    }
                }
            }
            .padding(24)
            .background(
                RoundedRectangle(cornerRadius: 28)
                    .fill(.ultraThinMaterial)
                    .overlay(RoundedRectangle(cornerRadius: 28).stroke(Color.white.opacity(0.1), lineWidth: 1))
            )
            .padding(.horizontal, 40)
            .shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)
            .onTapGesture {
                // absorb tap so background dismiss does not trigger
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                isNicknameInputFocused = true
            }
        }
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }
    
    // MARK: - Avatar Persistence
    
    private func persistAvatarImage(_ image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.88),
              let url = avatarStorageURL() else {
            return
        }
        
        do {
            try data.write(to: url, options: .atomic)
            LevelProgressManager.shared.saveAvatarPath(url.path)
        } catch {
            print("❌ MoreView: Failed to save avatar image: \(error)")
        }
    }
    
    private func loadPersistedAvatar() {
        guard let path = LevelProgressManager.shared.getAvatarPath() else { return }
        let url = URL(fileURLWithPath: path)
        guard FileManager.default.fileExists(atPath: path),
              let image = UIImage(contentsOfFile: url.path) else {
            return
        }
        avatarImage = Image(uiImage: image)
    }
    
    private func avatarStorageURL() -> URL? {
        guard let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        return dir.appendingPathComponent(avatarFileName)
    }
    
    private func saveNicknameAndDismiss() {
        let trimmed = nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty {
            LevelProgressManager.shared.saveNickname(trimmed)
            nickname = trimmed
        }
        isNicknameInputFocused = false
        withAnimation { showEditNicknameAlert = false }
    }
}

private struct LegacyPhotoPicker: UIViewControllerRepresentable {
    let onImagePicked: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss
    
    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.selectionLimit = 1
        config.filter = .images
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }
    
    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: LegacyPhotoPicker
        
        init(parent: LegacyPhotoPicker) {
            self.parent = parent
        }
        
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard let provider = results.first?.itemProvider,
                  provider.canLoadObject(ofClass: UIImage.self) else {
                parent.dismiss()
                return
            }
            provider.loadObject(ofClass: UIImage.self) { object, _ in
                DispatchQueue.main.async {
                    if let image = object as? UIImage {
                        self.parent.onImagePicked(image)
                    }
                    self.parent.dismiss()
                }
            }
        }
    }
}

// MARK: - About View

struct AboutView: View {
    @Environment(\.dismiss) var dismiss
    @State private var clickCount = 0
    @State private var showAdMobIdAlert = false
    @State private var deviceId = ""
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 24) {
                Spacer()
                
                // App Icon Logo (runtime asset + fallback symbol)
                Group {
                    if let appIcon = UIImage(named: "AppIcon") {
                        Image(uiImage: appIcon)
                            .resizable()
                            .scaledToFit()
                    } else {
                        Image(systemName: "puzzlepiece.fill")
                            .resizable()
                            .scaledToFit()
                            .padding(24)
                            .foregroundColor(.white.opacity(0.9))
                            .background(
                                LinearGradient(
                                    colors: [Color(hex: 0x2979FF), Color(hex: 0x00E676)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    }
                }
                .frame(width: 88, height: 88)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.3), radius: 10, x: 0, y: 5)
                .onTapGesture {
                    clickCount += 1
                    if clickCount >= 10 {
                        deviceId = AdMobIdUtils.getTestDeviceId()
                        showAdMobIdAlert = true
                        clickCount = 0
                    }
                }
                
                VStack(spacing: 8) {
                    Text(TRANS.get("app_name", "Traveler Puzzle"))
                        .font(.system(size: 24, weight: .black))
                        .foregroundColor(.white)
                    
                    Text("v1.0.1 " + TRANS.get("stable_build", "(Stable Build)"))
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.4))
                    
                    Text("by Netrill")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.white.opacity(0.55))
                }
                
                Text(TRANS.get("intro_prologue", "The year is 2077. Earth's energy is depleted, and the once-great civilization stands at the edge of extinction.\n\nYou are the Commander of the 'Ark Initiative.' Our only hope lies in collecting the primal energy cores scattered across the globe to forge a vessel capable of crossing the galaxy.\n\nEvery restored puzzle provides the digital power to ignite the warp engines. Gather the energy, reconstruct the blueprints, and lead humanity across the stars to find our new home."))
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
        .alert("AdMob Test Device ID", isPresented: $showAdMobIdAlert) {
            Button("Copy", action: {
                UIPasteboard.general.string = deviceId
            })
            Button("OK", role: .cancel) { }
        } message: {
            Text(deviceId + "\n\nCopy this ID to AdManager.swift to enable test ads.")
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
        Group {
            if #available(iOS 16.0, *) {
                sheetContent
                    .presentationDetents([.medium, .large])
            } else {
                sheetContent
            }
        }
    }
    
    private var sheetContent: some View {
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
