package com.example.model

import android.content.Context
import android.util.Log

object WebsiteSynthesisEngine {
    private const val TAG = "WebsiteSynthesisEngine"

    fun synthesizeAndLaunch(
        context: Context,
        prompt: String,
        openInChrome: Boolean = true
    ): Pair<Boolean, String> {
        val lower = prompt.lowercase().trim()

        val (title, htmlCode) = when {
            lower.contains("coaching") || lower.contains("tuition") || lower.contains("institute") || lower.contains("academy") || lower.contains("school") -> {
                Pair("Lakshya Premier Coaching & Academy", generateCoachingPortal())
            }
            lower.contains("e-commerce") || lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store") || lower.contains("bazaar") || lower.contains("cart") -> {
                Pair("AuraShop - Premier E-Commerce Platform", generateEcommercePortal())
            }
            lower.contains("lms") || lower.contains("learning management") || lower.contains("course") || lower.contains("class") -> {
                Pair("EduMaster LMS - Interactive Learning Platform", generateLmsPortal())
            }
            lower.contains("portfolio") || lower.contains("resume") || lower.contains("personal") -> {
                Pair("Rahul Sharma - Full-Stack Engineer & AI Architect", generatePortfolioPortal())
            }
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("food") || lower.contains("dhaba") -> {
                Pair("Zaika Grand Restaurant & Online Ordering", generateRestaurantPortal())
            }
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") || lower.contains("health") -> {
                Pair("Arogya Super-Speciality Hospital & Care", generateHospitalPortal())
            }
            else -> {
                val cleanTopic = prompt.replace(Regex("(?i)(maya|website|banao|bana do|ek|professional|complete|full-scale)"), "").trim().ifBlank { "Modern Web Enterprise" }
                Pair("$cleanTopic Enterprise Web Platform", generateUniversalEnterprisePortal(cleanTopic))
            }
        }

        val resultMsg = CodeStudioManager.buildAndLaunchWebsite(
            context = context,
            title = title,
            htmlContent = htmlCode,
            openInChrome = openInChrome
        )

        val isSuccess = !resultMsg.startsWith("Error", ignoreCase = true)
        return Pair(isSuccess, resultMsg)
    }

    // =========================================================================
    // 1. COACHING & EDUCATIONAL INSTITUTE FULL WEB PRODUCT
    // =========================================================================
    private fun generateCoachingPortal(): String = """
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lakshya Premier Institute - Engineering, Medical & Foundation</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-dark: #3730a3;
            --secondary: #06b6d4;
            --accent: #f59e0b;
            --success: #10b981;
            --danger: #ef4444;
            --bg: #0f172a;
            --card-bg: #1e293b;
            --card-hover: #334155;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --border: #334155;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Poppins', sans-serif; transition: all 0.2s ease; }
        body { background: var(--bg); color: var(--text-main); line-height: 1.6; }
        
        /* Navigation Bar */
        header { background: rgba(15, 23, 42, 0.95); backdrop-filter: blur(10px); position: sticky; top: 0; z-index: 100; border-bottom: 1px solid var(--border); }
        .nav-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; }
        .logo { display: flex; align-items: center; gap: 10px; font-size: 1.3rem; font-weight: 800; color: #fff; text-decoration: none; }
        .logo span { color: var(--accent); }
        .nav-menu { display: flex; gap: 8px; list-style: none; align-items: center; }
        .nav-btn { background: transparent; border: none; color: var(--text-muted); padding: 8px 14px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; font-weight: 500; }
        .nav-btn:hover, .nav-btn.active { color: #fff; background: var(--card-bg); }
        .nav-btn.admin-link { background: #312e81; color: #a5b4fc; }
        .nav-btn.admin-link:hover { background: var(--primary); color: #fff; }
        .admission-btn { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: #fff; padding: 9px 18px; border-radius: 20px; border: none; font-weight: 600; cursor: pointer; }
        .admission-btn:hover { transform: scale(1.04); }
        
        /* Container */
        .main-wrapper { max-width: 1200px; margin: 0 auto; padding: 24px 20px; }
        .tab-content { display: none; }
        .tab-content.active { display: block; animation: fadeIn 0.3s ease-in-out; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

        /* Hero */
        .hero { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 30px; align-items: center; padding: 40px 0; }
        .hero-badge { display: inline-block; background: rgba(79, 70, 229, 0.2); color: #818cf8; border: 1px solid #4f46e5; padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; margin-bottom: 16px; }
        .hero h1 { font-size: 2.8rem; font-weight: 800; line-height: 1.2; margin-bottom: 16px; }
        .hero h1 span { background: linear-gradient(to right, #818cf8, #38bdf8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .hero p { color: var(--text-muted); font-size: 1.05rem; margin-bottom: 24px; }
        .hero-actions { display: flex; gap: 14px; flex-wrap: wrap; }
        .primary-btn { background: var(--primary); color: #fff; border: none; padding: 12px 24px; border-radius: 10px; font-weight: 600; cursor: pointer; font-size: 0.95rem; }
        .primary-btn:hover { background: var(--primary-dark); }
        .secondary-btn { background: var(--card-bg); border: 1px solid var(--border); color: #fff; padding: 12px 24px; border-radius: 10px; font-weight: 600; cursor: pointer; }
        .secondary-btn:hover { background: var(--card-hover); }

        /* Stats Bar */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 30px 0; }
        .stat-card { background: var(--card-bg); border: 1px solid var(--border); padding: 20px; border-radius: 14px; text-align: center; }
        .stat-num { font-size: 2rem; font-weight: 800; color: var(--accent); }
        .stat-label { color: var(--text-muted); font-size: 0.85rem; margin-top: 4px; }

        /* Courses Section */
        .section-header { text-align: center; margin-bottom: 30px; }
        .section-header h2 { font-size: 2rem; font-weight: 700; margin-bottom: 8px; }
        .section-header p { color: var(--text-muted); }
        .filter-bar { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-bottom: 24px; }
        .filter-btn { background: var(--card-bg); border: 1px solid var(--border); color: var(--text-muted); padding: 8px 18px; border-radius: 20px; cursor: pointer; font-size: 0.85rem; }
        .filter-btn.active, .filter-btn:hover { background: var(--primary); color: #fff; border-color: var(--primary); }
        .search-box { width: 100%; max-width: 450px; margin: 0 auto 24px auto; position: relative; }
        .search-input { width: 100%; background: var(--card-bg); border: 1px solid var(--border); padding: 12px 16px; border-radius: 12px; color: #fff; font-size: 0.95rem; outline: none; }
        .search-input:focus { border-color: var(--primary); }

        .courses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
        .course-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; }
        .course-card:hover { transform: translateY(-4px); border-color: #6366f1; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }
        .course-img { height: 160px; background: linear-gradient(135deg, #1e1b4b, #312e81); display: flex; align-items: center; justify-content: center; font-size: 3rem; }
        .course-body { padding: 20px; flex: 1; display: flex; flex-direction: column; }
        .course-tag { display: inline-block; background: rgba(6, 182, 212, 0.15); color: #38bdf8; font-size: 0.75rem; font-weight: 700; padding: 4px 10px; border-radius: 6px; margin-bottom: 10px; align-self: flex-start; }
        .course-title { font-size: 1.15rem; font-weight: 700; margin-bottom: 8px; }
        .course-desc { color: var(--text-muted); font-size: 0.85rem; margin-bottom: 16px; flex: 1; }
        .course-meta { display: flex; justify-content: space-between; align-items: center; padding-top: 14px; border-top: 1px solid var(--border); }
        .course-price { font-size: 1.25rem; font-weight: 800; color: #34d399; }
        .enroll-card-btn { background: var(--primary); color: #fff; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 0.85rem; }

        /* Online Test / Quiz Simulator */
        .quiz-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 16px; padding: 24px; max-width: 750px; margin: 0 auto; }
        .quiz-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 14px; }
        .quiz-timer { background: #7f1d1d; color: #fca5a5; font-weight: 700; padding: 6px 12px; border-radius: 8px; font-size: 0.85rem; }
        .quiz-opt { background: #0f172a; border: 1px solid var(--border); padding: 12px 16px; border-radius: 10px; margin-bottom: 10px; cursor: pointer; display: flex; align-items: center; gap: 10px; }
        .quiz-opt:hover { background: #1e293b; border-color: var(--primary); }
        .quiz-opt.selected { background: #312e81; border-color: #818cf8; }

        /* Forms */
        .form-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 18px; padding: 30px; max-width: 650px; margin: 0 auto; }
        .form-group { margin-bottom: 18px; }
        .form-label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 6px; color: var(--text-muted); }
        .form-control { width: 100%; background: #0f172a; border: 1px solid var(--border); padding: 12px 14px; border-radius: 10px; color: #fff; font-size: 0.95rem; outline: none; }
        .form-control:focus { border-color: var(--primary); }
        .error-msg { color: var(--danger); font-size: 0.8rem; margin-top: 4px; display: none; }

        /* Admin Panel */
        .admin-grid { display: grid; grid-template-columns: 260px 1fr; gap: 24px; }
        .admin-sidebar { background: var(--card-bg); border: 1px solid var(--border); border-radius: 14px; padding: 18px; height: fit-content; }
        .admin-tab-btn { width: 100%; text-align: left; background: transparent; border: none; color: var(--text-muted); padding: 10px 14px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; margin-bottom: 6px; }
        .admin-tab-btn.active, .admin-tab-btn:hover { background: var(--primary); color: #fff; }
        .table-responsive { overflow-x: auto; background: var(--card-bg); border: 1px solid var(--border); border-radius: 14px; }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th, td { padding: 14px 18px; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
        th { background: #0f172a; color: var(--text-muted); font-weight: 600; }
        .status-badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; }
        .status-active { background: rgba(16, 185, 129, 0.2); color: #34d399; }
        .delete-btn { background: #ef4444; border: none; color: #fff; padding: 4px 10px; border-radius: 6px; cursor: pointer; font-size: 0.75rem; }

        /* Toast */
        #toast { position: fixed; bottom: 24px; right: 24px; background: #10b981; color: #fff; padding: 14px 22px; border-radius: 12px; font-weight: 600; display: none; z-index: 1000; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }

        @media (max-width: 768px) {
            .hero { grid-template-columns: 1fr; text-align: center; }
            .hero-actions { justify-content: center; }
            .stats-grid { grid-template-columns: 1fr 1fr; }
            .admin-grid { grid-template-columns: 1fr; }
            .nav-menu { display: none; }
        }
    </style>
</head>
<body>

    <header>
        <div class="nav-container">
            <a href="#" class="logo" onclick="switchTab('home')">
                🎓 <span>Lakshya</span> Academy
            </a>
            <ul class="nav-menu">
                <li><button class="nav-btn active" onclick="switchTab('home')">Home</button></li>
                <li><button class="nav-btn" onclick="switchTab('courses')">Courses</button></li>
                <li><button class="nav-btn" onclick="switchTab('admission')">Admission</button></li>
                <li><button class="nav-btn" onclick="switchTab('testseries')">Test Series</button></li>
                <li><button class="nav-btn" onclick="switchTab('contact')">Contact</button></li>
                <li><button class="nav-btn admin-link" onclick="switchTab('admin')">⚙️ Admin Panel</button></li>
            </ul>
            <button class="admission-btn" onclick="switchTab('admission')">Apply Online</button>
        </div>
    </header>

    <div class="main-wrapper">

        <!-- TAB 1: HOME -->
        <div id="tab-home" class="tab-content active">
            <section class="hero">
                <div>
                    <span class="hero-badge">🌟 Admissions Open for Batch 2026-27</span>
                    <h1>India's Most Trusted <span>JEE, NEET & Olympiad</span> Coaching</h1>
                    <p>Experienced top faculty, daily mock testing, personalized mentorship, and comprehensive digital study materials to guarantee academic excellence.</p>
                    <div class="hero-actions">
                        <button class="primary-btn" onclick="switchTab('courses')">Explore Courses 🚀</button>
                        <button class="secondary-btn" onclick="switchTab('testseries')">Free Mock Test 📝</button>
                    </div>
                </div>
                <div style="background: linear-gradient(135deg, #1e1b4b, #312e81); border-radius: 20px; padding: 30px; text-align: center; border: 1px solid var(--border);">
                    <div style="font-size: 4rem;">🏆</div>
                    <h3 style="margin-top: 10px; font-size: 1.4rem;">AIR 1, 4, 9 in JEE 2025</h3>
                    <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 6px;">Consistently producing nationwide toppers through our dedicated research-backed pedagogy.</p>
                </div>
            </section>

            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-num">12,500+</div>
                    <div class="stat-label">Students Selected</div>
                </div>
                <div class="stat-card">
                    <div class="stat-num">98.4%</div>
                    <div class="stat-label">Success Rate</div>
                </div>
                <div class="stat-card">
                    <div class="stat-num">45+</div>
                    <div class="stat-label">Expert IITian Mentors</div>
                </div>
                <div class="stat-card">
                    <div class="stat-num">100%</div>
                    <div class="stat-label">Doubt Clearance</div>
                </div>
            </div>
        </div>

        <!-- TAB 2: COURSES -->
        <div id="tab-courses" class="tab-content">
            <div class="section-header">
                <h2>Our Classroom & Hybrid Programs</h2>
                <p>Curated by top educators with interactive live classes, test series, and printed modules.</p>
            </div>

            <div class="search-box">
                <input type="text" id="courseSearch" class="search-input" placeholder="Search JEE, NEET, Class 11, Crash Course..." oninput="filterCourses()">
            </div>

            <div class="filter-bar">
                <button class="filter-btn active" onclick="setCourseCategory('ALL', this)">All Programs</button>
                <button class="filter-btn" onclick="setCourseCategory('JEE', this)">IIT-JEE Main/Adv</button>
                <button class="filter-btn" onclick="setCourseCategory('NEET', this)">NEET Medical</button>
                <button class="filter-btn" onclick="setCourseCategory('FOUNDATION', this)">Class 9-10 Foundation</button>
            </div>

            <div id="coursesContainer" class="courses-grid">
                <!-- Injected via JavaScript -->
            </div>
        </div>

        <!-- TAB 3: ADMISSION FORM -->
        <div id="tab-admission" class="tab-content">
            <div class="section-header">
                <h2>Online Admission Application 2026</h2>
                <p>Fill the form to secure your seat. Instant confirmation and student ID generation.</p>
            </div>

            <div class="form-card">
                <form id="admissionForm" onsubmit="handleAdmissionSubmit(event)">
                    <div class="form-group">
                        <label class="form-label">Full Name of Student *</label>
                        <input type="text" id="admName" class="form-control" placeholder="e.g. Rahul Sharma" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Parent / Guardian Contact Number *</label>
                        <input type="tel" id="admPhone" class="form-control" placeholder="10-digit Mobile Number" required pattern="[0-9]{10}">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Email Address *</label>
                        <input type="email" id="admEmail" class="form-control" placeholder="student@example.com" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Select Target Stream *</label>
                        <select id="admStream" class="form-control" required>
                            <option value="">-- Choose Course --</option>
                            <option value="IIT-JEE 2-Year Target Batch">IIT-JEE 2-Year Target Batch (Class 11)</option>
                            <option value="NEET Medical Super 30">NEET Medical Super 30 (Class 12/Dropper)</option>
                            <option value="Foundation NTSE/Olympiad">Foundation NTSE/Olympiad (Class 9-10)</option>
                            <option value="JEE Crash Course 2026">JEE Crash Course 2026</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Current School / College Name</label>
                        <input type="text" id="admSchool" class="form-control" placeholder="e.g. DPS International">
                    </div>
                    <button type="submit" class="primary-btn" style="width: 100%; margin-top: 10px;">Submit Application & Generate Receipt 📄</button>
                </form>
            </div>
        </div>

        <!-- TAB 4: TEST SERIES / LIVE QUIZ SIMULATOR -->
        <div id="tab-testseries" class="tab-content">
            <div class="section-header">
                <h2>Real-Time Practice Mock Test</h2>
                <p>Experience the NTA computer-based testing interface with immediate AI analysis.</p>
            </div>

            <div class="quiz-card" id="quizContainer">
                <div class="quiz-header">
                    <div>
                        <h3 id="quizSubject">Physics - JEE Main Practice</h3>
                        <span id="quizCounter" style="font-size: 0.85rem; color: var(--text-muted);">Question 1 of 3</span>
                    </div>
                    <div class="quiz-timer" id="quizTimer">Time Left: 02:45</div>
                </div>

                <div id="quizQuestionBody">
                    <p id="quizQuestionText" style="font-size: 1.05rem; font-weight: 500; margin-bottom: 20px;">
                        A body of mass 2 kg moving at 10 m/s collides inelastically with a stationary body of mass 3 kg. What is the common velocity after impact?
                    </p>
                    <div id="quizOptions">
                        <!-- Options injected -->
                    </div>
                </div>

                <div style="display: flex; justify-content: space-between; margin-top: 24px; border-top: 1px solid var(--border); padding-top: 16px;">
                    <button class="secondary-btn" onclick="prevQuestion()">Previous</button>
                    <button class="primary-btn" id="nextQuizBtn" onclick="nextQuestion()">Next Question ➡️</button>
                </div>
            </div>
        </div>

        <!-- TAB 5: CONTACT -->
        <div id="tab-contact" class="tab-content">
            <div class="section-header">
                <h2>Reach Out To Our Campus</h2>
                <p>Have questions regarding scholarships, hostel facilities, or syllabus? We are here 24x7.</p>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 30px; max-width: 900px; margin: 0 auto;">
                <div style="background: var(--card-bg); padding: 24px; border-radius: 16px; border: 1px solid var(--border);">
                    <h3 style="margin-bottom: 12px;">📍 Main Campus Address</h3>
                    <p style="color: var(--text-muted); font-size: 0.95rem; margin-bottom: 20px;">Lakshya Tower, Knowledge Park III, Greater Noida, Delhi NCR - 201306</p>
                    
                    <h3 style="margin-bottom: 12px;">📞 Admissions Helpline</h3>
                    <p style="color: var(--accent); font-weight: 700; font-size: 1.1rem; margin-bottom: 20px;">+91 98765 43210 / 1800-123-4567</p>

                    <h3 style="margin-bottom: 12px;">⏰ Visiting Hours</h3>
                    <p style="color: var(--text-muted); font-size: 0.95rem;">Monday - Saturday: 8:00 AM - 8:00 PM<br>Sunday: 9:00 AM - 4:00 PM</p>
                </div>
                <div class="form-card" style="margin: 0;">
                    <h3 style="margin-bottom: 16px;">Send Message</h3>
                    <form onsubmit="handleContactSubmit(event)">
                        <div class="form-group">
                            <label class="form-label">Your Name</label>
                            <input type="text" class="form-control" required placeholder="Name">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Phone Number</label>
                            <input type="tel" class="form-control" required placeholder="Mobile">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Query / Message</label>
                            <textarea class="form-control" rows="3" required placeholder="How can we help?"></textarea>
                        </div>
                        <button type="submit" class="primary-btn" style="width: 100%;">Send Message</button>
                    </form>
                </div>
            </div>
        </div>

        <!-- TAB 6: ADMIN PANEL -->
        <div id="tab-admin" class="tab-content">
            <div class="section-header">
                <h2>Institute Administration & Database</h2>
                <p>Live student admissions, course manager, inquiries, and revenue telemetry.</p>
            </div>

            <div class="admin-grid">
                <div class="admin-sidebar">
                    <button class="admin-tab-btn active" onclick="switchAdminSubTab('admissions')">📋 Student Registrations</button>
                    <button class="admin-tab-btn" onclick="switchAdminSubTab('courses')">📚 Manage Courses</button>
                    <button class="admin-tab-btn" onclick="switchAdminSubTab('analytics')">📊 Analytics & Fees</button>
                </div>

                <div class="admin-main">
                    <!-- Subtab Admissions -->
                    <div id="admin-sub-admissions">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                            <h3>Recent Student Applications</h3>
                            <button class="secondary-btn" onclick="clearAllAdmissions()" style="font-size: 0.8rem; padding: 6px 12px;">Clear Database</button>
                        </div>
                        <div class="table-responsive">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Student ID</th>
                                        <th>Name</th>
                                        <th>Course</th>
                                        <th>Phone</th>
                                        <th>Date</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody id="admissionsTableBody">
                                    <!-- Dynamic Rows -->
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- Subtab Manage Courses -->
                    <div id="admin-sub-courses" style="display: none;">
                        <h3 style="margin-bottom: 16px;">Add New Batch / Course</h3>
                        <div class="form-card" style="margin: 0 0 20px 0; max-width: 100%;">
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px;">
                                <input type="text" id="newCourseTitle" class="form-control" placeholder="Course Title (e.g. Olympiad Rankers)">
                                <input type="text" id="newCourseCat" class="form-control" placeholder="Category (JEE, NEET, FOUNDATION)">
                                <input type="text" id="newCoursePrice" class="form-control" placeholder="Fees (e.g. ₹45,000)">
                                <input type="text" id="newCourseDesc" class="form-control" placeholder="Short Description">
                            </div>
                            <button class="primary-btn" onclick="addNewCourseFromAdmin()" style="margin-top: 14px;">+ Add Course to Public Catalog</button>
                        </div>
                    </div>

                    <!-- Subtab Analytics -->
                    <div id="admin-sub-analytics" style="display: none;">
                        <div class="stats-grid" style="margin: 0 0 20px 0;">
                            <div class="stat-card">
                                <div class="stat-num" id="totalStudentsStat">0</div>
                                <div class="stat-label">Total Registered Students</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-num" id="totalFeeStat">₹0</div>
                                <div class="stat-label">Projected Revenue</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- Notification Toast -->
    <div id="toast">Application Submitted Successfully!</div>

    <script>
        // Data Store
        let courses = [
            { id: 1, category: "JEE", title: "IIT-JEE 2-Year Target Batch", desc: "Complete syllabus coverage for JEE Main & Advanced with 120+ weekly simulated CBT tests.", price: "₹65,000", tag: "Most Popular" },
            { id: 2, category: "NEET", title: "NEET Medical Super 30", desc: "Rigorous NCERT line-by-line biology and high-yield physics/chemistry numerical mastery.", price: "₹58,000", tag: "High Selection" },
            { id: 3, category: "FOUNDATION", title: "Class 9-10 NTSE Foundation", desc: "Strong conceptual building in Science & Maths targeting Olympiads, KVPY, and NTSE.", price: "₹38,000", tag: "Foundation" },
            { id: 4, category: "JEE", title: "JEE Rank Booster Crash Course", desc: "Intensive 90-day revision course with previous 15 years solved papers and short-tricks.", price: "₹25,000", tag: "Fast Track" }
        ];

        let admissions = JSON.parse(localStorage.getItem('lakshya_admissions') || '[]');
        if (admissions.length === 0) {
            admissions = [
                { id: "LAK-1001", name: "Aryan Verma", course: "IIT-JEE 2-Year Target Batch", phone: "9876543210", date: "22/09/2026", status: "Confirmed" },
                { id: "LAK-1002", name: "Ananya Mishra", course: "NEET Medical Super 30", phone: "9812345678", date: "21/09/2026", status: "Confirmed" }
            ];
            localStorage.setItem('lakshya_admissions', JSON.stringify(admissions));
        }

        // Navigation
        function switchTab(tabId) {
            document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
            document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
            
            const target = document.getElementById('tab-' + tabId);
            if (target) target.classList.add('active');

            if (tabId === 'courses') renderCourses(courses);
            if (tabId === 'admin') renderAdmin();
        }

        // Render Courses
        let activeCategory = "ALL";
        function setCourseCategory(cat, btn) {
            activeCategory = cat;
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            filterCourses();
        }

        function filterCourses() {
            const query = document.getElementById('courseSearch').value.toLowerCase();
            const filtered = courses.filter(c => {
                const matchCat = (activeCategory === "ALL" || c.category === activeCategory);
                const matchQuery = c.title.toLowerCase().includes(query) || c.desc.toLowerCase().includes(query);
                return matchCat && matchQuery;
            });
            renderCourses(filtered);
        }

        function renderCourses(list) {
            const container = document.getElementById('coursesContainer');
            if (!container) return;
            if (list.length === 0) {
                container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">No courses matching your criteria.</div>';
                return;
            }
            container.innerHTML = list.map(function(c) {
                return '<div class="course-card">' +
                    '<div class="course-img">📚</div>' +
                    '<div class="course-body">' +
                        '<span class="course-tag">' + c.tag + '</span>' +
                        '<h3 class="course-title">' + c.title + '</h3>' +
                        '<p class="course-desc">' + c.desc + '</p>' +
                        '<div class="course-meta">' +
                            '<span class="course-price">' + c.price + '</span>' +
                            '<button class="enroll-card-btn" onclick="enrollCourse(\'' + c.title + '\')">Enroll Now</button>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            }).join('');
        }

        function enrollCourse(courseTitle) {
            switchTab('admission');
            const stream = document.getElementById('admStream');
            if (stream) stream.value = courseTitle;
        }

        // Form Handling
        function handleAdmissionSubmit(e) {
            e.preventDefault();
            const name = document.getElementById('admName').value;
            const phone = document.getElementById('admPhone').value;
            const course = document.getElementById('admStream').value;
            const newId = "LAK-" + Math.floor(1000 + Math.random() * 9000);

            const newAdm = {
                id: newId,
                name: name,
                phone: phone,
                course: course,
                date: new Date().toLocaleDateString(),
                status: "Confirmed"
            };

            admissions.unshift(newAdm);
            localStorage.setItem('lakshya_admissions', JSON.stringify(admissions));
            showToast("Admission Confirmed! Student ID: " + newId);
            document.getElementById('admissionForm').reset();
            setTimeout(() => switchTab('admin'), 1200);
        }

        function handleContactSubmit(e) {
            e.preventDefault();
            showToast("Thank you! Our academic counsellor will contact you shortly.");
            e.target.reset();
        }

        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.innerText = msg;
            toast.style.display = 'block';
            setTimeout(() => { toast.style.display = 'none'; }, 3000);
        }

        // Admin Management
        function renderAdmin() {
            const tbody = document.getElementById('admissionsTableBody');
            if (!tbody) return;
            if (admissions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No student records found.</td></tr>';
            } else {
                tbody.innerHTML = admissions.map(function(a) {
                    return '<tr>' +
                        '<td><strong>' + a.id + '</strong></td>' +
                        '<td>' + a.name + '</td>' +
                        '<td>' + a.course + '</td>' +
                        '<td>' + a.phone + '</td>' +
                        '<td>' + a.date + '</td>' +
                        '<td><span class="status-badge status-active">' + a.status + '</span></td>' +
                        '<td><button class="delete-btn" onclick="deleteAdmission(\'' + a.id + '\')">Remove</button></td>' +
                    '</tr>';
                }).join('');
            }

            document.getElementById('totalStudentsStat').innerText = admissions.length;
            document.getElementById('totalFeeStat').innerText = "₹" + (admissions.length * 55000).toLocaleString();
        }

        function deleteAdmission(id) {
            admissions = admissions.filter(a => a.id !== id);
            localStorage.setItem('lakshya_admissions', JSON.stringify(admissions));
            renderAdmin();
            showToast("Record removed.");
        }

        function clearAllAdmissions() {
            if (confirm("Are you sure you want to clear all admissions?")) {
                admissions = [];
                localStorage.setItem('lakshya_admissions', JSON.stringify(admissions));
                renderAdmin();
            }
        }

        function switchAdminSubTab(sub) {
            document.getElementById('admin-sub-admissions').style.display = sub === 'admissions' ? 'block' : 'none';
            document.getElementById('admin-sub-courses').style.display = sub === 'courses' ? 'block' : 'none';
            document.getElementById('admin-sub-analytics').style.display = sub === 'analytics' ? 'block' : 'none';
        }

        function addNewCourseFromAdmin() {
            const title = document.getElementById('newCourseTitle').value;
            const cat = document.getElementById('newCourseCat').value.toUpperCase() || "GENERAL";
            const price = document.getElementById('newCoursePrice').value || "₹30,000";
            const desc = document.getElementById('newCourseDesc').value || "Comprehensive coaching module.";

            if (!title) return alert("Please enter course title.");
            courses.push({ id: Date.now(), category: cat, title: title, desc: desc, price: price, tag: "New Batch" });
            showToast("Course Added Successfully!");
            document.getElementById('newCourseTitle').value = "";
            document.getElementById('newCourseDesc').value = "";
        }

        // Quiz Simulator
        const quizData = [
            { q: "A body of mass 2 kg moving at 10 m/s collides inelastically with a stationary body of mass 3 kg. What is the common velocity after impact?", opts: ["4 m/s", "5 m/s", "6 m/s", "2 m/s"], ans: 0 },
            { q: "Which organelle is known as the powerhouse of the cell?", opts: ["Ribosome", "Mitochondria", "Golgi apparatus", "Lysosome"], ans: 1 },
            { q: "What is the pH of a neutral solution at 25°C?", opts: ["5", "7", "9", "14"], ans: 1 }
        ];
        let currentQ = 0;

        function loadQuizQuestion(index) {
            const item = quizData[index];
            document.getElementById('quizCounter').innerText = "Question " + (index + 1) + " of " + quizData.length;
            document.getElementById('quizQuestionText').innerText = item.q;
            const optsContainer = document.getElementById('quizOptions');
            optsContainer.innerHTML = item.opts.map(function(opt, i) {
                var letter = String.fromCharCode(65 + i);
                return '<div class="quiz-opt" onclick="selectQuizOption(this, ' + i + ')">' +
                    '<span>' + letter + '.</span>' +
                    '<span>' + opt + '</span>' +
                '</div>';
            }).join('');
        }

        function selectQuizOption(el, index) {
            document.querySelectorAll('.quiz-opt').forEach(o => o.classList.remove('selected'));
            el.classList.add('selected');
        }

        function nextQuestion() {
            if (currentQ < quizData.length - 1) {
                currentQ++;
                loadQuizQuestion(currentQ);
            } else {
                showToast("Test completed! Score: 3/3 (100% Accuracy) 🌟");
            }
        }

        function prevQuestion() {
            if (currentQ > 0) {
                currentQ--;
                loadQuizQuestion(currentQ);
            }
        }

        // Init
        renderCourses(courses);
        loadQuizQuestion(0);
    </script>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 2. E-COMMERCE FULL WEB PRODUCT (CATALOG, CART, CHECKOUT, ADMIN)
    // =========================================================================
    private fun generateEcommercePortal(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AuraShop - Premier Multi-Category E-Commerce Experience</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #f43f5e;
            --primary-dark: #e11d48;
            --bg: #09090b;
            --card-bg: #18181b;
            --card-border: #27272a;
            --text-main: #fafafa;
            --text-muted: #a1a1aa;
            --accent: #38bdf8;
            --success: #22c55e;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', sans-serif; transition: all 0.2s ease; }
        body { background: var(--bg); color: var(--text-main); }

        header { background: rgba(9, 9, 11, 0.95); backdrop-filter: blur(10px); position: sticky; top: 0; z-index: 100; border-bottom: 1px solid var(--card-border); padding: 14px 24px; }
        .nav-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; gap: 20px; }
        .logo { font-size: 1.4rem; font-weight: 800; color: #fff; text-decoration: none; display: flex; align-items: center; gap: 8px; }
        .logo span { color: var(--primary); }
        .search-bar { flex: 1; max-width: 450px; position: relative; }
        .search-bar input { width: 100%; background: var(--card-bg); border: 1px solid var(--card-border); padding: 10px 16px; border-radius: 20px; color: #fff; outline: none; font-size: 0.9rem; }
        .search-bar input:focus { border-color: var(--primary); }
        .nav-actions { display: flex; align-items: center; gap: 14px; }
        .cart-trigger { background: var(--card-bg); border: 1px solid var(--card-border); color: #fff; padding: 8px 16px; border-radius: 20px; cursor: pointer; display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 0.9rem; }
        .cart-badge { background: var(--primary); color: #fff; font-size: 0.75rem; padding: 2px 7px; border-radius: 12px; }

        .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
        .hero-banner { background: linear-gradient(135deg, #27272a, #18181b); border: 1px solid var(--card-border); border-radius: 20px; padding: 40px; margin-bottom: 30px; display: flex; justify-content: space-between; align-items: center; }
        .hero-banner h1 { font-size: 2.4rem; font-weight: 800; line-height: 1.2; margin-bottom: 12px; }
        .hero-banner p { color: var(--text-muted); font-size: 1rem; margin-bottom: 20px; }

        /* Catalog Layout */
        .catalog-layout { display: grid; grid-template-columns: 240px 1fr; gap: 24px; }
        .filter-panel { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 16px; padding: 20px; height: fit-content; }
        .filter-title { font-size: 1rem; font-weight: 700; margin-bottom: 14px; border-bottom: 1px solid var(--card-border); padding-bottom: 8px; }
        .category-item { padding: 8px 12px; border-radius: 8px; color: var(--text-muted); cursor: pointer; font-size: 0.9rem; margin-bottom: 4px; }
        .category-item:hover, .category-item.active { background: #27272a; color: #fff; }

        .product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
        .product-card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; }
        .product-card:hover { transform: translateY(-4px); border-color: var(--primary); box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
        .product-img { height: 180px; background: #27272a; display: flex; align-items: center; justify-content: center; font-size: 3.5rem; }
        .product-body { padding: 16px; flex: 1; display: flex; flex-direction: column; }
        .product-cat { color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; font-weight: 700; margin-bottom: 4px; }
        .product-name { font-size: 1rem; font-weight: 700; margin-bottom: 8px; }
        .product-rating { color: #facc15; font-size: 0.85rem; margin-bottom: 12px; }
        .product-footer { display: flex; justify-content: space-between; align-items: center; margin-top: auto; }
        .product-price { font-size: 1.2rem; font-weight: 800; color: #fff; }
        .add-cart-btn { background: var(--primary); border: none; color: #fff; padding: 8px 14px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 0.85rem; }

        /* Cart Drawer */
        .cart-modal { position: fixed; top: 0; right: -420px; width: 400px; height: 100vh; background: #121215; border-left: 1px solid var(--card-border); z-index: 200; padding: 24px; display: flex; flex-direction: column; transition: right 0.3s ease; }
        .cart-modal.open { right: 0; }
        .cart-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16px; border-bottom: 1px solid var(--card-border); }
        .cart-items { flex: 1; overflow-y: auto; padding: 16px 0; }
        .cart-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--card-border); }
        .cart-footer { border-top: 1px solid var(--card-border); padding-top: 16px; }
        .checkout-btn { width: 100%; background: var(--success); color: #000; border: none; padding: 12px; border-radius: 10px; font-weight: 700; font-size: 1rem; cursor: pointer; }

        @media (max-width: 768px) {
            .catalog-layout { grid-template-columns: 1fr; }
            .hero-banner { flex-direction: column; text-align: center; }
            .cart-modal { width: 100%; }
        }
    </style>
</head>
<body>

    <header>
        <div class="nav-container">
            <a href="#" class="logo">⚡ Aura<span>Shop</span></a>
            <div class="search-bar">
                <input type="text" id="searchInput" placeholder="Search gadgets, electronics, fashion..." oninput="handleSearch()">
            </div>
            <div class="nav-actions">
                <button class="cart-trigger" onclick="toggleCart()">
                    🛒 Cart <span class="cart-badge" id="cartBadge">0</span>
                </button>
            </div>
        </div>
    </header>

    <div class="container">
        <div class="hero-banner">
            <div>
                <span style="color: var(--primary); font-weight: 700; font-size: 0.85rem; text-transform: uppercase;">Mega Tech Festival 2026</span>
                <h1>Upgrade Your Digital Life.</h1>
                <p>Curated smart gadgets, ultra-fast delivery, zero hidden fees, and verified authentic guarantee.</p>
            </div>
            <div style="font-size: 4rem;">🎧📱⌚</div>
        </div>

        <div class="catalog-layout">
            <div class="filter-panel">
                <div class="filter-title">Categories</div>
                <div class="category-item active" onclick="selectCategory('ALL', this)">All Products</div>
                <div class="category-item" onclick="selectCategory('AUDIO', this)">Audio & Headphones</div>
                <div class="category-item" onclick="selectCategory('SMART', this)">Smart Watches</div>
                <div class="category-item" onclick="selectCategory('LAPTOPS', this)">Laptops & PCs</div>
            </div>

            <div class="product-grid" id="productGrid">
                <!-- Products injected by JS -->
            </div>
        </div>
    </div>

    <!-- Sliding Shopping Cart -->
    <div class="cart-modal" id="cartModal">
        <div class="cart-header">
            <h3>Your Shopping Cart</h3>
            <button onclick="toggleCart()" style="background: none; border: none; color: #fff; font-size: 1.5rem; cursor: pointer;">✕</button>
        </div>
        <div class="cart-items" id="cartItemsContainer">
            <!-- Injected by JS -->
        </div>
        <div class="cart-footer">
            <div style="display: flex; justify-content: space-between; font-size: 1.1rem; font-weight: 700; margin-bottom: 16px;">
                <span>Total Amount:</span>
                <span id="cartTotal">₹0</span>
            </div>
            <button class="checkout-btn" onclick="checkout()">Proceed to Secure Checkout 🔒</button>
        </div>
    </div>

    <script>
        const products = [
            { id: 1, name: "Aura ANC Wireless Headphones", category: "AUDIO", price: 4999, rating: "★★★★★ (4.9)", icon: "🎧" },
            { id: 2, name: "Pro Ultra Titanium Smartwatch", category: "SMART", price: 6499, rating: "★★★★☆ (4.7)", icon: "⌚" },
            { id: 3, name: "Infinity M4 Power Laptop 16GB", category: "LAPTOPS", price: 68999, rating: "★★★★★ (5.0)", icon: "💻" },
            { id: 4, name: "Studio Bass Hi-Fi Earbuds", category: "AUDIO", price: 2199, rating: "★★★★☆ (4.5)", icon: "🎵" },
            { id: 5, name: "Titan Fit Pulse Tracker", category: "SMART", price: 3299, rating: "★★★★☆ (4.6)", icon: "🏃" },
            { id: 6, name: "Slim Airbook 512GB SSD", category: "LAPTOPS", price: 47990, rating: "★★★★★ (4.8)", icon: "🖥️" }
        ];

        let cart = JSON.parse(localStorage.getItem('aurashop_cart') || '[]');
        let activeCat = "ALL";

        function renderProducts(list) {
            const grid = document.getElementById('productGrid');
            if (list.length === 0) {
                grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">No items match your filter.</div>';
                return;
            }
            grid.innerHTML = list.map(function(p) {
                return '<div class="product-card">' +
                    '<div class="product-img">' + p.icon + '</div>' +
                    '<div class="product-body">' +
                        '<span class="product-cat">' + p.category + '</span>' +
                        '<h4 class="product-name">' + p.name + '</h4>' +
                        '<div class="product-rating">' + p.rating + '</div>' +
                        '<div class="product-footer">' +
                            '<span class="product-price">₹' + p.price.toLocaleString() + '</span>' +
                            '<button class="add-cart-btn" onclick="addToCart(' + p.id + ')">+ Add</button>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            }).join('');
        }

        function selectCategory(cat, el) {
            activeCat = cat;
            document.querySelectorAll('.category-item').forEach(c => c.classList.remove('active'));
            el.classList.add('active');
            filterProducts();
        }

        function handleSearch() {
            filterProducts();
        }

        function filterProducts() {
            const query = document.getElementById('searchInput').value.toLowerCase();
            const filtered = products.filter(p => {
                const matchCat = (activeCat === "ALL" || p.category === activeCat);
                const matchQuery = p.name.toLowerCase().includes(query);
                return matchCat && matchQuery;
            });
            renderProducts(filtered);
        }

        function toggleCart() {
            document.getElementById('cartModal').classList.toggle('open');
            renderCart();
        }

        function addToCart(id) {
            const prod = products.find(p => p.id === id);
            const existing = cart.find(c => c.id === id);
            if (existing) {
                existing.qty += 1;
            } else {
                cart.push({ ...prod, qty: 1 });
            }
            localStorage.setItem('aurashop_cart', JSON.stringify(cart));
            updateCartBadge();
            renderCart();
        }

        function updateCartBadge() {
            const totalCount = cart.reduce((sum, item) => sum + item.qty, 0);
            document.getElementById('cartBadge').innerText = totalCount;
        }

        function renderCart() {
            const container = document.getElementById('cartItemsContainer');
            if (cart.length === 0) {
                container.innerHTML = '<div style="color: var(--text-muted); text-align: center; margin-top: 40px;">Your cart is empty.</div>';
                document.getElementById('cartTotal').innerText = "₹0";
                return;
            }
            let total = 0;
            container.innerHTML = cart.map(function(item) {
                total += item.price * item.qty;
                return '<div class="cart-item">' +
                    '<div>' +
                        '<div style="font-weight: 700; font-size: 0.95rem;">' + item.name + '</div>' +
                        '<div style="color: var(--text-muted); font-size: 0.85rem;">₹' + item.price.toLocaleString() + ' × ' + item.qty + '</div>' +
                    '</div>' +
                    '<button onclick="removeFromCart(' + item.id + ')" style="background: none; border: none; color: #ef4444; cursor: pointer; font-size: 1.1rem;">🗑️</button>' +
                '</div>';
            }).join('');
            document.getElementById('cartTotal').innerText = "₹" + total.toLocaleString();
        }

        function removeFromCart(id) {
            cart = cart.filter(c => c.id !== id);
            localStorage.setItem('aurashop_cart', JSON.stringify(cart));
            updateCartBadge();
            renderCart();
        }

        function checkout() {
            if (cart.length === 0) return alert("Your cart is empty!");
            const orderId = "AURA-" + Math.floor(100000 + Math.random() * 900000);
            alert("Order Placed Successfully! 🎉\nOrder ID: " + orderId + "\nEstimated delivery in 2 business days.");
            cart = [];
            localStorage.setItem('aurashop_cart', JSON.stringify(cart));
            updateCartBadge();
            toggleCart();
        }

        // Init
        renderProducts(products);
        updateCartBadge();
    </script>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 3. LMS (LEARNING MANAGEMENT SYSTEM) WEB PRODUCT
    // =========================================================================
    private fun generateLmsPortal(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EduMaster LMS - Interactive Learning Platform</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root { --primary: #3b82f6; --bg: #0f172a; --panel: #1e293b; --border: #334155; --text: #f8fafc; --muted: #94a3b8; }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { background: var(--bg); color: var(--text); }
        header { background: var(--panel); border-bottom: 1px solid var(--border); padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; }
        .logo { font-size: 1.3rem; font-weight: 800; color: #fff; }
        .logo span { color: var(--primary); }
        .layout { display: grid; grid-template-columns: 280px 1fr; height: calc(100vh - 65px); }
        .sidebar { background: var(--panel); border-right: 1px solid var(--border); padding: 20px; overflow-y: auto; }
        .lesson-item { padding: 12px; border-radius: 8px; margin-bottom: 8px; cursor: pointer; background: #0f172a; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
        .lesson-item.active { background: #1e3a8a; border-color: var(--primary); }
        .content-area { padding: 30px; overflow-y: auto; }
        .video-box { height: 380px; background: #000; border-radius: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; margin-bottom: 24px; border: 1px solid var(--border); }
        .btn { background: var(--primary); color: #fff; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 600; cursor: pointer; }
    </style>
</head>
<body>
    <header>
        <div class="logo">📘 Edu<span>Master</span> LMS</div>
        <div><span>Logged in as: <strong>Rahul (Student)</strong></span></div>
    </header>
    <div class="layout">
        <div class="sidebar">
            <h4 style="margin-bottom: 16px; color: var(--muted); font-size: 0.85rem; text-transform: uppercase;">Course Modules (4/12 Done)</h4>
            <div class="lesson-item active" onclick="loadLesson(1)">
                <span>1. Introduction to Neural Networks</span>
                <span>✅</span>
            </div>
            <div class="lesson-item" onclick="loadLesson(2)">
                <span>2. Backpropagation & Gradient Descent</span>
                <span>⏳</span>
            </div>
            <div class="lesson-item" onclick="loadLesson(3)">
                <span>3. Transformers & Attention Mechanisms</span>
                <span>🔒</span>
            </div>
        </div>
        <div class="content-area">
            <h2 id="lessonTitle" style="margin-bottom: 14px;">1. Introduction to Neural Networks</h2>
            <div class="video-box">
                <div style="font-size: 4rem;">▶️</div>
                <div style="margin-top: 10px; color: var(--muted);">Interactive HD Video Lecture Player</div>
            </div>
            <h3>Lesson Notes & Key Takeaways</h3>
            <p style="color: var(--muted); margin-top: 8px; line-height: 1.7;">
                Neural networks form the computational backbone of modern artificial intelligence. Today we cover forward propagation, activation functions (ReLU, Sigmoid), and loss calculation.
            </p>
            <div style="margin-top: 24px;">
                <button class="btn" onclick="alert('Module marked as completed! Next lecture unlocked.')">Mark As Completed ✅</button>
            </div>
        </div>
    </div>
    <script>
        function loadLesson(num) {
            document.querySelectorAll('.lesson-item').forEach(el => el.classList.remove('active'));
            event.currentTarget.classList.add('active');
            if (num === 1) document.getElementById('lessonTitle').innerText = "1. Introduction to Neural Networks";
            if (num === 2) document.getElementById('lessonTitle').innerText = "2. Backpropagation & Gradient Descent";
            if (num === 3) document.getElementById('lessonTitle').innerText = "3. Transformers & Attention Mechanisms";
        }
    </script>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 4. PORTFOLIO & RESUME WEB PRODUCT
    // =========================================================================
    private fun generatePortfolioPortal(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rahul Sharma - Full-Stack Engineer & AI Architect</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
        body { background: #0a0a0c; color: #fff; line-height: 1.6; }
        .hero { min-height: 80vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; padding: 40px 20px; }
        h1 { font-size: 3.5rem; font-weight: 800; background: linear-gradient(135deg, #00f2fe, #4facfe); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        p { color: #888899; max-width: 600px; margin: 16px auto; font-size: 1.1rem; }
        .tags { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; justify-content: center; }
        .tag { background: #1c1c24; border: 1px solid #2e2e3a; padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; color: #00f2fe; }
        .projects { max-width: 1000px; margin: 40px auto; padding: 0 20px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-top: 20px; }
        .card { background: #141419; border: 1px solid #242430; border-radius: 16px; padding: 24px; }
        .btn { background: #00f2fe; color: #000; border: none; padding: 12px 24px; border-radius: 30px; font-weight: 700; cursor: pointer; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="hero">
        <div style="font-size: 4rem;">⚡</div>
        <h1>Rahul Sharma</h1>
        <p>Lead AI & Android Systems Architect building hyper-scalable platforms, autonomous assistants, and real-time audio neural engines.</p>
        <div class="tags">
            <span class="tag">Kotlin & Compose</span>
            <span class="tag">Next.js & React</span>
            <span class="tag">Gemini Multimodal Live</span>
            <span class="tag">Python & PyTorch</span>
        </div>
        <button class="btn" onclick="alert('CV downloaded!')">Download Resume (PDF) 📄</button>
    </div>
    <div class="projects">
        <h2>Selected Production Deployments</h2>
        <div class="grid">
            <div class="card">
                <h3>Maya AI Assistant</h3>
                <p style="color: #888899; font-size: 0.9rem; margin-top: 6px;">Android agentic assistant with realtime bidirectional audio, screen analysis, and voice-controlled web synthesis.</p>
            </div>
            <div class="card">
                <h3>Aura E-Commerce</h3>
                <p style="color: #888899; font-size: 0.9rem; margin-top: 6px;">Distributed commerce application serving 500k monthly active users with sub-50ms query latency.</p>
            </div>
            <div class="card">
                <h3>EduMaster LMS</h3>
                <p style="color: #888899; font-size: 0.9rem; margin-top: 6px;">High-concurrency online examination engine with proctoring telemetry.</p>
            </div>
        </div>
    </div>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 5. RESTAURANT & FOOD ORDERING PORTAL
    // =========================================================================
    private fun generateRestaurantPortal(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Zaika Grand - Royal Indian Cuisine & Online Table Reservation</title>
    <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600;800&family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Poppins', sans-serif; }
        body { background: #0c0a09; color: #f5f5f4; }
        h1, h2, h3 { font-family: 'Playfair Display', serif; }
        header { background: #1c1917; padding: 16px 24px; border-bottom: 1px solid #292524; display: flex; justify-content: space-between; align-items: center; }
        .hero { padding: 60px 20px; text-align: center; background: radial-gradient(circle at center, #292524, #0c0a09); }
        .hero h1 { font-size: 3rem; color: #f59e0b; margin-bottom: 12px; }
        .menu-grid { max-width: 1000px; margin: 40px auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; padding: 0 20px; }
        .dish-card { background: #1c1917; border: 1px solid #292524; border-radius: 14px; padding: 20px; text-align: center; }
        .dish-price { color: #f59e0b; font-weight: 700; font-size: 1.2rem; margin: 8px 0; }
        .btn { background: #f59e0b; color: #000; border: none; padding: 8px 18px; border-radius: 8px; font-weight: 700; cursor: pointer; }
    </style>
</head>
<body>
    <header>
        <h2 style="color: #f59e0b;">👑 Zaika Grand</h2>
        <button class="btn" onclick="alert('Table Reserved for 2 at 8:00 PM!')">Book Table 🍷</button>
    </header>
    <div class="hero">
        <h1>Authentic Royal Flavours</h1>
        <p style="color: #a8a29e;">Signature Mughlai, Tandoori Delicacies and Hand-ground aromatic spices.</p>
    </div>
    <div class="menu-grid">
        <div class="dish-card">
            <div style="font-size: 3rem;">🥘</div>
            <h3>Dum Biryani Handi</h3>
            <div class="dish-price">₹380</div>
            <button class="btn" onclick="alert('Added Dum Biryani to order!')">Order Online</button>
        </div>
        <div class="dish-card">
            <div style="font-size: 3rem;">🧈</div>
            <h3>Butter Paneer Royale</h3>
            <div class="dish-price">₹320</div>
            <button class="btn" onclick="alert('Added Butter Paneer to order!')">Order Online</button>
        </div>
        <div class="dish-card">
            <div style="font-size: 3rem;">🫓</div>
            <h3>Garlic Naan Basket</h3>
            <div class="dish-price">₹120</div>
            <button class="btn" onclick="alert('Added Garlic Naan to order!')">Order Online</button>
        </div>
    </div>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 6. HEALTHCARE & HOSPITAL PORTAL
    // =========================================================================
    private fun generateHospitalPortal(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Arogya Super-Speciality Hospital</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', sans-serif; }
        body { background: #f8fafc; color: #1e293b; }
        header { background: #0284c7; color: #fff; padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
        .hero { background: #e0f2fe; padding: 50px 20px; text-align: center; }
        .hero h1 { font-size: 2.5rem; color: #0369a1; }
        .dept-grid { max-width: 1000px; margin: 40px auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; padding: 0 20px; }
        .dept-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; text-align: center; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
        .btn { background: #0284c7; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 700; cursor: pointer; }
    </style>
</head>
<body>
    <header>
        <h2>🏥 Arogya Hospital</h2>
        <span>Emergency 24x7: <strong>108 / 102</strong></span>
    </header>
    <div class="hero">
        <h1>Comprehensive Care, Compassionate Healing</h1>
        <p style="color: #64748b; margin-top: 8px;">Over 40 super-specialities, advanced robotic surgery, and dedicated ICU.</p>
        <button class="btn" style="margin-top: 16px;" onclick="alert('Doctor Appointment Booked for tomorrow 10:00 AM!')">Book OPD Appointment</button>
    </div>
    <div class="dept-grid">
        <div class="dept-card">
            <div style="font-size: 2.5rem;">❤️</div>
            <h4 style="margin-top: 10px;">Cardiology</h4>
            <p style="color: #64748b; font-size: 0.85rem; margin-top: 4px;">24x7 Cath Lab & Heart Care</p>
        </div>
        <div class="dept-card">
            <div style="font-size: 2.5rem;">🧠</div>
            <h4 style="margin-top: 10px;">Neurology</h4>
            <p style="color: #64748b; font-size: 0.85rem; margin-top: 4px;">Advanced Neuro & Stroke ICU</p>
        </div>
        <div class="dept-card">
            <div style="font-size: 2.5rem;">🦴</div>
            <h4 style="margin-top: 10px;">Orthopaedics</h4>
            <p style="color: #64748b; font-size: 0.85rem; margin-top: 4px;">Joint Replacement & Trauma</p>
        </div>
    </div>
</body>
</html>
    """.trimIndent()

    // =========================================================================
    // 7. UNIVERSAL ENTERPRISE PORTAL
    // =========================================================================
    private fun generateUniversalEnterprisePortal(topic: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$topic - Next-Generation Enterprise Experience</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root { --primary: #6366f1; --bg: #09090b; --card: #18181b; --border: #27272a; --text: #fafafa; --muted: #a1a1aa; }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Poppins', sans-serif; }
        body { background: var(--bg); color: var(--text); }
        header { background: rgba(9,9,11,0.95); border-bottom: 1px solid var(--border); padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; }
        .logo { font-size: 1.3rem; font-weight: 800; color: #fff; }
        .logo span { color: var(--primary); }
        .hero { text-align: center; padding: 60px 20px; }
        .hero h1 { font-size: 3rem; font-weight: 800; margin-bottom: 12px; }
        .hero p { color: var(--muted); max-width: 650px; margin: 0 auto 24px auto; }
        .grid { max-width: 1100px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; padding: 20px; }
        .card { background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 24px; }
        .btn { background: var(--primary); color: #fff; border: none; padding: 10px 22px; border-radius: 8px; font-weight: 600; cursor: pointer; }
    </style>
</head>
<body>
    <header>
        <div class="logo">⚡ $topic <span>Hub</span></div>
        <button class="btn" onclick="alert('Welcome to $topic!')">Get Started 🚀</button>
    </header>
    <div class="hero">
        <h1>Transforming $topic</h1>
        <p>Engineered for high performance, modern architecture, and customer satisfaction.</p>
        <button class="btn" onclick="alert('Inquiry recorded!')">Request Demo</button>
    </div>
    <div class="grid">
        <div class="card">
            <h3>🚀 Ultra Fast Architecture</h3>
            <p style="color: var(--muted); font-size: 0.9rem; margin-top: 8px;">Optimized for instantaneous loading, SEO compliance, and modern standards.</p>
        </div>
        <div class="card">
            <h3>🔒 Enterprise Security</h3>
            <p style="color: var(--muted); font-size: 0.9rem; margin-top: 8px;">Complete data privacy, end-to-end encryption, and role-based permissions.</p>
        </div>
        <div class="card">
            <h3>📱 Fully Responsive</h3>
            <p style="color: var(--muted); font-size: 0.9rem; margin-top: 8px;">Designed for mobile, tablet, desktop, and large displays seamlessly.</p>
        </div>
    </div>
</body>
</html>
    """.trimIndent()
}
