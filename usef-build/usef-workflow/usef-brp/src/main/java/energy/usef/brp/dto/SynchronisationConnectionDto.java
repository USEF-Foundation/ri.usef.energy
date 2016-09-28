package energy.usef.brp.dto;

public class SynchronisationConnectionDto {

    private Long id;

    private String entityAddress;

    private String lastModificationTime;

    public SynchronisationConnectionDto(Long id, String entityAddress, String lastModificationTime) {
        this.id = id;
        this.entityAddress = entityAddress;
        this.lastModificationTime = lastModificationTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public String getLastModificationTime() {
        return lastModificationTime;
    }

    public void setLastModificationTime(String lastModificationTime) {
        this.lastModificationTime = lastModificationTime;
    }
}
