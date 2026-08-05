# 📱 SMS Expense Tracker

A personal finance **Android app** (100% Java + XML) that **automatically reads your bank SMS messages** and turns them into a clean, organized expense tracker — no manual entry needed.

---

## 📌 What Does This App Do?

Every time your bank sends you an SMS like:

> *"Your a/c no. XX0046 debited for Rs.34.00 on 17-03-2026 19:17:24 trf to bharatpe@upi (RefNo 120174243512) - DNS Bank"*

This app:
1. **Reads** that SMS automatically (from inbox + in real-time)
2. **Extracts** the amount, date, type (credit/debit), and party
3. **Saves** it to a local SQLite database on your phone
4. **Displays** it in a clean list — latest transaction on top

You can also **add cash transactions manually** using the ➕ button.

---

## 🛠️ Tech Stack

| Technology | What It Is | Why We Used It |
|------------|-----------|----------------|
| **Java** | Programming language | 100% native Android — no Kotlin, no Flutter/Dart |
| **XML Layouts** | UI design files | Defines how every screen looks |
| **Android Studio** | Development IDE | Official IDE for building Android apps |
| **SQLite** | Local database | Stores all transactions on the phone — no internet needed |
| **RecyclerView** | UI component | Shows transactions in a smooth scrollable list |
| **ContentResolver** | Android API | Used to read SMS messages from the phone |
| **BroadcastReceiver** | Android component | Listens for incoming SMS in real time |
| **Regex** | Pattern matching | Extracts amount, date, party from SMS text |
| **ExecutorService + Handler** | Background threading | Scans SMS without freezing the UI (replaces deprecated AsyncTask) |
| **SharedPreferences** | Small data storage | Remembers if the full SMS scan has been done |
| **Google AdMob** | Ad network | Banner ad at the bottom of the screen |

---

## 📂 Project Structure

```
SMSExpenseTracker/
│
├── app/
│   └── src/main/
│       │
│       ├── java/com/example/smsexpensetracker/
│       │   ├── MainActivity.java          → Main screen — controls everything
│       │   ├── Transaction.java           → Data model (blueprint for one transaction)
│       │   ├── DatabaseHelper.java        → All SQLite operations (save, read, update)
│       │   ├── SMSReader.java             → Reads SMS inbox from the phone
│       │   ├── SMSParser.java             → Extracts data from SMS text using Regex
│       │   ├── TransactionAdapter.java    → Connects data to the RecyclerView list
│       │   ├── TransactionDialog.java     → Popup shown when you tap a transaction
│       │   └── AddTransactionDialog.java  → Popup for manually adding a cash transaction
│       │
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml          → Main screen layout
│       │   │   ├── item_transaction.xml       → Single row in the transaction list
│       │   │   ├── dialog_transaction.xml     → Transaction detail popup layout
│       │   │   └── dialog_add_transaction.xml → Add cash transaction popup layout
│       │   │
│       │   ├── values/
│       │   │   ├── colors.xml    → App color definitions
│       │   │   ├── strings.xml   → All text used in the app
│       │   │   └── themes.xml    → App theme and style
│       │   │
│       │   └── drawable/
│       │       ├── bg_dialog_rounded.xml → Rounded white background for popups
│       │       └── ic_launcher.png       → App icon
│       │
│       └── AndroidManifest.xml   → App permissions and component configuration
│
├── app/build.gradle        → App-level dependencies and build config
├── build.gradle            → Project-level Gradle config
├── settings.gradle         → Project name and module inclusion
├── gradle.properties       → JVM and AndroidX settings
├── gradlew / gradlew.bat   → Gradle wrapper scripts
└── README.md               → This file
```

---

## 🔄 How The App Works — Step By Step

### First Time You Open The App

