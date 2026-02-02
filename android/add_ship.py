from PIL import Image, ImageFilter
import os

# Source Artifacts (Clean originals)
bg_source_path = '/Users/ghw/.gemini/antigravity/brain/fadf8f18-eca3-4f09-bd81-1a04ec5a0afc/eye_of_universe_v2_golden_1769960845188.png'
ship_source_path = '/Users/ghw/.gemini/antigravity/brain/fadf8f18-eca3-4f09-bd81-1a04ec5a0afc/spaceship_v3_white_bg_1769961720131.png'

# Output Path (Game Asset)
output_path = '/Users/ghw/AndroidStudioProjects/2026/onepic/app/src/main/assets/gallery_levels/level_60_B_EyeOfTheUniverse.webp'

try:
    print(f"Loading background from: {bg_source_path}")
    bg = Image.open(bg_source_path).convert("RGBA")
    
    print(f"Loading spaceship from: {ship_source_path}")
    ship = Image.open(ship_source_path).convert("RGBA")
    
    # 1. Resize Ship
    # Target 20% of background width
    target_width = int(bg.width * 0.20)
    aspect_ratio = ship.height / ship.width
    target_height = int(target_width * aspect_ratio)
    ship = ship.resize((target_width, target_height), Image.Resampling.LANCZOS)
    
    # 2. Flood Fill Background Removal (Magic Wand)
    pixels = ship.load()
    width, height = ship.size
    
    # Sample background color from top-left (Should be White now)
    start_color = pixels[0, 0] # (r, g, b, a)
    bg_r, bg_g, bg_b = start_color[0], start_color[1], start_color[2]
    
    # Increase tolerance for white compression artifacts
    tolerance = 80 
    
    # BFS Queue for Flood Fill
    visited = set()
    queue = []
    
    # Start from all 4 corners to be safe (assuming background touches all corners)
    corners = [(0, 0), (width-1, 0), (0, height-1), (width-1, height-1)]
    for pt in corners:
        queue.append(pt)
        visited.add(pt)
        
    def color_match(c1, r2, g2, b2):
        return (abs(c1[0] - r2) + abs(c1[1] - g2) + abs(c1[2] - b2)) < tolerance

    while queue:
        x, y = queue.pop(0)
        
        # Get current pixel color
        r, g, b, a = pixels[x, y]
        
        if color_match((r, g, b), bg_r, bg_g, bg_b):
            # It's background -> Make transparent
            pixels[x, y] = (0, 0, 0, 0)
            
            # Add neighbors
            for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height:
                    if (nx, ny) not in visited:
                        visited.add((nx, ny))
                        queue.append((nx, ny))
    
    # 2.5 DEFRINGING (Remove pre-multiplied WHITE halo)
    # The edges have WHITE mixed in. We need different math for white un-multiply?
    # Actually, if it's white premultiplied:  Observed = Original*Alpha + White*(1-Alpha)
    # So: Original = (Observed - 255*(1-Alpha)) / Alpha
    # Let's try simple alpha erosion first, as un-multiplying white is tricky and can darken edges too much if Alpha varies.
    # Given the glowing background, let's skip complex math and just use typical matting erosion.


    # 3. Soften edges (Anti-aliasing) & Remove Dark Halo (Erosion)
    r, g, b, a = ship.split()
    
    # Erode the alpha channel to cut off the dark fringe pixels
    # MinFilter(3) roughly erodes by 1 pixel radius (3x3 kernel)
    a = a.filter(ImageFilter.MinFilter(3))
    
    # Then Gaussian Blur for smoothness
    a = a.filter(ImageFilter.GaussianBlur(radius=1))
    
    ship.putalpha(a)
    
    # 4. Position Ship
    # Center Horizontally
    x = (bg.width - ship.width) // 2
    
    # Vertically: Place it lower (70% down)
    y = int(bg.height * 0.70) - (ship.height // 2)
    
    # 5. Composite
    bg.paste(ship, (x, y), ship)
    
    bg.save(output_path, 'WEBP', quality=95)
    print(f"Successfully created clean composite at: {output_path}")

except Exception as e:
    print(f"Error: {e}")
