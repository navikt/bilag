# Bilag
Bilag tilbyr eit REST-endepunkt for å hente fram og vise fakturaar for OEBS.

## Testing
For å teste i dev trengs ein Trygdeetaten-brukar som er medlem av minst ei av fylgjande AD-grupper 
(dersom ein mangler denne tilgangen kan ein sjølv leggje den til i IDA)
```
0000-GA-Varseladmin
0000-GA-Eye-Share
```
Denne kan ein bruke for å hente ut ein faktura
```
https://bilag-q1.dev.intern.nav.no/hent/1622562
```
Denne kan ein bruke for å få 'Fant ikke dokumentet'-feilsida
```
https://bilag-q1.dev.intern.nav.no/hent/123456
```
Og denne e.l. for å få 'DokId er ikke en gyldig dokumentId'-feilsida
```
https://bilag-q1.dev.intern.nav.no/hent/123a
```

## Førespurnadar
Spørsmål om koda eller prosjektet kan stillast på [Slack-kanalen for \#Team  Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ).