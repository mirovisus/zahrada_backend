package upce.fei.garden.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Registruje {@code /uploads/**} jako statický resource handler nad fyzickým adresářem
 * {@code app.upload.dir} (viz {@link upce.fei.garden.service.FileStorageService}), aby byly
 * nahrané fotografie zahrad dostupné přímo přes HTTP GET (v {@code SecurityConfig} veřejně,
 * bez nutnosti autentizace - jde jen o čtení statického souboru, ne o API zdroj).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebMvcConfig(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + path + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
