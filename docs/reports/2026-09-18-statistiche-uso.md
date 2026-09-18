# Statistiche d'uso — Zaffiro Mini WhatsApp

- **Sorgente**: `BE/logs/app.log` (unico file disponibile, nessuna rotazione `.gz` necessaria)
- **Periodo coperto dal file**: 2026-09-18, 10:49:57 → 12:25:59 (circa 1h36m di attività)
- **Eventi AUDIT totali**: 45
- **Nota metodologica**: gli username che iniziano per `zztest` (`zztesta`) sono account di test automatici creati oggi durante lo sviluppo. Sono riportati **separatamente** e **esclusi** dalle cifre principali sotto indicate.

## Account di test (`zztest*`) — esclusi dalle statistiche principali

| Evento | Occorrenze | Dettaglio |
|---|---|---|
| LOGIN_OK | 4 | tutte `zztesta`, tra le 11:35 e le 11:57 |
| AI_SUGGESTION | 5 | tutte `zztesta` → partner `zztestb` |
| **Totale eventi zztest** | **9** | — |

`zztestb` compare solo come `partner` in `AI_SUGGESTION`, non ha mai effettuato login né inviato messaggi: non risulta come "utente attivo".

## Utenti attivi (esclusi zztest)

Definizione: username distinti in `LOGIN_OK`, `WS_CONNECT`, `MSG_SENT`.

| Utente | LOGIN_OK | WS_CONNECT | WS_DISCONNECT | MSG_SENT | MSG_RICEVUTI (recipient) | AI_SUGGESTION (come richiedente) | STATS_EMAILED |
|---|---|---|---|---|---|---|---|
| lorenzo | 4 | 4 | 2 (`wentOffline=true` entrambe) | 3 | 3 | 2 (partner: bruno) | 1 |
| bruno | 1 | 2 | 1 (`wentOffline=false`) | 3 | 3 | 1 (partner: lorenzo) | 0 |

**Utenti reali attivi: 2** (lorenzo, bruno). Nessun login fallito (`LOGIN_FAIL`) registrato nel periodo, per nessun utente.

## Coppie di utenti più attive

| Coppia | Messaggi scambiati | AI_SUGGESTION richieste |
|---|---|---|
| lorenzo ↔ bruno | 6 (unica coppia reale) | 3 (2 lorenzo→bruno, 1 bruno→lorenzo) |
| zztesta ↔ zztestb (test) | 0 messaggi | 5 |

lorenzo↔bruno è l'unica coppia con scambio reale di messaggi nel periodo.

## Messaggi: invio, consegna, lettura

| Metrica | Valore |
|---|---|
| MSG_SENT | 6 (messageId 8–13) |
| MSG_DELIVERED (somma `count`) | 6 |
| MSG_READ (somma `count`) | 6 |
| **Tasso di consegna** | 6/6 = **100%** |
| **Tasso di lettura** | 6/6 = **100%** |

Dettaglio invii per data/ora (unico giorno, 2026-09-18):

| Fascia oraria | MSG_SENT | Note |
|---|---|---|
| 12:00–12:59 | 6 | tutti i messaggi reali (bruno↔lorenzo) |

Non ci sono messaggi in altre fasce orarie: i 6 invii sono concentrati tra le 12:09:06 e le 12:11:27.

### Tempi di risposta (correlazione `messageId` ↔ `messageIds`)

| Metrica | Media | Mediana |
|---|---|---|
| Invio → Consegna | ~10,5 ms | ~7,3 ms |
| Invio → Lettura | ~390 ms* | ~25,3 ms |

\* La media invio→lettura è alterata da un outlier: il messaggio `8` (bruno→lorenzo, 12:09:06) è stato letto dopo 2,22 s, mentre tutti gli altri 5 messaggi sono stati letti entro 21–27 ms dalla consegna (probabile primo messaggio della sessione, con l'utente che apre la chat con un piccolo ritardo). Escludendo l'outlier, la media scende a ~25 ms, coerente con la mediana.

## Login e connessioni WebSocket

| Utente | Login riusciti | Login falliti | WS_CONNECT | WS_DISCONNECT |
|---|---|---|---|---|
| lorenzo | 4 | 0 | 4 | 2 |
| bruno | 1 | 0 | 2 | 1 |
| zztesta (test) | 4 | 0 | 0 | 0 |

Nessun `LOGIN_FAIL` nel periodo coperto dal log: non ci sono tentativi di forzatura da segnalare.

Curiosità: la sessione WebSocket di bruno aperta alle 12:11:17 si chiude un secondo dopo (12:11:18) con `wentOffline:false` — compatibile con un refresh/apertura doppia scheda, non un'anomalia.

## Funzionalità AI e statistiche via email (eventi introdotti oggi)

| Evento | Totale (esclusi zztest) | Dettaglio |
|---|---|---|
| AI_SUGGESTION | 3 | lorenzo→bruno ×2, bruno→lorenzo ×1 |
| STATS_EMAILED | 1 | lorenzo (12:25:12) |

Includendo gli account di test, `AI_SUGGESTION` totali nel log sono 8 (3 reali + 5 di test `zztesta`).

## Cosa emerge

Il log copre una sessione di test/sviluppo molto breve (meno di 2 ore, un solo giorno): prima una fase di prova con account `zztest*` dedicata quasi solo a testare i suggerimenti AI (5 `AI_SUGGESTION`, nessun messaggio reale), poi una sessione reale tra `lorenzo` e `bruno` con 6 messaggi scambiati, consegna e lettura al 100% e tempi di risposta sotto i 30 ms (a parte un primo messaggio letto con ~2,2 s di ritardo). L'utente `lorenzo` ha anche richiesto l'invio delle statistiche account via email una volta nel periodo osservato. Il campione è troppo piccolo per trarre conclusioni su fasce orarie di punta o pattern d'uso ricorrenti: servono più giorni di log per un'analisi statisticamente significativa.
