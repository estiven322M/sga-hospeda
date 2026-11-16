package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Una imagen de un apartamento (7.3), guardada en la tabla apartamento_imagen. La retirada se conserva (retiro lógico). */
@Embeddable
public class ImagenApartamentoEmbeddable {

    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "referencia", nullable = false, length = 300)
    private String referencia;

    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Column(name = "retirada", nullable = false)
    private boolean retirada;

    protected ImagenApartamentoEmbeddable() {
    }

    public ImagenApartamentoEmbeddable(String id, String url, String referencia, boolean principal, boolean retirada) {
        this.id = id;
        this.url = url;
        this.referencia = referencia;
        this.principal = principal;
        this.retirada = retirada;
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getReferencia() { return referencia; }
    public boolean isPrincipal() { return principal; }
    public boolean isRetirada() { return retirada; }
}
