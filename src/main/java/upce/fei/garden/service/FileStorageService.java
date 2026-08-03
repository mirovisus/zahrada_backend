package upce.fei.garden.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import upce.fei.garden.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ukládání nahraných souborů (fotografie zahrad) na lokální disk pod {@code app.upload.dir}.
 * <p>
 * Veškerá sanitizace vstupu je soustředěna v {@link #store}, v tomto pořadí:
 * <ol>
 *   <li>soubor nesmí být prázdný;</li>
 *   <li>velikost nesmí překročit {@code app.upload.max-size} (kontrola navíc k
 *       {@code spring.servlet.multipart.max-file-size}, které request s větším souborem odmítne
 *       už na úrovni multipart parseru, dřív než se dostane sem);</li>
 *   <li>klientem deklarovaný {@code Content-Type} musí být v seznamu {@code app.upload.allowed-types};</li>
 *   <li><b>skutečný</b> obsah souboru musí odpovídat jednomu z povolených formátů (JPEG/PNG/WebP)
 *       podle signatury (magic bytes) v prvních bajtech – klientem deklarovaný {@code Content-Type}
 *       ani přípona originálního názvu souboru se pro toto rozhodnutí nepoužívají, jde jen o snadno
 *       zfalšovatelné metadata (soubor přejmenovaný na {@code .jpg} s cizím obsahem tak neprojde).</li>
 * </ol>
 * Název souboru na disku si generuje služba sama (UUID + přípona odvozená ze skutečně
 * detekovaného typu) – originální název souboru od klienta se nikde nepoužije, takže nemůže
 * posloužit k path traversal útoku (např. {@code ../../etc/passwd}) ani k záměně přípony.
 * {@link #delete} navíc před smazáním ověří, že cílová cesta po normalizaci stále leží uvnitř
 * {@code app.upload.dir}, pro případ, že by volající předal neočekávanou hodnotu URL.
 */
@Slf4j
@Service
public class FileStorageService {

    private static final String URL_PREFIX = "/uploads/";
    private static final int MAGIC_BYTES_HEADER_LENGTH = 12;

    // magic bytes - viz https://en.wikipedia.org/wiki/List_of_file_signatures
    private static final int[] JPEG_SIGNATURE = {0xFF, 0xD8, 0xFF};
    private static final int[] PNG_SIGNATURE = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46}; // "RIFF" na zacatku
    private static final int[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50}; // "WEBP" od 9. bajtu
    private static final int WEBP_SIGNATURE_OFFSET = 8;

    private final Path uploadDir;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDirProperty,
                               @Value("${app.upload.max-size}") DataSize maxSize,
                               @Value("${app.upload.allowed-types}") List<String> allowedContentTypes) {
        this.uploadDir = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSize.toBytes();
        this.allowedContentTypes = allowedContentTypes.stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Nepodařilo se vytvořit adresář pro nahrané soubory: " + this.uploadDir, e);
        }
    }

    /**
     * Uloží nahraný soubor na disk pod náhodně vygenerovaným názvem a vrátí jeho veřejnou
     * relativní URL (např. {@code /uploads/3f2a...-b1.jpg}).
     *
     * @throws ValidationException pokud soubor nesplní některou z kontrol popsaných v Javadoc třídy
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Odmítnut soubor při nahrávání: soubor je prázdný.");
            throw new ValidationException("Nahraný soubor je prázdný.");
        }
        if (file.getSize() > maxSizeBytes) {
            log.warn("Odmítnut soubor při nahrávání: velikost {} B překračuje limit {} B.", file.getSize(), maxSizeBytes);
            throw new ValidationException(
                    "Soubor je příliš velký. Maximální povolená velikost je " + (maxSizeBytes / (1024 * 1024)) + " MB.");
        }

        String declaredContentType = file.getContentType() == null
                ? null : file.getContentType().toLowerCase(Locale.ROOT);
        if (declaredContentType == null || !allowedContentTypes.contains(declaredContentType)) {
            log.warn("Odmítnut soubor při nahrávání: nepovolený Content-Type '{}'.", declaredContentType);
            throw new ValidationException(
                    "Nepovolený typ souboru. Povoleny jsou pouze: " + String.join(", ", allowedContentTypes));
        }

        ImageType detectedType = detectImageType(file)
                .orElseThrow(() -> {
                    log.warn("Odmítnut soubor při nahrávání: obsah neodpovídá signatuře žádného z povolených "
                            + "obrázkových formátů (deklarovaný Content-Type '{}').", declaredContentType);
                    return new ValidationException(
                            "Obsah souboru neodpovídá žádnému z povolených formátů obrázku (JPEG, PNG, WebP).");
                });

        String filename = UUID.randomUUID() + "." + detectedType.extension;
        Path target = resolveWithinUploadDir(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Nepodařilo se uložit nahraný soubor: " + filename, e);
        }

        log.info("Nahrán soubor: name={}, size={} B", filename, file.getSize());
        return URL_PREFIX + filename;
    }

    /**
     * Smaže dříve uložený soubor podle URL vrácené z {@link #store}. Pokud {@code url} je
     * {@code null}, prázdné, nebo po sanitizaci vede mimo {@code app.upload.dir}, nic neprovede
     * (tichý no-op) – volající (např. {@code GardenService}) tak nemusí ověřovat, zda zahrada
     * vůbec nějakou fotografii má.
     */
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        String filename = extractFilename(url);
        if (filename.isBlank()) {
            return;
        }

        Path target = resolveWithinUploadDir(filename);
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("Smazán soubor: name={}", filename);
            }
        } catch (IOException e) {
            log.warn("Nepodařilo se smazat soubor '{}': {}", filename, e.getMessage());
        }
    }

    /**
     * Vezme jen poslední segment cesty (za posledním {@code /}) – i kdyby {@code url} obsahovala
     * {@code ../} nebo jiné segmenty cesty, do fyzického mazání/čtení se z ní dostane jen samotný
     * název souboru.
     */
    private String extractFilename(String url) {
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    /**
     * Sestaví cestu k souboru uvnitř {@code app.upload.dir} a ověří (defense-in-depth vedle
     * {@link #extractFilename}), že po normalizaci skutečně leží uvnitř tohoto adresáře.
     */
    private Path resolveWithinUploadDir(String filename) {
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.getParent().equals(uploadDir)) {
            log.warn("Pokus o přístup k souboru mimo upload adresář: '{}'.", filename);
            throw new ValidationException("Neplatný název souboru.");
        }
        return target;
    }

    private Optional<ImageType> detectImageType(MultipartFile file) {
        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(MAGIC_BYTES_HEADER_LENGTH);
        } catch (IOException e) {
            throw new UncheckedIOException("Nepodařilo se přečíst obsah nahraného souboru.", e);
        }

        if (matchesSignature(header, 0, JPEG_SIGNATURE)) {
            return Optional.of(ImageType.JPEG);
        }
        if (matchesSignature(header, 0, PNG_SIGNATURE)) {
            return Optional.of(ImageType.PNG);
        }
        if (matchesSignature(header, 0, RIFF_SIGNATURE) && matchesSignature(header, WEBP_SIGNATURE_OFFSET, WEBP_SIGNATURE)) {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    private static boolean matchesSignature(byte[] header, int offset, int[] signature) {
        if (header.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Formáty obrázků rozpoznávané podle signatury (magic bytes) v hlavičce souboru
     * ({@link #detectImageType}) – {@code extension} určuje příponu použitou při ukládání.
     */
    private enum ImageType {
        JPEG("jpg"),
        PNG("png"),
        WEBP("webp");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }
    }
}