```
App Opens
    ↓
Ask for SMS Permission (READ_SMS + RECEIVE_SMS)
    ↓
Permission Granted?
    ├── YES → Scan entire SMS inbox in background thread
    └── NO  → Show empty screen (can grant later via Settings)
            ↓
    Read all SMS from inbox (content://sms/inbox)
            ↓
    For each SMS — is it a bank SMS? (keyword check)
    ├── NO  → Skip it
    └── YES → Extract details using Regex
                    ↓
            Amount found AND type found?
            ├── NO  → Skip
            └── YES → Save to SQLite database
                            ↓
                    Show in list (latest on top)
```

### Every Time After That

```
App Opens
    ↓
Load transactions from SQLite database instantly
    ↓
Show in RecyclerView list
    ↓
SMSReceiver catches any new SMS that arrive in real time
```

### When You Tap the Reload Button (🔄)

```
Tap Reload FAB
    ↓
Re-scan SMS inbox in background thread
    ↓
Only NEW SMS are added (duplicates automatically skipped via UNIQUE constraint)
    ↓
List refreshes
```

### When You Tap the ➕ Button

```
Tap Add FAB
    ↓
Popup opens
    ↓
Fill in: Date, Time, Credit/Debit, Amount, Party, Description
    ↓
Tap Add
    ↓
Saved to SQLite database
    ↓
Appears at top of list
```

### When You Tap a Transaction Row

```
Tap any row
    ↓
Popup shows full details:
  • Date & Time
  • Amount
  • Type (Credited / Debited)
  • Party (who sent/received)
  • Reference number / UTR
  • Description (editable)
    ↓
Edit description → Tap Save
    ↓
Updated in SQLite instantly
```

---

## 🧠 How SMS Parsing Works

The app uses **Regex (Regular Expressions)** to find patterns in text.

### Example SMS:
```
Your a/c no. XX0046 debited for Rs.34.00 on 17-03-2026 19:17:24
trf to bharatpe.9q0p0e0k0c835361@unit (RefNo 120174243512). DNS Bank
```

### What Gets Extracted:

| Field | Extracted Value | Pattern Used |
|-------|----------------|--------------|
| Amount | 34.00 | `Rs\.?\s*([\d,]+\.?\d*)` |
| Type | Debited | Word "debited" detected |
| Date-Time | 17-03-2026 19:17:24 | `dd-MM-yyyy HH:mm:ss` |
| Party | bharatpe.9q...@unit | VPA pattern `\w+@\w+` |
| Reference | 120174243512 | Number after "RefNo" |

### Supported Banks & Payment Apps

| Bank / App | Supported |
|-----------|-----------|
| DNS Bank | ✅ |
| HDFC Bank | ✅ |
| SBI | ✅ |
| ICICI Bank | ✅ |
| Axis Bank | ✅ |
| Kotak Bank | ✅ |
| GPay | ✅ |
| PhonePe | ✅ |
| Paytm | ✅ |
| BHIM UPI | ✅ |
| Most Indian banks | ✅ |

---

## 🗄️ Database Schema

Single SQLite table: **`transactions`**

| Column | Type | What It Stores |
|--------|------|----------------|
| `id` | INTEGER PK | Auto-generated unique ID |
| `datetime` | TEXT | Date and time of transaction |
| `amount` | REAL | Transaction amount (Rs.) |
| `type` | TEXT | `"Credited"` or `"Debited"` |
| `description` | TEXT | User-editable note |
| `party` | TEXT | Who sent/received the money |
| `reference` | TEXT | Bank reference / UTR number |
| `sms_id` | TEXT UNIQUE | Original SMS ID — prevents duplicates |
| `sms_date` | INTEGER | SMS epoch timestamp — used for sorting |

> **Duplicate Prevention:** `sms_id` has a `UNIQUE` constraint. If the same SMS is scanned twice, the database automatically ignores the second insert via `CONFLICT_IGNORE`.

---

## 📱 App Screens

### Main Screen
- Blue toolbar with app name
- Summary bar: total transactions | total credited | total debited
- Column headers: Date/Time | Amount | Type
- Scrollable RecyclerView list
  - 🟢 **Green** = Credited (money received)
  - 🔴 **Red** = Debited (money spent)
