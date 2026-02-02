import json
import os

def generate_story():
    modules = [
        {
            "name": "Quantum Keel",
            "name_zh": "量子龙骨",
            "theme": "Foundation",
            "fragment_name": "Titanium Alloy", 
            "logs": [
                "First alloy synthesized. The foundation of our future is laid.",
                "Keel segment alpha stabilized. It's heavier than hope.",
                "Micro-fractures sealed. The spine must not break.",
                "Resonance tests passed. It sings in the void.",
                "Structural stabilizers aligned. The spine is holding.",
                "Mid-section welded. We are building a giant.",
                "Shock absorbers installed. Ready for turbulence.",
                "Keel integrity at 80%. It looks like a ship now.",
                "Final stress test initiated. Holding firm.",
                "Quantum Keel complete. Phase 1 successful."
            ],
            "motivations": [
                "A journey of a light-year begins with a single weld.",
                "Stand firm, Architect. Gravity is just a suggestion.",
                "Every piece matters. Do not falter.",
                "The void is cold, but our will is iron.",
                "Stay focused. The weight of hope is heavy.",
                "Build strong. Build for eternity.",
                "Perfection is not optional. It is survival.",
                "We are the architects of tomorrow.",
                "Almost there. The backbone is forming.",
                "Great work. Now, let's give this giant a heart."
            ]
        },
        {
            "name": "Fusion Reactor Heart",
             "name_zh": "聚变核心",
            "theme": "Energy",
            "logs": [
                "Core containment field active. It's hungry.",
                "Plasma injectors calibrated. The heat is rising.",
                "Magnetic confinement stable. A star in a bottle.",
                "Ignition sequence primmer synchronized.",
                "Cooling systems online. Preventing meltdown.",
                "First spark achieved. It breathes fire.",
                "Energy output nominal. The heartbeat begins.",
                "Shielding reinforced. Containing the sun.",
                "Reactor harmonics synchronized. Beautiful.",
                "Fusion Reactor online. Infinite power secured."
            ],
            "motivations": [
                "Light the fire that will never go out.",
                "Powering the dreams of billions.",
                "Control the chaos. Harness the star.",
                "Energy is life. Don't let it fade.",
                "Feel the heat? That is progress.",
                "Brighter than the darkness around us.",
                "Steady hands. The core is volatile.",
                "Functioning within parameters. Keep going.",
                "The pulse of the Ark strengthens.",
                "Unlimited power. Now we can move."
            ]
        },
        {
            "name": "Neuro-Link Cockpit",
             "name_zh": "神经链接驾驶舱",
            "theme": "Control",
            "logs": [
                "Neural interface mapped. Mind meets machine.",
                "Haptic feedback sensors distributed.",
                "Pilot seat installed. The throne of the navigator.",
                "Viewscreens polished. A window to the abyss.",
                "Flight controls linked to the cortex.",
                "Emergency override systems codified.",
                "Navigation AI 'OnePic' integrated.",
                "Life support for the bridge operational.",
                "System latency reduced to zero.",
                "Cockpit operational. The mind is the pilot."
            ],
             "motivations": [
                "Where the mind goes, the ship follows.",
                "See beyond the horizon.",
                "You are the brain of this beast.",
                "clarity of purpose. Clarity of vision.",
                "The stars are calling. Can you hear them?",
                "Focus. The universe is watching.",
                "One thought, one destination.",
                "Prepare to take the helm.",
                "Connection established.",
                "We are ready to see the universe."
            ]
        },
        {
            "name": "Cryostasis Hall",
             "name_zh": "休眠大厅",
            "theme": "Preservation",
            "logs": [
                "Chamber 1 constructed. For the dreamers.",
                "Cryo-fluid synthetics produced.",
                "Vital monitoring arrays wide-networked.",
                "Power redundancy for stasis pods secured.",
                "Thaw protocols written with care.",
                "Capacity expanded to 5 million souls.",
                "Dream-suppression fields active.",
                "Safety locks verify 100% seal.",
                "The sleepers will be safe here.",
                "Cryostasis Hall sealed. Evolution paused."
            ],
             "motivations": [
                "Guardians of the sleeping.",
                "Silence is safety.",
                "They trust us with their lives.",
                "A long sleep for a new dawn.",
                "Preserve the past to save the future.",
                "Quiet. The future is resting.",
                "Every pod is a universe of dreams.",
                "Watch over them, Architect.",
                "Safe passage for all.",
                "The passengers are secured."
            ]
        },
         {
            "name": "The Bio-Dome",
             "name_zh": "生态循环仓",
            "theme": "Life",
            "logs": [
                "Hydroponics bay assembled. Green in the dark.",
                "Atmospheric scrubbers online. Fresh air.",
                "Water purification cycle established.",
                "Seed vault integration complete.",
                "Artificial sunlight arrays mounted.",
                "Micro-ecosystem bacterial balance achieved.",
                "First sprout observed. Life finds a way.",
                "Carbon cycle optimized for long-haul.",
                "The smell of rain synthesized.",
                "Bio-Dome flourishing. Earth, reborn."
            ],
             "motivations": [
                "Bring a piece of home with us.",
                "Green is the color of hope.",
                "Breathe. Build. Bloom.",
                "Life requires balance.",
                "Nurture the seed of tomorrow.",
                "A garden in the stars.",
                "Sustain the cycle.",
                "Nature is our greatest technology.",
                "Growth is slow, but certain.",
                "We carry the forests with us."
            ]
        },
        {
            "name": "Void Shields",
             "name_zh": "虚空护盾",
            "theme": "Protection",
            "logs": [
                 "Emitter arrays positioned on the hull.",
                 "Force field harmonics calculated.",
                 "Meteorite deflection subroutines compiled.",
                 "Radiation absorption mesh applied.",
                 "Energy dispersion matrix active.",
                 "Impact simulation: Success.",
                 "Shield regeneration capacitors charged.",
                 "Hull integrity reinforced x100.",
                 "Invisible armor wrapping the Ark.",
                 "Void Shields active. We are invincible."
            ],
             "motivations": [
                "The void is hostile. Be ready.",
                "Protect what matters most.",
                "Strength is an invisible wall.",
                "Nothing shall pass.",
                "Deflect the darkness.",
                "Safety through superiority.",
                "Armor the hope.",
                "Stand against the storm.",
                "Unbreakable.",
                "Safe from the cosmic winds."
            ]
        },
         {
            "name": "Hyper-Sensors",
             "name_zh": "超感雷达",
            "theme": "Perception",
            "logs": [
                "Deep space telescope lens ground.",
                "Spectrometers tuned to extra-solar frequencies.",
                "Gravitational wave detectors mounted.",
                "Dark matter scanners online.",
                "Long-range telemetry array deployed.",
                "Signal noise filtration improved.",
                "First image of Destination Alpha captured.",
                "Nebula mapping protocols active.",
                "Blind spots eliminated.",
                "Hyper-Sensors calibrated. We see everything."
            ],
             "motivations": [
                "Knowledge is the first step.",
                "Piercing the veil of the unknown.",
                "Look closer.",
                "The truth is out there.",
                "Eyes on the prize.",
                "Mapping the path to salvation.",
                "See the invisible.",
                "Nothing can hide from us.",
                "Awareness is survival.",
                "The path is clear."
            ]
        },
        {
             "name": "Ion Thrusters",
             "name_zh": "离子推进器",
            "theme": "Movement",
            "logs": [
                "Ionization chamber assembled.",
                "Magnetic nozzles focused.",
                "Xenon fuel tanks pressurized.",
                "Thrust vectoring gimbals greased.",
                "Sub-light velocity simulated.",
                "Exhaust manifold heat-shielded.",
                "Acceleration curves smoothed.",
                "Braking thrusters installed. Stopping is hard.",
                "Engine test fire: Blue flame achieved.",
                "Ion Thrusters nominal. We have movement."
            ],
             "motivations": [
                "Momentum is key.",
                "Push forward, always.",
                "Speed is life.",
                "Leave the past behind.",
                "Steady acceleration.",
                "Forward. Only forward.",
                "Feel the drift.",
                "Precision in motion.",
                "Blue fire, bright future.",
                "We are no longer static."
            ]
        },
         {
            "name": "Communication Spire",
             "name_zh": "星语塔",
            "theme": "Connection",
            "logs": [
                "Subspace transceiver core synthesized.",
                "Antenna array extended. Listening.",
                "Encryption keys generated.",
                "Universal translator database loaded.",
                "Distress beacon functionality tested.",
                "Inter-ship comms network live.",
                "Message to Earth archives recorded.",
                "Signal boosters amplified.",
                "The silence of space is broken.",
                "Communication Spire active. We are not alone."
            ],
             "motivations": [
                "Speak to the stars.",
                "Connection is unbreakable.",
                "Listen carefully.",
                "Voices across the void.",
                "Sending hope.",
                "Never silence the truth.",
                "A signal in the dark.",
                "Calling home.",
                "We are heard.",
                "United by signal."
            ]
        },
        {
            "name": "Genesis Library",
             "name_zh": "文明火种库",
            "theme": "Knowledge",
            "logs": [
                "Data crystal storage banks racked.",
                "All of human literature uploaded.",
                "DNA sequencing of all Earth species stored.",
                "Art history archives digitized.",
                "Scientific theorems preserved.",
                "Music of the spheres recorded.",
                "History lessons for the new world.",
                "Server cooling optimized.",
                "Redundancy 99.9%. Knowledge is safe.",
                "Genesis Library sealed. We remember."
            ],
             "motivations": [
                "Knowledge is the true treasure.",
                "Remember who we are.",
                "Culture is our soul.",
                "Do not forget.",
                "Learning for the future.",
                "The sum of us.",
                "Wisdom is eternal.",
                "A library for the stars.",
                "Preserving the spark.",
                "We carry our history."
            ]
        },
        {
             "name": "Warp Drive",
             "name_zh": "曲率引擎",
            "theme": "Speed",
            "logs": [
                 "Space-time distortion coils wound.",
                 "Dark matter injector synced.",
                 "Reality anchors stabilization check.",
                 "Warp bubble geometry calculated.",
                 "Relativity compensators active.",
                 "Tachyon burst emitters aligned.",
                 "Folding space... simulation pass.",
                 "Event horizon safety locks on.",
                 "The fabric of the universe bends.",
                 "Warp Drive operational. Einstein was right."
            ],
             "motivations": [
                "Beyond the speed of light.",
                "Bend the rules.",
                "Distance is an illusion.",
                "Faster than thought.",
                "The universe is small.",
                "Warp speed ahead.",
                "Breaking the barrier.",
                "Time is relative.",
                "Impossible is nothing.",
                "The stars are within reach."
            ]
        },
        {
            "name": "The Launch Key",
             "name_zh": "启动密钥",
            "theme": "Finale",
            "logs": [
                 "System wide diagnostics: Green.",
                 "Captain's clearance code verified.",
                 "Countdown protocols initiated.",
                 "Fuel lines fully pressurized.",
                 "Passenger manifest confirmed.",
                 "Launch trajectory plotted.",
                 "OnePic AI final handshake.",
                 "Safety locks disengaged.",
                 "Ignition sequence ready.",
                 "The Launch Key turned. The Ark breathes."
            ],
             "motivations": [
                "This is it.",
                "The final step.",
                "Destiny awaits.",
                "No turning back.",
                "For humanity.",
                "Press the button.",
                "Ready or not.",
                "The end of the beginning.",
                "Launch.",
                "Take the helm, Savior. The stars await."
            ]
        }
    ]

    story_data = []
    level_id = 1
    
    for module in modules:
        for i in range(10):
            # Calculate progress percentage (10% to 100% for the current module)
            progress = (i + 1) * 10
            is_complete = (i == 9)
            
            entry = {
                "level_id": level_id,
                "module_name": module["name"],
                "module_name_zh": module["name_zh"],
                "log": module["logs"][i] if i < len(module["logs"]) else "System processing...",
                "status": f"{module['name']}: {progress}% Integrity" if not is_complete else f"MODULE ONLINE: {module['name']} Complete",
                "motivation": module["motivations"][i] if i < len(module["motivations"]) else "Proceed."
            }
            story_data.append(entry)
            level_id += 1

    # Save to JSON
    output_path = "app/src/main/assets/project_exodus_story.json"
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(story_data, f, indent=4, ensure_ascii=False)
    
    print(f"Successfully generated story for {len(story_data)} levels at {output_path}")

if __name__ == "__main__":
    generate_story()
