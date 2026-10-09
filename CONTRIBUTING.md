# Linee Guida per Contribuire a SoundFocus

Grazie per il tuo interesse nel contribuire a **SoundFocus**! Questo progetto è open-source e aperto ai contributi della community.

---

## 🛠️ Come Iniziare

1. Fai il **Fork** del repository su GitHub.
2. Clona il tuo fork localmente:
   ```bash
   git clone https://github.com/<tuo-username>/SoundFocus.git
   cd SoundFocus
   ```
3. Crea un ramo per la tua modifica:
   ```bash
   git checkout -b feature/nome-della-funzionalita
   ```

---

## 📐 Standard di Codice

- **Stile**: Segui le convenzioni ufficiali Kotlin e Jetpack Compose.
- **Architettura**: MVVM con StateFlow e Room Database.
- **Audio & Sistema**:
  - Non modificare mai `STREAM_MUSIC` per evitare interferenze con l'esperienza utente.
  - Verifica sempre che la policy Non Disturbare includa `PRIORITY_CATEGORY_MEDIA`.
- **Test**: Assicurati che tutti i test passino prima di inviare una PR:
  ```bash
  gradle :app:testDebugUnitTest
  ```

---

## 🚀 Invio di una Pull Request

1. Scrivi messaggi di commit chiari e descrittivi in italiano o inglese.
2. Assicurati che il codice compili con `gradle assembleDebug`.
3. Invia la tua Pull Request verso il branch `main`.
4. Descrivi chiaramente il problema risolto o la funzionalità aggiunta.
