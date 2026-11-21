package com.example.data.model

object LegalDocuments {
    const val PRIVACY_VERSION = "1.0"
    const val TERMS_VERSION = "1.0"
    const val GUIDELINES_VERSION = "1.0"

    val privacyPolicyText = """
LITHFIT PRIVACY POLICY
Effective Date: November 2026 | Version $PRIVACY_VERSION
Operated by CLASTAD ("CLASTAD", "we", "our") • Clastad.xyz

1. INFORMATION WE COLLECT
- Account Information: Your chosen display name, unique username, and locally hashed credentials saved securely on your device using Android SharedPreferences.
- Profile & Style Preferences: Your chosen username, avatar studio configuration, preferred fashion styles, preferred colors, clothing categories, currency, region, and optional city name for weather styling.
- Digital Wardrobe & Outfits: Clothing names, categories, colors, materials, notes, outfit combinations, and optional garment or private full-body styling photographs you choose to capture or select.
- AI Stylist Conversations: Prompts you send to LITHFIT AI, generated styling rationales, and optional outfit preview illustrations you choose to generate or save.

2. HOW PHOTOGRAPHS ARE PROCESSED
- Photos you capture via Camera or pick via the Android Photo Picker are compressed and stored privately on your device inside app-private storage.
- Personal full-body styling photos are stored strictly on-device in private app storage and are never published, ranked, or shared. You can delete them at any time in one tap.
- When you trigger AI garment recognition or visual search, the selected garment image is transmitted securely via Firebase App Check to classify garment attributes (category, color, pattern, material, style).

3. LOCAL STORAGE & PRIVACY CONTROLS
- Your profile, account credentials (salted SHA-256 hash), preferences, avatar setup, wardrobe items, outfits, and saved items are stored privately per-account on your device.
- LITHFIT does not collect device GPS or background location data.
- Camera and Notification permissions are strictly optional and only requested if you choose to use camera capture or reminders.

4. THIRD-PARTY SERVICES
- Cloud AI Services & Visual Preview Rendering: Powers LITHFIT AI styling advice, garment identification, and key-free outfit preview illustrations.
- Open-Meteo API: Receives only a city name you voluntarily enter (never your name or device GPS) to return weather forecasts for outfit advice.

5. UNDER-18 PRIVACY PROTECTIONS
- Users under 18 receive a private-by-default experience with zero public discovery exposure.

6. DATA RETENTION, EXPORT & ACCOUNT DELETION
- You can export a summary of your wardrobe and outfits or permanently delete your local account and wardrobe records at any time from Profile > Account Management.

7. CONTACT CLASTAD
- Visit https://clastad.xyz (Clastad.xyz) for support or privacy enquiries.
    """.trimIndent()

    val termsOfServiceText = """
LITHFIT TERMS OF SERVICE
Effective Date: November 2026 | Version $TERMS_VERSION
Operated by CLASTAD • Clastad.xyz

1. ACCEPTANCE OF TERMS
By creating a LITHFIT account or accessing the application, you agree to these Terms of Service and our Community Guidelines.

2. FREE CORE ACCESS
LITHFIT provides wardrobe management, avatar customization, outfit building, Style Radar discovery, weather styling, and LITHFIT AI styling tools free of charge without coin, credit, or token paywalls. Temporary fair-use capacity limits may apply to online generation services.

3. YOUR CONTENT & INTELLECTUAL PROPERTY
- You retain ownership of original wardrobe photos and outfit notes you upload.
- You must only upload or import images that you have the right or permission to use.
- LITHFIT wordmarks, emblem, UI design, and original editorial assets remain the intellectual property of CLASTAD.

4. AI STYLING & GENERATED PREVIEWS DISCLAIMER
- LITHFIT AI outfit suggestions and generated visual previews are styling illustrations for personal inspiration.
- Generated outfit previews do not represent verified photographs of physically purchasable merchandise.
- LITHFIT does not fabricate product stock, prices, or guarantees of retail availability.

5. STYLE RADAR & PARTICIPATING SELLERS
- Listings displayed in STYLE RADAR come solely from connected seller catalogues. Product availability, pricing, external website links, and fulfillment are managed independently by each verified seller.

6. PROHIBITED CONDUCT
- Do not impersonate brands, designers, or participating sellers.
- Do not upload unlawful, infringing, harmful, or non-consensual imagery.

7. ACCOUNT DELETION & SUPPORT
- You may delete your account at any time in Profile settings.
- Service is provided "as is" without warranties regarding uninterrupted third-party weather or online generation availability.
- Support & Enquiries: https://clastad.xyz
    """.trimIndent()

    val communityGuidelinesText = """
LITHFIT COMMUNITY & SAFETY GUIDELINES
Effective Date: November 2026 | Version $GUIDELINES_VERSION

1. BODY-NEUTRAL & INCLUSIVE STYLING
- LITHFIT celebrates personal expression, comfort, and practical styling for everyone.
- LITHFIT never scores attractiveness, ranks bodies, or promotes body-shaming or unhealthy body modification.

2. RESPECT COPYRIGHT & TRADEMARKS
- Only share or import clothing photographs and lookbook entries you own or are authorized to share.
- Never claim AI-generated apparel illustrations are authentic branded merchandise.
- Report suspected copyright infringement or impersonation using the Report action on any discovery card or in Profile > Legal, Safety & Credits.

3. YOUTH SAFETY
- Under-18 accounts are private by default with zero stranger messaging and zero location exposure.
- Keep styling entries respectful and age-appropriate.
    """.trimIndent()

    val creditsText = """
LITHFIT CREDITS & ATTRIBUTION

Founder and Creator: Jason Prince
Developed by CLASTAD
Website: https://clastad.xyz

Brand & Tagline:
LITHFIT — Your Style. Your World.

Built as an inclusive, fashion-forward styling studio and digital wardrobe companion for teenagers and adults across diverse fashion cultures worldwide.
    """.trimIndent()
}
