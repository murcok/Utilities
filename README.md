# 🎵 SoundFocus

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-green.svg)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-cyan.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20%2F%20No%20Tracking-brightgreen.svg)]()

> **SoundFocus** è un'applicazione Android Open Source concepita per ascoltare musica senza interruzioni fastidiose di messaggi e notifiche, garantendo che le chiamate telefoniche continuino a squillare normalmente.

---

## 🌟 Caratteristiche Principali

- 🎧 **Rilevamento Intelligente della Riproduzione Musicale**:
  - Monitora automaticamente sessioni attive da player musicali popolari (**Spotify, YouTube Music, Apple Music, Deezer, Amazon Music, Tidal, VLC, Pocket Casts**, ecc.) o qualsiasi app selezionata.
- 🔕 **Silenzioso per Messaggi & Chat**:
  - All'avvio della riproduzione, silenzia automaticamente le notifiche di messaggistica (WhatsApp, Telegram, SMS, Slack, Discord).
- 📞 **Chiamate Sempre Udibili**:
  - Le chiamate in entrata mantengono squillo e vibrazione attivi. Puoi scegliere tra:
    - *Tutte le Chiamate* (qualsiasi numero può far squillare il telefono).
    - *Solo Contatti in Rubrica*.
    - *Solo Contatti Preferiti*.
    - *Chiamanti Ripetuti* (squillo attivo se la stessa persona richiama entro 15 minuti).
- 🔊 **Protezione Totale del Volume Multimediale (`STREAM_MUSIC`)**:
  - La musica non viene **mai azzerata** e il cursore del volume rimane sempre libero e regolabile (grazie alla policy `PRIORITY_CATEGORY_MEDIA`).
- ⚡ **Ripristino Istantaneo alla Pausa**:
  - Nel momento esatto in cui metti in pausa o fermi la musica, il volume delle notifiche e le impostazioni normali vengono ripristinati immediatamente.
- 🎛️ **Profili Audio Personalizzabili**:
  - Oltre al *Profilo Musica*, puoi creare profili personalizzati (*Studio & Lavoro*, *Guida Sicura*) con attivazione manuale (1-tap) o automatica.
- 🧪 **Simulatore di Prova Integrato**:
  - Testa istantaneamente il comportamento del silenzioso e dello squillo chiamate direttamente dall'app, senza dover avviare un lettore esterno.
- 🔒 **100% Rispetto della Privacy**:
  - Funziona completamente offline. Nessun dato, statistica o informazione personale viene trasmessa a server esterni.

---

## 🏗️ Architettura & Tecnologie

- **Linguaggio**: Kotlin 2.x
- **Interfaccia Utente**: Jetpack Compose con Material Design 3 (M3)
- **Database Locale**: Room Database (`MonitoredApp`, `AudioProfile`, `FocusLog`)
- **Concorrenza & Flussi**: Kotlin Coroutines & StateFlow
- **Integrazione Sistema Android**:
  - `NotificationListenerService` e `MediaSessionManager` per il rilevamento istantaneo delle tracce musicali.
  - `NotificationManager` (Zen Mode / InterruptionFilter con policy prioritaria trasparente verso Media e Chiamate).
  - `AudioManager` per la gestione indipendente degli stream `STREAM_NOTIFICATION`, `STREAM_RING` e `STREAM_MUSIC`.

---

## 🔐 Permessi Richiesti & Trasparenza

SoundFocus necessita solo dei permessi minimi indispensabili al suo funzionamento locale:

| Permesso | Scopo |
|---|---|
| `ACCESS_NOTIFICATION_POLICY` | Consente all'app di attivare la modalità Non Disturbare a priorità (silenzia messaggi, lascia passare chiamate). |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Permette di rilevare quando un player musicale è in riproduzione attiva. |
| `MODIFY_AUDIO_SETTINGS` | Per azzerare il volume delle notifiche e ripristinarlo alla pausa. |
| `POST_NOTIFICATIONS` | Per mostrare la notifica di stato del servizio in background. |
| `FOREGROUND_SERVICE` | Garantisce che il monitoraggio rimanga attivo anche con schermo spento. |

---

## 🛠️ Come Compilare il Progetto

### Prerequisiti
- **JDK 17** o superiore
- **Android SDK** (API Level 36 compilazione, minimo API 24 - Android 7.0+)
- **Gradle 8.x+**

### Comandi da Terminale
```bash
# Clona il repository
git clone https://github.com/your-username/SoundFocus.git
cd SoundFocus

# Compila l'APK di Debug
gradle assembleDebug

# Esegui i test unitari e Robolectric
gradle :app:testDebugUnitTest
```

L'APK compilato si troverà in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🤝 Come Contribuire

I contributi sono i benvenuti! Per dettagli su come proporre modifiche o nuove funzionalità, consulta [CONTRIBUTING.md](CONTRIBUTING.md).

1. Fai il Fork del repository
2. Crea un branch per la tua feature (`git checkout -b feature/nuova-funzionalita`)
3. Esegui i test (`gradle testDebugUnitTest`)
4. Fai il commit delle modifiche (`git commit -m 'Aggiunta nuova funzionalità'`)
5. Invia una Pull Request

---

## 📄 Licenza

Distribuito sotto licenza **MIT**. Consulta il file [LICENSE](LICENSE) per ulteriori informazioni.
