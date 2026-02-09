import SwiftUI

struct CheckInView: View {
    @ObservedObject var visitorManager: CelestialVisitorManager
    @State private var hasCheckedInToday = LevelProgressManager.shared.hasCheckedInToday()
    @State private var consecutiveDays = LevelProgressManager.shared.getConsecutiveDays()
    @State private var totalCoins = LevelProgressManager.shared.getCoins()
    @State private var coinsEarnedToday = 0
    
    // Animation States
    @State private var cardScale: CGFloat = 1.0
    @State private var showSuccessEffect = false
    @State private var coinAnimationProgress: Double = 0
    @State private var dotPulseScale: CGFloat = 1.0
    
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        ZStack {
            // 1. Background Layer (Cosmos Background)
            SharedGalaxyBackground(atmosphereTheme: "signin", visitorManager: visitorManager)
                .ignoresSafeArea()
            
            // 2. Holographic Grid Overlay
            HolographicGridView()
                .opacity(0.3)
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header Area
                    headerSection
                    
                    // Main Check-In Card
                    mainCheckInCard
                    
                    // Stats Row
                    statsRow
                    
                    // 30-Day History
                    MonthlyCheckInHistoryView()
                    
                    Spacer(minLength: 80)
                }
                .padding(20)
            }
        }
        .onAppear {
            totalCoins = LevelProgressManager.shared.getCoins()
            hasCheckedInToday = LevelProgressManager.shared.hasCheckedInToday()
            consecutiveDays = LevelProgressManager.shared.getConsecutiveDays()
            coinsEarnedToday = LevelProgressManager.shared.getCheckInRewardToday()
        }
        .navigationBarHidden(true)
    }
    
    private var headerSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(TRANS.get("daily_check_in", "Daily Check-in"))
                    .font(.system(size: 28, weight: .black))
                    .foregroundColor(.white)
                
                Text(getCurrentDateLongString())
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
            
            // Coin Display
            HStack(spacing: 8) {
                CoinIconView(size: 20)
                Text("\(totalCoins)")
                    .font(.system(size: 18, weight: .bold))
                    .monospacedDigit()
                    .foregroundColor(Color(hex: 0xFFD700))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(Color(hex: 0xFFD700).opacity(0.2))
                    .overlay(Capsule().stroke(Color(hex: 0xFFD700).opacity(0.5), lineWidth: 1))
            )
        }
        .padding(.top, 10)
    }
    
    private var mainCheckInCard: some View {
        Button(action: {
            if !hasCheckedInToday {
                performCheckInAction()
            }
        }) {
            ZStack {
                // Background Glass
                RoundedRectangle(cornerRadius: 32)
                    .fill(.ultraThinMaterial)
                    .background(
                        RoundedRectangle(cornerRadius: 32)
                            .fill(hasCheckedInToday ? Color.green.opacity(0.05) : Color.blue.opacity(0.05))
                    )
                
                // Border with Gradient
                RoundedRectangle(cornerRadius: 32)
                    .stroke(
                        LinearGradient(
                            colors: hasCheckedInToday 
                                ? [Color(hex: 0x00E676).opacity(0.5), .clear, Color(hex: 0x00E676).opacity(0.2)]
                                : [Color(hex: 0x00B0FF).opacity(0.5), .clear, Color(hex: 0x00B0FF).opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
                
                // Decorative Mesh (Optional: Simpler version for now)
                
                VStack(spacing: 12) {
                    if hasCheckedInToday {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(Color(hex: 0x00E676))
                            .scaleEffect(showSuccessEffect ? 1.2 : 1.0)
                        
                        Text(TRANS.get("check_in_energy_active", "Energy Synchronized"))
                            .font(.system(size: 20, weight: .heavy))
                            .foregroundColor(.white)
                    } else {
                        Image(systemName: "sparkle")
                            .font(.system(size: 48))
                            .foregroundColor(Color(hex: 0x00B0FF))
                            .rotationEffect(.degrees(showSuccessEffect ? 360 : 0))
                        
                        Text(TRANS.get("check_in_energy_activate", "Activate Daily Signal"))
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(.white)
                        
                        Text(TRANS.get("check_in_sync_required", "Ready for synchronization"))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(Color(hex: 0x00B0FF).opacity(0.6))
                    }
                }
                
                if !hasCheckedInToday {
                    ZStack {
                        Circle()
                            .fill(Color.red.opacity(0.3))
                            .frame(width: 20, height: 20)
                            .scaleEffect(dotPulseScale * 1.4)
                        
                        Circle()
                            .fill(Color.red)
                            .frame(width: 10, height: 10)
                            .overlay(Circle().stroke(Color.white, lineWidth: 1))
                    }
                    .offset(x: 140, y: -60)
                    .onAppear {
                        withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
                            dotPulseScale = 1.2
                        }
                    }
                }
            }
            .frame(height: 160)
            .scaleEffect(cardScale)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(hasCheckedInToday)
    }
    
    private var statsRow: some View {
        HStack(spacing: 12) {
            statItem(
                title: TRANS.get("consecutive_check_in", "Consecutive"),
                value: TRANS.get("check_in_days_suffix", "%d Days").replacingOccurrences(of: "%1$d", with: "\(consecutiveDays)").replacingOccurrences(of: "%d", with: "\(consecutiveDays)"),
                color: Color(hex: 0x00B0FF)
            )
            
            statItem(
                title: TRANS.get("today_earned", "Today"),
                value: hasCheckedInToday ? "+\(coinsEarnedToday) 🪙" : TRANS.get("check_in_pending", "Pending"),
                color: Color(hex: 0xFFD700)
            )
        }
    }
    
    private func statItem(title: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title.uppercased())
                .font(.system(size: 12, weight: .black))
                .foregroundColor(.white.opacity(0.4))
                .kerning(1)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(color)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: 100)
        .background(Color.white.opacity(0.05))
        .cornerRadius(20)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.white.opacity(0.1), lineWidth: 0.5)
        )
    }
    
    private func performCheckInAction() {
        SoundManager.shared.playClick()
        
        withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
            cardScale = 1.15
        }
        
        let (reward, streak) = LevelProgressManager.shared.performCheckIn()
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                cardScale = 1.0
                hasCheckedInToday = true
                consecutiveDays = streak
                coinsEarnedToday = reward
                totalCoins = LevelProgressManager.shared.getCoins()
                showSuccessEffect = true
            }
        }
    }
    
    private func getCurrentDateLongString() -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.locale = Locale(identifier: LevelProgressManager.shared.getSelectedLanguage())
        return formatter.string(from: Date())
    }
}

