# Udaljeno platno

Ovaj projekat predstavlja aplikaciju za zajedničko crtanje u realnom vremenu. Sastoji se od serverskog i klijentskog dijela: server upravlja prijemom i prosljeđivanjem poruka, dok klijenti omogućavaju korisnicima da crtaju na istom platnu. Novi korisnici pri povezivanju dobijaju trenutno stanje platna i nastavljaju da rade zajedno sa ostalima.

# Client strana
Ovo je klijentska strana aplikacije za zajedničko crtanje na udaljenom platnu. Više korisnika se može povezati na server, kreirati sobe, ulaziti u njih i crtati u realnom vremenu.

### Pokretanje

1. Pokrenuti server.
2. Pokrenuti klijent iz klase `Client`
3. Nakon unosa imena i povezivanja moguće je kreirati ili odabrati sobu i crtati zajedno sa drugim korisnicima.   


## Struktura projekta

- **ClientConnection.java** – upravlja mrežnom komunikacijom sa serverom (slanje i prijem poruka, poruke za crtanje, rad u posebnom thread-u).  
- **ClientGUI.java** – glavni grafički interfejs. Omogućava pregled soba, ulazak u sobe i crtanje na zajedničkom platnu.  
- **ClientGUIStart.java** – početna scena u kojoj se unosi ime i uspostavlja konekcija sa serverom.  
- **CreateRoomScene.java** – forma za kreiranje novih soba.  


## Funkcionalnosti

- Pregled i osvežavanje liste soba  
- Kreiranje sobe sa opcionalnom lozinkom  
- Ulazak u sobu i crtanje u realnom vremenu  
- Podrška za linije, pravougaonike, elipse i reset platna  
- Izbor boje i debljine linije  
- Automatska sinhronizacija svih poteza između klijenata  

# Server strana

Server strana je napisana u Javi koristeći NIO (non-blocking I/O) mehanizam. On komunicira sa klijentima putem unapred definisanih tekstualnih poruka.


## Pokretanje servera

Server se pokreće putem glavne klase `Server`

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





