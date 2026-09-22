package com.example.model

data class MedicineDetails(
    val name: String,
    val saltComposition: String,
    val category: String,
    val usesInHindi: String,
    val usesInEnglish: String,
    val commonDosageForm: String,
    val commonPrecautions: String
)

object MedicineKnowledgeEngine {

    const val MANDATORY_DOCTOR_DISCLAIMER_HINDI = 
        "⚠️ महत्वपूर्ण चेतावनी: किसी भी दवा का उपयोग करने से पहले कृपया अपने डॉक्टर या योग्य मेडिकल प्रैक्टिशनर से परामर्श अवश्य लें। बिना डॉक्टर की सलाह के कोई भी दवा न लें।"

    const val MANDATORY_DOCTOR_DISCLAIMER_HINGLISH = 
        "⚠️ Zaroori Salah: Dawa lene se pehle kripya doctor ya certified medical practitioner se consult zaroor karein. Bina doctor ki salah ke koi dawa na lein."

    private val medicineDatabase = mapOf(
        "paracetamol" to MedicineDetails(
            name = "Paracetamol (Dolo 650 / Crocin / Calpol)",
            saltComposition = "Paracetamol (Acetaminophen)",
            category = "Antipyretic & Analgesic (बुखार और दर्द निवारक)",
            usesInHindi = "बुखार कम करने (Fever), सिरदर्द, बदन दर्द, दांत दर्द, और हल्के जोड़ों के दर्द में उपयोग की जाती है।",
            usesInEnglish = "Used for reducing fever and relieving mild to moderate pain (headache, body ache, toothache).",
            commonDosageForm = "Tablet, Syrup, Drops",
            commonPrecautions = "लिवर की समस्या में सावधानी बरतें। 24 घंटे में अत्यधिक मात्रा न लें।"
        ),
        "dolo" to MedicineDetails(
            name = "Dolo 650",
            saltComposition = "Paracetamol 650mg",
            category = "Antipyretic & Analgesic",
            usesInHindi = "तेज बुखार (High Fever) और पूरे शरीर के दर्द से राहत दिलाने में काम आती है।",
            usesInEnglish = "Relieves high fever, viral fever symptoms, and general body ache.",
            commonDosageForm = "Tablet 650mg",
            commonPrecautions = "खाली पेट अत्यधिक न लें। डॉक्टर द्वारा सुझाई खुराक ही लें।"
        ),
        "azithromycin" to MedicineDetails(
            name = "Azithromycin (Azee / Azithral)",
            saltComposition = "Azithromycin",
            category = "Macrolide Antibiotic (एंटीबायोटिक)",
            usesInHindi = "गले में खराश/इन्फेक्शन (Throat infection), टॉन्सिल, छाती/फेफड़ों के इन्फेक्शन, साइनस, और बैक्टीरिया जनित संक्रमण में दी जाती है।",
            usesInEnglish = "Used to treat bacterial infections of throat, tonsils, respiratory tract, and sinuses.",
            commonDosageForm = "Tablet 250mg/500mg, Suspension",
            commonPrecautions = "यह एंटीबायोटिक है, इसे डॉक्टर के पर्चे और पूरे कोर्स के अनुसार ही पूरा करें।"
        ),
        "pantoprazole" to MedicineDetails(
            name = "Pantoprazole (Pan 40 / Pantocid / Pantop-D)",
            saltComposition = "Pantoprazole (often combined with Domperidone in Pan-D)",
            category = "Proton Pump Inhibitor (PPI - एंटासिड / गैस की दवा)",
            usesInHindi = "पेट में ज्यादा एसिड बनने, एसिडिटी, सीने में जलन (Heartburn), गैस, पेट के अल्सर और GERD में दी जाती है।",
            usesInEnglish = "Reduces stomach acid, treats GERD, acid reflux, heartburn, and peptic ulcers.",
            commonDosageForm = "Tablet 40mg, Capsule",
            commonPrecautions = "आमतौर पर सुबह खाली पेट (नाश्ते से आधा घंटा पहले) ली जाती है।"
        ),
        "combiflam" to MedicineDetails(
            name = "Combiflam",
            saltComposition = "Ibuprofen + Paracetamol",
            category = "NSAID + Analgesic (दर्द और सूजन की दवा)",
            usesInHindi = "सिरदर्द, दांत दर्द, मांसपेशियों का दर्द, जोड़ों का दर्द, पीरियड्स के दर्द और सूजन से राहत के लिए।",
            usesInEnglish = "Relieves acute pain, swelling, toothache, muscle aches, and headache.",
            commonDosageForm = "Tablet",
            commonPrecautions = "पेट में अल्सर या एसिडिटी होने पर बिना डॉक्टर सलाह न लें। हमेशा भोजन के बाद लें।"
        ),
        "cetirizine" to MedicineDetails(
            name = "Cetirizine (Cetzine / Okacet / Alerid)",
            saltComposition = "Cetirizine Hydrochloride",
            category = "Antihistamine (एंटी-एलर्जी)",
            usesInHindi = "एलर्जी, छींक आना, बहती नाक, आंखों में खुजली या पानी आना, और त्वचा पर खुजली/पित्ती (Hives) में काम आती है।",
            usesInEnglish = "Treats allergic symptoms: runny nose, sneezing, watery eyes, and skin itching.",
            commonDosageForm = "Tablet 10mg, Syrup",
            commonPrecautions = "इसे लेने के बाद हल्की नींद या सुस्ती आ सकती है, गाड़ी चलाते समय सावधानी रखें।"
        ),
        "amoxicillin" to MedicineDetails(
            name = "Amoxicillin (Augmentin / Mox)",
            saltComposition = "Amoxicillin (often + Clavulanic Acid)",
            category = "Penicillin Antibiotic (एंटीबायोटिक)",
            usesInHindi = "कान, नाक, गले, दांतों के इन्फेक्शन, यूरिनरी ट्रैक्ट (UTI) और छाती के बैक्टीरियल इन्फेक्शन में उपयोग होती है।",
            usesInEnglish = "Treats various bacterial infections including dental, ear, throat, and urinary tract infections.",
            commonDosageForm = "Tablet, Capsule, Syrup",
            commonPrecautions = "पेनिसिलिन एलर्जी होने पर न लें। पूरा कोर्स डॉक्टर की सलाह से लें।"
        ),
        "ondansetron" to MedicineDetails(
            name = "Ondansetron (Emeset / Vomikind)",
            saltComposition = "Ondansetron",
            category = "Antiemetic (उल्टी और मतली रोकने की दवा)",
            usesInHindi = "उल्टी (Vomiting) और जी मिचलाने (Nausea) को तुरंत रोकने में काम आती है।",
            usesInEnglish = "Prevents and treats nausea and vomiting.",
            commonDosageForm = "Tablet (MD), Syrup, Injection",
            commonPrecautions = "डॉक्टर द्वारा निर्देशित मात्रा में ही लें।"
        ),
        "metformin" to MedicineDetails(
            name = "Metformin (Glycomet)",
            saltComposition = "Metformin Hydrochloride",
            category = "Antidiabetic (ब्लड शुगर / डायबिटीज की दवा)",
            usesInHindi = "टाइप 2 डायबिटीज में ब्लड शुगर लेवल को नियंत्रित करने के लिए उपयोग की जाती है।",
            usesInEnglish = "Controls high blood sugar levels in Type 2 Diabetes Mellitus.",
            commonDosageForm = "Tablet 500mg/850mg/1000mg",
            commonPrecautions = "डॉक्टर की नियमित निगरानी और ब्लड शुगर टेस्ट के साथ ही लें।"
        ),
        "telmisartan" to MedicineDetails(
            name = "Telmisartan (Telma 40)",
            saltComposition = "Telmisartan",
            category = "Antihypertensive (हाई ब्लड प्रेशर की दवा)",
            usesInHindi = "उच्च रक्तचाप (High Blood Pressure / Hypertension) को सामान्य बनाए रखने और दिल की सुरक्षा के लिए।",
            usesInEnglish = "Used to manage and lower high blood pressure and protect against heart attacks.",
            commonDosageForm = "Tablet 20mg/40mg/80mg",
            commonPrecautions = "दवा अचानक बंद न करें, केवल डॉक्टर के परामर्श अनुसार नियमित लें।"
        )
    )

    fun searchMedicine(query: String): String {
        val cleanQuery = query.lowercase().trim()
        val foundKey = medicineDatabase.keys.firstOrNull { key ->
            cleanQuery.contains(key) || key.contains(cleanQuery)
        }

        if (foundKey != null) {
            val med = medicineDatabase[foundKey]!!
            return """
                📋 दवा की जानकारी (Medicine Info):
                • नाम (Brand/Drug): ${med.name}
                • साल्ट कंपोज़िशन (Salt): ${med.saltComposition}
                • श्रेणी (Category): ${med.category}
                • किस काम आती है (Uses): ${med.usesInHindi}
                • खुराक रूप (Form): ${med.commonDosageForm}
                • सावधानियां (Precautions): ${med.commonPrecautions}
                
                $MANDATORY_DOCTOR_DISCLAIMER_HINGLISH
            """.trimIndent()
        }

        // Generic fallback for any other medicine
        return """
            📋 दवा की सामान्य जानकारी:
            दवा '$query' के बारे में सटीक उपयोग, साल्ट कंपोजीशन और सही डोज़ के लिए कृपया इसके लेबल को चेक करें।
            
            $MANDATORY_DOCTOR_DISCLAIMER_HINGLISH
        """.trimIndent()
    }
}
