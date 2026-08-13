# Google Sign-In — Mobile Integration Guide

Integration guide for the 4 mobile apps:

| App | Platform | Repo | OAuth client type |
|---|---|---|---|
| CareNest (patient) | Android | `CareNest-Patient` | Android (`com.carenest`) |
| CareNest (provider) | Android | `CareNest-Provider` | Android (`com.carenest.provider`) |
| Carely (patient) | iOS | `Carely-Patient` | iOS (`iti.Carely`) |
| Enaya (provider) | iOS | `Enaya-Provider` | iOS (`iti.EnayaProvider`) |

The backend endpoints are already implemented and tested on branch `feat/google-sign-in`.

---

## 1. How the flow works

```
Mobile app (Google Sign-In SDK)
        │  user signs in with Google
        ▼
ID token (JWT)  ──────────────►  POST /api/v1/auth/google  (patients)
                                POST /api/v1/auth/nurse/google  (nurses)
                                        │
                                        ▼
                        Backend validates token with Google
                        (aud = web client ID, email verified)
                                        │
                              ┌─────────┴──────────┐
                              ▼                    ▼
                     account complete          phone required
                     (has phone)               (no phone yet)
                              │                    │
                              ▼                    ▼
                   200 AUTHENTICATED      200 PHONE_REQUIRED
                   accessToken            pendingToken + user info
                   refreshToken                  │
                              ┌───────────────────┘
                              ▼
                   App shows phone + OTP screen
                              │
                              ▼
              POST /api/v1/auth/verify-otp
              { phoneNumber, otp, pendingToken }
                              │
                              ▼
                  200 AUTHENTICATED (tokens)
```

## 2. Prerequisites (backend team)

- [x] Google Cloud project with **published** consent screen (test mode exited)
- [x] **Web client ID**: `588669996312-j7mtte5q10a1lcsu4jfn4g18n9gri40e.apps.googleusercontent.com`
- [ ] Android OAuth clients created (package + SHA-1, see section 7)
- [x] iOS OAuth clients created (bundle IDs `iti.Carely`, `iti.EnayaProvider`)
- [ ] `feat/google-sign-in` deployed to Railway
- [x] Railway env var `GOOGLE_WEB_CLIENT_ID` set to the web client ID above

> **Why one `aud` for everyone?** The backend only accepts ID tokens whose `aud` claim equals the **web client ID**. Android does this by passing the web client ID to `requestIdToken(...)`. iOS does it via the `serverClientID` parameter of `GIDConfiguration`. If an app sends a token with any other `aud`, the backend rejects it with `401`.

## 3. Key values

| Value | Where to use |
|---|---|
| `588669996312-j7mtte5q10a1lcsu4jfn4g18n9gri40e.apps.googleusercontent.com` | Web client ID: Android `requestIdToken`, iOS `serverClientID`, backend `GOOGLE_WEB_CLIENT_ID` |
| `com.carenest` | Android OAuth client (patient) — package name |
| `com.carenest.provider` | Android OAuth client (provider) — package name |
| `SHA1: <from build machine>` | Android OAuth client(s) fingerprint — same value for both apps (same debug keystore) |
| `iti.Carely` | iOS OAuth client (patient) — bundle ID |
| `iti.EnayaProvider` | iOS OAuth client (provider) — bundle ID |
| `588669996312-kqkeu03csqmh030btmpfnq450l563dmo.apps.googleusercontent.com` | iOS client ID — **Carely** (patient), `GIDClientID` + URL scheme |
| `588669996312-4jm91t591o9ej3q045d7a0adbdk29vdr.apps.googleusercontent.com` | iOS client ID — **Enaya** (provider), `GIDClientID` + URL scheme |
| `https://home-nursing-system-production.up.railway.app` | Production base URL |

## 4. Backend contract

Base URL: `https://home-nursing-system-production.up.railway.app`

### 4.1 Google login

`POST /api/v1/auth/google` — patients (CareNest patient, Carely)
`POST /api/v1/auth/nurse/google` — nurses/providers (CareNest provider, Enaya)

