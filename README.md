# Udaljeno platno

## Server strana

Server strana je napisana u Javi koristeći NIO (non-blocking I/O) mehanizam. On komunicira sa klijentima putem unapred definisanih tekstualnih poruka.



## Pokretanje servera

Server se pokreće putem glavne klase:

```bash
java Server.Server
```

Server koristi port `12345` i sluša dolazne konekcije na `localhost`.

### Tehnički detalji
- Server koristi `Selector` za obradu više konekcija bez blokiranja.
- Svaki korisnik je predstavljen objektom `User` koji sadrži njegovo ime, socket i trenutni ID sobe.
- Sobe se čuvaju u mapi `roomMap`, a korisnici u listi `userList`.
- Platno se eksportuje iz JavaFX komponente u Base64 string.



### Funkcionalnosti

- Upravljanje korisnicima (dodavanje, uklanjanje)
- Kreiranje i čuvanje soba za crtanje
- Prikaz i manipulacija zajedničkim platnom u sobi
- Poruke za sinhronizaciju stanja između klijenata
- Podrška za crtanje linija, pravougaonika, elipsi, resetovanje platna i dohvat slike u Base64 formatu


## Poruke koje server prima

Server komunicira sa klijentima preko string poruka koje se završavaju znakom `\n`. Svaka poruka se parsira i procesuira u skladu sa svojom komandom.

| Poruka                  | Opis |
|------------------------|------|
| `IME\|ime`              | Postavlja ime korisnika. Ako je ime dostupno, vraća `POTVRDI`, inače `ERROR`. |
| `NSOBA\|naziv;lozinka`  | Kreira novu sobu sa imenom i (opcionom) lozinkom. |
| `SOBE`                 | Vraća listu soba u formatu `ime:id;...`. |
| `ULAZ\|id;lozinka`      | Pokušava da uđe u sobu sa datim ID-om i lozinkom. Vraća `POTVRDI` ili `ERROR`. |
| `IZLAZ` / `KRAJ`       | Napušta trenutnu sobu i briše je ako je prazna. |
| `INFO\|id`              | Vraća spisak korisnika u sobi sa zadatim ID-om. |
| `PLATNO\|id`            | Vraća trenutno stanje platna u sobi kao Base64 string. |
| `CRTAJ\|...`            | Crta oblik na platnu sobe korisnika. Detaljan format u nastavku. |

---

### Format poruke `CRTAJ`

```
CRTAJ|oblik;x1;y1;x2;y2;R;G;B;O;W
```

- `oblik`: `LINE`, `RECT`, `OVAL`, `REST`
- `x1, y1, x2, y2`: Koordinate oblika (po potrebi)
- `R, G, B`: Boja (0–1 za svaku boju)
- `O`: Opacity (0–1)
- `W`: Debljina linije

Ako je oblik `REST`, platno se resetuje.


## Poruke koje server šalje klijentima
*Kao i kad klijent šalje, svaka poruka od strane servera se završava  `\n`  znakom*

| Poruka                        | Opis |
|------------------------------|------|
| `POTVRDI`                    | Potvrda uspešne operacije (npr. uspešno logovanje, ulazak u sobu) |
| `ERROR`                      | Neuspešna operacija (npr. loša lozinka, zauzeto ime) |
| `ime:id;ime2:id2;...`        | Lista soba u sistemu |
| `ime1;ime2;...`              | Lista korisnika u sobi |
| `CRTAJ\|oblik;x1;y1;x2;y2;R;G;B;O;W` | Prenosi akciju crtanja drugim korisnicima |
| Base64 string                | Slika platna kod zahteva `PLATNO|id` |

---





