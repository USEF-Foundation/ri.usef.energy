package energy.usef.dso.dto;

import java.util.ArrayList;
import java.util.List;

public class SynchronisationCongestionPointDto {

    private Long id;

    private String entityAddress;

    private String lastModificationTime;

    private List<SynchronisationConnectionDto> connections;

    public SynchronisationCongestionPointDto(Long id, String entityAddress, String lastModificationTime) {
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

    public List<SynchronisationConnectionDto> getConnections() {
        if (connections == null) {
            connections = new ArrayList<>();
        }
        return connections;
    }

    public void setConnections(List<SynchronisationConnectionDto> connections) {
        this.connections = connections;
    }
}