- 🔄 **Reload FAB** (blue) — re-scans SMS inbox
- ➕ **Add FAB** (orange) — manually add a cash transaction
- AdMob banner at the very bottom

### Transaction Detail Popup
- Full transaction details (read-only)
- Editable description field
- **Save** and **Close** buttons

### Add Transaction Popup
- Date picker (with calendar dialog)
- Time picker (24-hour)
- Credit / Debit radio buttons
- Amount field
- Party / Person field (optional)
- Description field (optional)
- **Add** and **Cancel** buttons

---

## 🔐 Permissions

| Permission | Why Needed |
|-----------|-----------|
| `READ_SMS` | Read bank SMS messages from the inbox |
| `RECEIVE_SMS` | Listen for new incoming SMS in real time |
| `INTERNET` | Required by AdMob SDK to fetch ads |
| `ACCESS_NETWORK_STATE` | Required by AdMob SDK |

> **Privacy:** All data stays **on your phone**. No data is sent to any server. The app works fully offline (except for AdMob ads).

---

## 💰 AdMob Integration

- Banner ad anchored at the bottom of the main screen
- **App ID:** `ca-app-pub-4195105056058261~4846325772`
- **Ad Unit ID:** `ca-app-pub-4195105056058261/4225843823`
- Ad lifecycle properly managed (`pause` / `resume` / `destroy` with Activity)

---

## ⚙️ Setup Instructions

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android device running Android 5.0 (API 21) or above
- Physical device recommended — SMS inbox is not available on the emulator

### Steps to Run
1. Open **Android Studio**
2. **File → Open** → select the `SMSExpenseTracker` folder
3. Wait for Gradle sync to complete
4. Connect your Android phone via USB with **USB Debugging** enabled
5. Click **Run ▶**
6. Grant SMS permission when prompted
7. App automatically scans your SMS inbox on first launch

### Building a Debug APK
1. **Build → Build Bundle(s)/APK(s) → Build APK(s)**
2. APK is saved at: `app/build/outputs/apk/debug/app-debug.apk`
3. Share via WhatsApp or Google Drive
4. On the recipient's device: enable **Install from Unknown Sources**, then install
5. Recipients may see a Play Protect warning — tap **Install Anyway**

---

## 🐛 Common Issues & Fixes

| Problem | Fix |
|---------|-----|
| App crashes on open | Grant SMS permission in phone **Settings → Apps → SMS Expense Tracker → Permissions** |
| No transactions showing | Tap the 🔄 reload button |
| Wrong / incomplete transactions showing | Only bank SMS are parsed — check `SMSParser.java` keywords |
| Banner ad not showing | New AdMob accounts take 24–48 hours to activate |
| `"App not installed"` error | Uninstall old version first, then reinstall |
| Play Protect warning | Tap **More details → Install Anyway** — normal for sideloaded APKs |
| Gradle sync fails | Check internet connection; invalidate caches (**File → Invalidate Caches**) |

---

## 📊 Example Output

Once running with real data you might see:

```
899 Transactions  |  + Rs.115387  |  - Rs.97226
```

This means:
- **899** bank transactions found and saved
- **₹1,15,387** total money credited (received)
- **₹97,226** total money debited (spent)

---

## 🏗️ Build Info

| Property | Value |
|----------|-------|
| Language | Java (100%) |
| Database | SQLite (on-device, no cloud) |
| UI | Material Design Components (XML) |
| Min Android | 5.0 (API 21) |
| Target Android | 15 (API 35) |
| Compile SDK | 35 |
| Version | 2.0 (versionCode 2) |
| AGP | 8.7.3 |
| AdMob SDK | 23.6.0 |
| Background Threading | `ExecutorService` + `Handler` (no deprecated `AsyncTask`) |

---

*Built for personal finance tracking. All data stored locally on device. No cloud, no login, no subscription.*