Request:
```json
{
  "idToken": "<Google ID token from the SDK>"
}
```

Response `200` — `AUTHENTICATED` (account complete):
```json
{
  "status": "AUTHENTICATED",
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900,
  "user": { }              // patients: UserResponse
}
```
For providers the same shape carries `"nurseUser": { }` instead of `"user"`.

Response `200` — `PHONE_REQUIRED` (first-time sign-in, phone needed):
```json
{
  "status": "PHONE_REQUIRED",
  "pendingToken": "...",
  "email": "...",
  "firstName": "...",
  "lastName": "...",
  "profileImageUrl": "..."
}
```
`pendingToken` is valid for **10 minutes**. Use the returned name/email/image to prefill your phone-linking screen.

### 4.2 Complete phone linking

`POST /api/v1/auth/verify-otp`

```json
{
  "phoneNumber": "+201234567890",
  "otp": "123456",
  "pendingToken": "<pendingToken from PHONE_REQUIRED>"
}
```

> Use `/api/v1/auth/login` (same body `{"phoneNumber": "..."}`) to request the OTP SMS first, or `/api/v1/auth/dev/request-otp` in dev environments (returns the OTP directly).

Response `200` — same `AUTHENTICATED` shape as above.

### 4.3 Errors

| Status | Code/message | Meaning |
|---|---|---|
| `400` | `Bad Request` | Missing `idToken` (validation) |
| `401` | `Invalid Google ID token` | Token expired/malformed/signature invalid |
| `401` | `Google ID token was not issued for this application` | `aud` ≠ web client ID — check SDK config (section 5/6) |
| `401` | `Google email is not verified` | Account has an unverified email |
| `404` | `User not found` | Account was deleted |
| `409` | `This account is already registered as a nurse. Please use the nurse login.` | Google account is a nurse, app called the patient endpoint |
| `409` | `This account is already registered as a regular user. Please use the user login.` | Google account is a patient, app called the nurse endpoint |
| `409` | `This phone is already registered to another account.` | Phone entered during linking belongs to a different account |
| `401` | `Invalid OTP` / `OTP has expired or is invalid` | Wrong/expired OTP during linking |

Error body (`ApiError`):
```json
{
  "timestamp": "2026-08-13T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "CONFLICT",
  "message": "This account is already registered as a nurse. Please use the nurse login.",
  "details": null
}
```

### 4.4 Using the tokens

- Send `Authorization: Bearer <accessToken>` on all authenticated calls (your existing `AuthInterceptor`/`NetworkClient` already does this — store these tokens in the same place as the phone-flow tokens).
- `POST /api/v1/auth/refresh` with `{"refreshToken": "..."}` to refresh.
- `POST /api/v1/auth/logout` with `{"refreshToken": "..."}` to sign out.

## 5. Android integration (CareNest-Patient & CareNest-Provider)

### 5.1 Dependency

`app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
```

### 5.2 Configure the sign-in client

```kotlin
const val WEB_CLIENT_ID =
    "588669996312-j7mtte5q10a1lcsu4jfn4g18n9gri40e.apps.googleusercontent.com"

val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(WEB_CLIENT_ID)   // aud = web client ID (required)
    .requestEmail()
    .build()

val googleSignInClient = GoogleSignIn.getClient(this, gso)
```

### 5.3 Launch sign-in and get the ID token

```kotlin
// Register (before onCreate, e.g. in a Fragment/Activity or Compose)
val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    val account = try {
        task.getResult(ApiException::class.java)
    } catch (e: ApiException) {
        return@registerForActivityResult // user cancelled or failed
    }
    val idToken = account.idToken ?: return@registerForActivityResult
    loginWithGoogle(idToken)
}

launcher.launch(googleSignInClient.signInIntent)
```

### 5.4 Send the token to the backend

