package co.edu.uniquindio.sga.infrastructure.imagenes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import co.edu.uniquindio.sga.domain.repository.AlmacenImagenes;

/**
 * Elige el adaptador del puerto AlmacenImagenes con {@code sga.imagenes.proveedor}: {@code local} (por defecto, sin internet ni
 * credenciales) o {@code cloudinary}. Con cloudinary, si faltan las credenciales la aplicación NO arranca y dice cuáles faltan:
 * es mejor fallar al iniciar que descubrirlo al subir la primera imagen. También sirve la carpeta local en {@code /imagenes/**}
 * (las imágenes de ejemplo de los datos de prueba están ahí aunque el proveedor sea Cloudinary).
 */
@Configuration
public class ImagenesConfig implements WebMvcConfigurer {

    private final AlmacenImagenesLocal local;

    public ImagenesConfig(AlmacenImagenesLocal local) {
        this.local = local;
    }

    /** @Primary: el almacén local también es un bean (lo usan los datos de prueba), pero quien pide el puerto recibe el elegido. */
    @Bean
    @org.springframework.context.annotation.Primary
    public AlmacenImagenes almacenImagenes(
            @Value("${sga.imagenes.proveedor:local}") String proveedor,
            @Value("${sga.imagenes.tiempo-espera-ms}") long tiempoEsperaMs,
            @Value("${sga.imagenes.cloudinary.url-api}") String urlApi,
            @Value("${sga.imagenes.cloudinary.cloud-name:}") String cloudName,
            @Value("${sga.imagenes.cloudinary.api-key:}") String apiKey,
            @Value("${sga.imagenes.cloudinary.api-secret:}") String apiSecret) {
        return switch (proveedor.trim().toLowerCase()) {
            case "local" -> local;
            case "cloudinary" -> {
                if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
                    throw new IllegalStateException("sga.imagenes.proveedor=cloudinary exige sga.imagenes.cloudinary.cloud-name, "
                            + "api-key y api-secret (en application-secrets.properties o como variables de entorno)");
                }
                yield new AlmacenImagenesCloudinary(cloudName, apiKey, apiSecret, urlApi, tiempoEsperaMs);
            }
            default -> throw new IllegalStateException("sga.imagenes.proveedor debe ser local o cloudinary, no " + proveedor);
        };
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/imagenes/**").addResourceLocations(local.getRaiz().toUri().toString());
    }
}
