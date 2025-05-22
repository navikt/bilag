# Bilag
Bilag tilbyr eit REST-endepunkt for å hente fram og vise fakturaar for OEBS.

## Testing
For å teste at det fungerer å hente ut ein faktura i dev kan ein lime inn dette i nettlesaren. 
Merk at ein fyrst må logge inn med ein Trygdeetaten-brukar.
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