```kotlin
suspend fun loginWithGoogle(idToken: String) {
    val isProvider = BuildConfig.APPLICATION_ID == "com.carenest.provider"
    val endpoint = if (isProvider) "/api/v1/auth/nurse/google" else "/api/v1/auth/google"

    // POST { "idToken": idToken } → GoogleAuthResponse
    val response = apiClient.post(endpoint, mapOf("idToken" to idToken))

    when (response.status) {
        "AUTHENTICATED" -> {
            // store accessToken + refreshToken (same store as phone flow)
            tokenStore.save(response.accessToken, response.refreshToken)
            navigateToHome()
        }
        "PHONE_REQUIRED" -> {
            // prefill the phone-linking screen:
            navigateToPhoneLinking(
                pendingToken = response.pendingToken,
                email = response.email,
                firstName = response.firstName,
                lastName = response.lastName,
                profileImageUrl = response.profileImageUrl
            )
        }
    }
}

// Phone-linking screen (after user enters phone):
suspend fun linkPhone(phone: String, otp: String, pendingToken: String) {
    // 1. request OTP: POST /api/v1/auth/login { "phoneNumber": phone }
    // 2. after user enters OTP:
    val response = apiClient.post(
        "/api/v1/auth/verify-otp",
        mapOf("phoneNumber" to phone, "otp" to otp, "pendingToken" to pendingToken)
    )
    // response.status == "AUTHENTICATED" → save tokens, navigate to home
}
```

### 5.5 Handling errors

- `401` `aud` error → double-check `requestIdToken(WEB_CLIENT_ID)`.
- `409` role conflict → route the user to the other app's login (nurse login for providers) or show the message.
- `409` phone taken → prompt for a different phone number.

## 6. iOS integration (Carely-Patient & Enaya-Provider)

### 6.1 Dependency

Swift Package Manager: `https://github.com/google/GoogleSignIn-iOS` (or CocoaPods: `pod 'GoogleSignIn'`).

### 6.2 Info.plist

**Carely (patient):**
```xml
<key>GIDClientID</key>
<string>588669996312-kqkeu03csqmh030btmpfnq450l563dmo.apps.googleusercontent.com</string>

<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.588669996312-kqkeu03csqmh030btmpfnq450l563dmo</string>
        </array>
    </dict>
</array>
```

**Enaya (provider):**
```xml
<key>GIDClientID</key>
<string>588669996312-4jm91t591o9ej3q045d7a0adbdk29vdr.apps.googleusercontent.com</string>

<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.588669996312-4jm91t591o9ej3q045d7a0adbdk29vdr</string>
        </array>
    </dict>
</array>
```

> The URL scheme must match the "iOS URL scheme" field shown for the client in Google Cloud Console → Credentials.

### 6.3 Configure the sign-in (critical: serverClientID)

```swift
import GoogleSignIn

let webClientID = "588669996312-j7mtte5q10a1lcsu4jfn4g18n9gri40e.apps.googleusercontent.com"

GIDSignIn.sharedInstance.configuration = GIDConfiguration(
    clientID: iosClientID,        // the iOS OAuth client ID of THIS app
    serverClientID: webClientID   // REQUIRED: makes the ID token aud = web client ID
)
```

Per app, `clientID` is:
- Carely (patient): `588669996312-kqkeu03csqmh030btmpfnq450l563dmo.apps.googleusercontent.com`
- Enaya (provider): `588669996312-4jm91t591o9ej3q045d7a0adbdk29vdr.apps.googleusercontent.com`

> ⚠️ **Do not omit `serverClientID`.** Without it, the ID token's `aud` is the iOS client ID and the backend rejects it with `401` ("Google ID token was not issued for this application").

### 6.4 Sign in and get the ID token

```swift
GIDSignIn.sharedInstance.signIn(withPresenting: presentingVC) { result, error in
    guard error == nil, let user = result?.user else { return }
    guard let idToken = user.idToken?.tokenString else { return }
    loginWithGoogle(idToken: idToken)
}
```

### 6.5 Send the token to the backend

```swift
func loginWithGoogle(idToken: String) async throws {
    let isProvider = Bundle.main.bundleIdentifier == "iti.EnayaProvider"
    let endpoint = isProvider ? "/api/v1/auth/nurse/google" : "/api/v1/auth/google"

    // POST { "idToken": idToken } → GoogleAuthResponse (decode with your JSONDecoder)
    let response: GoogleAuthResponse = try await apiClient.post(endpoint, body: ["idToken": idToken])

    switch response.status {
    case "AUTHENTICATED":
        // store accessToken + refreshToken in the Keychain (same as phone flow)
        keychainTokenStore.save(accessToken: response.accessToken,
                                refreshToken: response.refreshToken)
        navigateToHome()
    case "PHONE_REQUIRED":
        // prefill the phone-linking screen:
        navigateToPhoneLinking(pendingToken: response.pendingToken,
                               email: response.email,
                               firstName: response.firstName,
                               lastName: response.lastName,
                               profileImageUrl: response.profileImageUrl)
    default:
        break
    }
}

// Phone-linking screen (after user enters phone):
func linkPhone(phone: String, otp: String, pendingToken: String) async throws {
    // 1. request OTP: POST /api/v1/auth/login { "phoneNumber": phone }
    // 2. after user enters OTP:
    let response: GoogleAuthResponse = try await apiClient.post(
        "/api/v1/auth/verify-otp",
        body: ["phoneNumber": phone, "otp": otp, "pendingToken": pendingToken]
    )
    // response.status == "AUTHENTICATED" → save tokens, navigate to home
}
```

### 6.6 Handling errors

Same as Android (section 5.5), including the `401` `aud` mismatch (→ check `serverClientID`).

## 7. Getting the Android SHA-1 (for the OAuth clients)

Run this on the machine that builds the Android apps (Android Studio installed):

Windows:
```
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```
macOS/Linux:
```
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the `SHA1:` line. Both apps built on the same machine share the same debug keystore, so the fingerprint is identical for `com.carenest` and `com.carenest.provider`.

Notes:
- The debug keystore is created automatically by Android Studio on first build; it is per-machine and must not be committed.
- If release builds use a separate signing keystore, register that SHA-1 as an additional fingerprint on the same Android OAuth clients (Google Console supports multiple fingerprints per client).
- If the signing keystore ever changes (different machine, recreated keystore), update the fingerprints — normal app/version changes do **not** affect the SHA-1.

## 8. Google Cloud Console checklist (backend team)

1. **Credentials → Create credentials → OAuth client ID**
2. **Android** (×2): Application type *Android* — package name `com.carenest` (and `com.carenest.provider`), SHA-1 from section 7.
3. **iOS** (×2): Application type *iOS* — bundle ID `iti.Carely` (and `iti.EnayaProvider`). No fingerprint needed. ✅ done — client IDs `588669996312-kqkeu03csqmh030btmpfnq450l563dmo` (Carely) and `588669996312-4jm91t591o9ej3q045d7a0adbdk29vdr` (Enaya).
4. Keep the web client ID as-is (already used by the backend).
5. After creating the iOS clients, copy each client ID + its reversed URL scheme into the apps (section 6.2).

## 9. Acceptance checklist

- [ ] Patient app (Android + iOS): fresh Google account → `PHONE_REQUIRED` → phone+OTP → `AUTHENTICATED` → app opens with existing endpoints working (Bearer token accepted).
- [ ] Provider app (Android + iOS): same flow via `/api/v1/auth/nurse/google`; nurse record created (`UNDER_REVIEW`).
- [ ] Existing phone-account user signs in with the same Google email → linked instantly, no OTP (account already has a phone).
- [ ] Google nurse account hitting the patient endpoint → `409` with the "use the nurse login" message.
- [ ] Wrong audience (e.g. iOS without `serverClientID`) → `401` aud message, no crash.
- [ ] Phone already registered to another account during linking → `409`, user can retry with another phone.
- [ ] Token refresh and logout work with the new tokens.