// MARK: - Subviews

struct HolographicGridView: View {
    var body: some View {
        Canvas { context, size in
            let step: CGFloat = 20
            
            // Vertical lines
            for x in stride(from: 0, through: size.width, by: step) {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: x, y: 0))
                        path.addLine(to: CGPoint(x: x, y: size.height))
                    },
                    with: .color(.white.opacity(0.05)),
                    lineWidth: 0.5
                )
            }
            
            // Horizontal lines
            for y in stride(from: 0, through: size.height, by: step) {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addLine(to: CGPoint(x: size.width, y: y))
                    },
                    with: .color(.white.opacity(0.05)),
                    lineWidth: 0.5
                )
            }
        }
    }
}

struct MonthlyCheckInHistoryView: View {
    let history = LevelProgressManager.shared.getCheckInHistory()
    let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 7)
    
    var days: [DayInfo] {
        let calendar = Calendar.current
        let today = Date()
        var result: [DayInfo] = []
        
        for i in (0..<30).reversed() {
            if let date = calendar.date(byAdding: .day, value: -i, to: today) {
                let str = formatDate(date)
                result.append(DayInfo(
                    id: str,
                    isChecked: history.contains(str),
                    isToday: calendar.isDateInToday(date),
                    dayNumber: "\(calendar.component(.day, from: date))"
                ))
            }
        }
        return result
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Rectangle()
                    .fill(Color(hex: 0x00E676))
                    .frame(width: 4, height: 16)
                Text(TRANS.get("check_in_station_log", "Synchronization Log"))
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white.opacity(0.7))
                    .kerning(1)
            }
            
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(days) { day in
                    ZStack {
                        Circle()
                            .fill(day.isChecked ? Color(hex: 0x00E676).opacity(0.2) : Color.white.opacity(0.05))
                            .frame(width: 36, height: 36)
                        
                        if day.isChecked {
                            Image(systemName: "checkmark")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(hex: 0x00E676))
                        } else {
                            Text(day.dayNumber)
                                .font(.system(size: 14, weight: .bold)) // Slightly larger for readability
                                .foregroundColor(day.isToday ? Color(hex: 0x00B0FF) : Color.white.opacity(0.3))
                        }
                    }
                    .overlay(
                        Circle().stroke(day.isToday ? Color(hex: 0x00B0FF).opacity(0.5) : Color.clear, lineWidth: 1)
                    )
                }
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.03))
        .cornerRadius(24)
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(Color.white.opacity(0.05), lineWidth: 0.5)
        )
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

struct DayInfo: Identifiable {
    let id: String
    let isChecked: Bool
    let isToday: Bool
    let dayNumber: String
}

#Preview {
    CheckInView(visitorManager: CelestialVisitorManager())
}
